#!/usr/bin/env python3
"""
Download the cover scans indexed by annualreport.gallery (Annual Report
Archive: 2,670 vintage corporate annual report covers, 1945-1992) at the
maximum resolution available.

IMPORTANT: annualreport.gallery does not hold these scans. It is a search
and discovery layer (semantic search over image+caption embeddings) built
by @philhedayatnia on top of four upstream collections. Its /covers/*.jpg
are web-sized derivatives. The full-resolution scans live upstream:

    Internet Archive  -- "The Annual Reports Archive"        1339 items
    Internet Archive  -- McGill University Library           993 items
    Internet Archive  -- misc / Intel corporate archives      22 items
    UW Libraries      -- Digital Collections (CONTENTdm)     316 items

So the index is read once from the gallery (a single page fetch, already
cached in .cache/) and every image is then pulled from the upstream
archive's own IIIF endpoint. That gets real print resolution and puts the
load on large institutional infrastructure instead of one small site.

Per-item pipeline:

    IA multi-file ({item}__{file}):
        service = iiif.archive.org/image/iiif/3/
                  {item}/{file}_jp2.zip/{file}_jp2/{file}_0000.jp2
    IA single-file:
        iiif.archive.org/iiif/3/{id}/manifest.json -> canvas 0 service id
    UW CONTENTdm (uw{n}):
        service = digitalcollections.lib.washington.edu/iiif/2/reports:{n}

    then  {service}/info.json          -> native width/height
          {service}/full/{w},/0/default.jpg

Two checks guard every download, both learned the hard way:
  * the returned JPEG must not be smaller than info.json declares
    (IIIF servers can silently serve a tile-sized thumbnail with HTTP 200);
  * its aspect ratio must match the gallery record's own aspect field,
    which confirms we grabbed the cover page and not an interior page.

If an upstream IIIF endpoint cannot serve an item (some UW compound
objects return HTTP 501), the gallery's web-sized cover is used as a
fallback so every record still yields a file. The manifest records which
source each file came from.

Politeness: strictly sequential, DELAY seconds between every request,
descriptive User-Agent. These are nonprofit archives and a university
library -- do not add parallelism.

Usage:
    python3 download.py                 # full batch (2,670)
    python3 download.py --limit 3       # first N records
    python3 download.py --only-sample   # one item per source pattern
    python3 download.py --refresh-index # re-read the gallery index
"""

import argparse
import csv
import json
import os
import re
import struct
import subprocess
import sys
import time
import urllib.parse

HERE = os.path.dirname(os.path.abspath(__file__))
IMAGES = os.path.join(HERE, "images")
CACHE = os.path.join(HERE, ".cache")
ITEMS_JSON = os.path.join(CACHE, "items.json")
GALLERY_HTML = os.path.join(CACHE, "gallery_home.html")
MANIFEST_CSV = os.path.join(HERE, "manifest.csv")
FAILURES_LOG = os.path.join(HERE, "failures.log")
CREDIT_TXT = os.path.join(HERE, "CREDIT.txt")

UA = ("AnnualReportArchiveResearch/1.0 (personal, non-commercial research; "
      "contact wyattacurrie@gmail.com)")
GALLERY = "https://annualreport.gallery/"
IA_IMAGE = "https://iiif.archive.org/image/iiif/3/"
IA_MANIFEST = "https://iiif.archive.org/iiif/3/%s/manifest.json"
UW_SERVICE = "https://digitalcollections.lib.washington.edu/iiif/2/reports:%s"
EXPECTED_ITEMS = 2670

DELAY = 2.0
MAX_ATTEMPTS = 5
BACKOFF_START = 4.0
ASPECT_TOLERANCE = 0.04

CREDITS = [
    "Internet Archive (archive.org) -- The Annual Reports Archive collection",
    "McGill University Library -- Canadian Corporate Reports (via Internet Archive)",
    "University of Washington Libraries -- Digital Collections",
    "bitsavers.org",
    "Index and semantic search: Annual Report Archive (annualreport.gallery), "
    "built by Phil Hedayatnia (@philhedayatnia)",
]


class HardStop(Exception):
    """Access refused -- abort rather than routing around it."""


def log_failure(what, err):
    with open(FAILURES_LOG, "a") as fh:
        fh.write("%s\t%s\t%s\n" %
                 (time.strftime("%Y-%m-%dT%H:%M:%S"), what, err))


# ---------------------------------------------------------------- transport

def _curl(url, out_path=None, timeout=300):
    cmd = ["curl", "-sS", "--max-time", str(timeout), "-A", UA, "-L"]
    if out_path:
        cmd += ["-o", out_path, "-w", "%{http_code}"]
    else:
        cmd += ["-w", "\n%{http_code}"]
    cmd.append(url)
    p = subprocess.run(cmd, capture_output=True, text=True)
    if p.returncode != 0:
        stderr = p.stderr.strip()
        if "host_not_allowed" in stderr or "host_not_allowed" in p.stdout:
            raise HardStop("host_not_allowed for %s" % url)
        return None, None, stderr or ("curl exit %d" % p.returncode)
    if out_path:
        return p.stdout.strip(), None, None
    body, _, code = p.stdout.rpartition("\n")
    return code.strip(), body, None


def fetch(url, out_path=None, timeout=300, label=None, retry_4xx=False):
    """Fetch with retry + exponential backoff. HardStop on 403."""
    label = label or url
    delay = BACKOFF_START
    last = "unknown error"
    for attempt in range(1, MAX_ATTEMPTS + 1):
        code, body, err = _curl(url, out_path, timeout)
        if err is None:
            if code == "200":
                return body
            if code == "403":
                raise HardStop("HTTP 403 fetching %s" % url)
            last = "HTTP %s" % code
            # 404/501 are settled answers, not transient -- don't hammer.
            if code in ("404", "410", "501") and not retry_4xx:
                raise RuntimeError("%s: %s" % (last, url))
        else:
            last = err
            if "host_not_allowed" in last:
                raise HardStop(last)
        if attempt < MAX_ATTEMPTS:
            sys.stderr.write("    retry %d/%d in %.0fs (%s) %s\n"
                             % (attempt, MAX_ATTEMPTS, delay, last, label))
            time.sleep(delay)
            delay *= 2
    raise RuntimeError("%s after %d attempts: %s"
                       % (last, MAX_ATTEMPTS, url))


# ---------------------------------------------------------------- indexing

RECORD_RE = re.compile(r'\{"id":"')


def _json_objects(html):
    """Yield each flat JSON object in the page that starts with {"id":"."""
    for m in RECORD_RE.finditer(html):
        start = m.start()
        depth, i, instr, esc = 0, start, False, False
        while i < len(html):
            ch = html[i]
            if instr:
                if esc:
                    esc = False
                elif ch == "\\":
                    esc = True
                elif ch == '"':
                    instr = False
            else:
                if ch == '"':
                    instr = True
                elif ch == "{":
                    depth += 1
                elif ch == "}":
                    depth -= 1
                    if depth == 0:
                        break
            i += 1
        try:
            obj = json.loads(html[start:i + 1])
        except ValueError:
            continue
        if isinstance(obj, dict) and "img" in obj:
            yield obj


def read_index(refresh=False):
    """The gallery embeds its whole catalogue in the homepage payload, so
    one request gets all 2,670 records. Cached on disk thereafter."""
    if refresh or not os.path.exists(GALLERY_HTML):
        print("Fetching gallery index (single request)...")
        html = fetch(GALLERY, timeout=120, label="gallery homepage")
        with open(GALLERY_HTML, "w", encoding="utf-8") as fh:
            fh.write(html)
    else:
        html = open(GALLERY_HTML, encoding="utf-8", errors="replace").read()

    seen, records = set(), []
    for obj in _json_objects(html):
        if obj["id"] not in seen:
            seen.add(obj["id"])
            records.append(obj)
    if len(records) != EXPECTED_ITEMS:
        sys.stderr.write("WARNING: expected %d records, parsed %d\n"
                         % (EXPECTED_ITEMS, len(records)))
    return records


def slugify(text, maxlen=48):
    s = re.sub(r"[^a-z0-9]+", "-", (text or "").lower()).strip("-")
    if len(s) > maxlen:
        s = s[:maxlen].rstrip("-")
    return s or "unknown"


def build_index(records):
    """Normalize records, order chronologically, assign unique filenames."""
    items = []
    for r in records:
        items.append({
            "id": r["id"],
            "company": r.get("o", ""),
            "year": r.get("y", ""),
            "industry": r.get("i", ""),
            "color": r.get("k", ""),
            "region": r.get("r", ""),
            "collection": r.get("c", ""),
            "source_page_url": r.get("s", ""),
            "gallery_img": urllib.parse.urljoin(GALLERY, r["img"]),
            "aspect": r.get("a"),
            "description": r.get("d", ""),
        })
    items.sort(key=lambda i: (i["year"], i["company"].lower(), i["id"]))
    used = {}
    for item in items:
        stem = "%s_%s" % (item["year"] or "0000", slugify(item["company"]))
        n = used.get(stem, 0) + 1
        used[stem] = n
        item["filename"] = "%s.jpg" % (stem if n == 1 else "%s-%d" % (stem, n))
    return items


# ---------------------------------------------------------------- IIIF

def ia_service_from_manifest(identifier):
    """Single-file IA items: the inner jp2 name differs from the identifier,
    so read the manifest and take canvas 0's image service."""
    url = IA_MANIFEST % urllib.parse.quote(identifier, safe="")
    m = json.loads(fetch(url, timeout=120, label="IA manifest"))
    canvases = m.get("items") or []
    if not canvases:
        raise RuntimeError("manifest has no canvases")
    body = canvases[0]["items"][0]["items"][0]["body"]
    services = body.get("service") or []
    if services and services[0].get("id"):
        return services[0]["id"].rstrip("/")
    # Fall back to trimming the IIIF params off the body id.
    return re.sub(r"/full/[^/]+/\d+/[^/]+$", "", body["id"])


def resolve_service(item):
    """Return (service_base_url, requests_used)."""
    ident = item["id"]

    if ident.startswith("uw") and ident[2:].isdigit():
        return UW_SERVICE % ident[2:], 0

    if "__" in ident:
        # IA multi-report item: inner jp2 path is deterministic from the file
        # stem, so no lookup request is needed.
        collection, _, stem = ident.partition("__")
        inner = "%s/%s_jp2.zip/%s_jp2/%s_0000.jp2" % (collection, stem, stem, stem)
        return IA_IMAGE + urllib.parse.quote(inner, safe=""), 0

    return ia_service_from_manifest(ident), 1


def native_size(service_base):
    info = json.loads(fetch(service_base + "/info.json", timeout=120,
                            label="info.json"))
    return int(info["width"]), int(info["height"])


def full_image_url(service_base, width):
    """Explicit sizeByW. /full/max/ and /full/full/ are silently capped at
    tile size by some IIIF servers, so always name the width we want."""
    return "%s/full/%d,/0/default.jpg" % (service_base, width)


# ---------------------------------------------------------------- JPEG probe

SOF_MARKERS = {0xc0, 0xc1, 0xc2, 0xc3, 0xc5, 0xc6, 0xc7,
               0xc9, 0xca, 0xcb, 0xcd, 0xce, 0xcf}


def jpeg_size(path):
    """Width/height from the JPEG SOF marker. No dependencies."""
    with open(path, "rb") as fh:
        if fh.read(2) != b"\xff\xd8":
            raise ValueError("not a JPEG (bad SOI)")
        while True:
            byte = fh.read(1)
            while byte == b"\xff":
                byte = fh.read(1)
            if not byte:
                raise ValueError("no SOF marker found")
            marker = byte[0]
            if marker in (0xd8, 0x01) or 0xd0 <= marker <= 0xd7:
                continue
            head = fh.read(2)
            if len(head) < 2:
                raise ValueError("truncated JPEG")
            length = struct.unpack(">H", head)[0]
            if marker in SOF_MARKERS:
                fh.read(1)
                h, w = struct.unpack(">HH", fh.read(4))
                return w, h
            fh.seek(length - 2, 1)


def complete_jpeg(path, min_bytes=20_000):
    if not os.path.exists(path) or os.path.getsize(path) < min_bytes:
        return False
    try:
        jpeg_size(path)
    except Exception:
        return False
    with open(path, "rb") as fh:
        fh.seek(-2, 2)
        return fh.read(2) == b"\xff\xd9"


def print_size(width, height, dpi):
    return "%.1f x %.1f" % (width / float(dpi), height / float(dpi))


# ---------------------------------------------------------------- output

CSV_COLUMNS = ["id", "company", "year", "industry", "color", "region",
               "source_collection", "image_source", "source_page_url",
               "iiif_url", "width_px", "height_px", "bytes",
               "max_print_in_at_300dpi", "max_print_in_at_150dpi",
               "filename", "description"]


def write_credit():
    with open(CREDIT_TXT, "w") as fh:
        fh.write("Vintage corporate annual report covers, 1945-1992.\n\n")
        fh.write("Scans courtesy of:\n")
        for line in CREDITS:
            fh.write("  * %s\n" % line)
        fh.write("\nEach image's specific source collection and source page "
                 "are recorded per row\nin manifest.csv. Downloaded for "
                 "personal, non-commercial use.\n\n")
        fh.write("Note on rights: these are corporate publications from "
                 "1945-1992. Many are\nstill under copyright even though the "
                 "hosting institutions make the scans\npublicly viewable. "
                 "Fine for private study and personal prints; check the\n"
                 "rights statement on the source page before publication or "
                 "resale.\n")


def load_csv_rows():
    if not os.path.exists(MANIFEST_CSV):
        return {}
    with open(MANIFEST_CSV, newline="") as fh:
        return {r["id"]: r for r in csv.DictReader(fh)}


def write_csv(rows, items):
    with open(MANIFEST_CSV, "w", newline="") as fh:
        w = csv.DictWriter(fh, fieldnames=CSV_COLUMNS)
        w.writeheader()
        for item in items:
            row = rows.get(item["id"])
            if row:
                w.writerow({k: row.get(k, "") for k in CSV_COLUMNS})


# ---------------------------------------------------------------- driver

def download_one(item, path):
    """Fetch the best available image. Returns a manifest row dict."""
    service, _ = resolve_service(item)
    time.sleep(DELAY)

    width, height = native_size(service)
    time.sleep(DELAY)

    url = full_image_url(service, width)
    tmp = path + ".part"
    fetch(url, out_path=tmp, timeout=900, label=item["filename"])

    got_w, got_h = jpeg_size(tmp)
    if got_w < width or got_h < height:
        os.remove(tmp)
        raise RuntimeError("server returned %dx%d, info.json declares %dx%d"
                           % (got_w, got_h, width, height))

    # Aspect check: confirms this is the cover page, not an interior page.
    expected = item.get("aspect")
    if expected:
        actual = got_h / float(got_w)
        if abs(actual - expected) > ASPECT_TOLERANCE:
            os.remove(tmp)
            raise RuntimeError("aspect %.3f != expected %.3f (wrong page?)"
                               % (actual, expected))

    os.replace(tmp, path)
    return url, "upstream-iiif", got_w, got_h, os.path.getsize(path)


def download_gallery_fallback(item, path):
    """Last resort: the gallery's own web-sized derivative."""
    tmp = path + ".part"
    fetch(item["gallery_img"], out_path=tmp, timeout=300,
          label=item["filename"] + " (gallery fallback)")
    got_w, got_h = jpeg_size(tmp)
    os.replace(tmp, path)
    return (item["gallery_img"], "gallery-derivative",
            got_w, got_h, os.path.getsize(path))


def make_row(item, url, source, w, h, size):
    return {
        "id": item["id"],
        "company": item["company"],
        "year": item["year"],
        "industry": item["industry"],
        "color": item["color"],
        "region": item["region"],
        "source_collection": item["collection"],
        "image_source": source,
        "source_page_url": item["source_page_url"],
        "iiif_url": url,
        "width_px": w,
        "height_px": h,
        "bytes": size,
        "max_print_in_at_300dpi": print_size(w, h, 300),
        "max_print_in_at_150dpi": print_size(w, h, 150),
        "filename": item["filename"],
        "description": item["description"],
    }


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--limit", type=int, default=0)
    ap.add_argument("--only-sample", action="store_true",
                    help="one item per source pattern, to verify each path")
    ap.add_argument("--refresh-index", action="store_true")
    ap.add_argument("--no-fallback", action="store_true",
                    help="fail rather than using the gallery derivative")
    args = ap.parse_args()

    os.makedirs(IMAGES, exist_ok=True)
    os.makedirs(CACHE, exist_ok=True)
    write_credit()

    records = read_index(refresh=args.refresh_index)
    with open(ITEMS_JSON, "w") as fh:
        json.dump(records, fh, indent=1)
    items = build_index(records)
    print("%d records indexed (chronological).\n" % len(items))

    todo = items
    if args.only_sample:
        picked, todo = set(), []
        for item in items:
            if item["id"].startswith("uw") and item["id"][2:].isdigit():
                kind = "uw"
            elif "__" in item["id"]:
                kind = "ia-multi"
            else:
                kind = "ia-single"
            if kind not in picked:
                picked.add(kind)
                todo.append(item)
        print("sample: %s\n" % ", ".join(sorted(picked)))
    elif args.limit:
        todo = items[:args.limit]

    rows = load_csv_rows()
    done = fell_back = failed = skipped = 0

    for n, item in enumerate(todo, 1):
        path = os.path.join(IMAGES, item["filename"])
        tag = "[%4d/%4d] %s" % (n, len(todo), item["filename"])

        if complete_jpeg(path) and item["id"] in rows:
            skipped += 1
            print("%s  skip (on disk)" % tag)
            continue

        print("%s\n           %s" % (tag, item["id"]))
        try:
            url, source, w, h, size = download_one(item, path)
        except HardStop as e:
            log_failure(item["id"], "ACCESS REFUSED: %s" % e)
            print("\nACCESS REFUSED: %s" % e)
            print("Stopping immediately -- not routing around it.")
            write_csv(rows, items)
            return 2
        except Exception as e:
            for stale in (path + ".part",):
                if os.path.exists(stale):
                    os.remove(stale)
            if args.no_fallback:
                failed += 1
                log_failure(item["id"], "upstream: %s" % e)
                print("           FAILED: %s" % e)
                time.sleep(DELAY)
                continue
            print("           upstream unavailable (%s)" % e)
            log_failure(item["id"], "upstream: %s -- using gallery derivative" % e)
            time.sleep(DELAY)
            try:
                url, source, w, h, size = download_gallery_fallback(item, path)
                fell_back += 1
            except HardStop as e2:
                log_failure(item["id"], "ACCESS REFUSED: %s" % e2)
                print("\nACCESS REFUSED: %s" % e2)
                write_csv(rows, items)
                return 2
            except Exception as e2:
                failed += 1
                log_failure(item["id"], "gallery fallback: %s" % e2)
                print("           FAILED (both sources): %s" % e2)
                time.sleep(DELAY)
                continue
        else:
            done += 1

        rows[item["id"]] = make_row(item, url, source, w, h, size)
        write_csv(rows, items)
        print("           %dx%d  %.1f MB  %s in @300dpi  [%s]"
              % (w, h, size / 1e6, print_size(w, h, 300), source))
        time.sleep(DELAY)

    write_csv(rows, items)
    print("\nupstream=%d fallback=%d skipped=%d failed=%d  (%d/%d in manifest.csv)"
          % (done, fell_back, skipped, failed, len(rows), len(items)))
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
