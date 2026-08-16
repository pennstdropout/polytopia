#!/usr/bin/env python3
"""
Download every plate of Herbert Bayer's *World Geo-Graphic Atlas*
(Container Corporation of America, 1953) from the David Rumsey Map
Collection at the maximum resolution the IIIF Image API will serve.

Pipeline per item:
    LUNA search API  ->  item id
    IIIF manifest    ->  image service base URL
    info.json        ->  native width/height
    {base}/full/full/0/default.jpg  ->  full-resolution JPEG

Politeness: strictly sequential, one request at a time, DELAY seconds
between every request, descriptive User-Agent. This is a nonprofit
library -- do not add parallelism.

Resume: any image already on disk with a plausible size is skipped.
Failures are appended to failures.log and never abort the run.
A 403 or a proxy host_not_allowed aborts the whole run immediately.

Usage:
    python3 download.py            # full batch (all 145)
    python3 download.py --limit 1  # first plate only
    python3 download.py --refresh-index   # re-enumerate from LUNA
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
ITEMS_RAW = os.path.join(CACHE, "items_raw.json")
MANIFEST_CSV = os.path.join(HERE, "manifest.csv")
FAILURES_LOG = os.path.join(HERE, "failures.log")
CREDIT_TXT = os.path.join(HERE, "CREDIT.txt")

UA = ("BayerAtlasResearch/1.0 (personal, non-commercial research; "
      "contact wyattacurrie@gmail.com)")
SEARCH = "https://www.davidrumsey.com/luna/servlet/as/search"
QUERY = ('who="Bayer, Herbert" AND who="Container Corporation of America" '
         'AND what="World Atlas"')
SORT = "Pub_List_No_InitialSort,Pub_Date,Pub_List_No,Series_No"
COLLECTION = "RUMSEY~8~1"
EXPECTED_ITEMS = 145

DELAY = 2.0          # seconds between every request
MAX_ATTEMPTS = 5     # per request
BACKOFF_START = 4.0  # seconds, doubled each retry

CREDIT = ("David Rumsey Map Collection, David Rumsey Map Center, "
          "Stanford Libraries")


class HardStop(Exception):
    """Access was refused -- abort the run rather than routing around it."""


def log_failure(what, err):
    with open(FAILURES_LOG, "a") as fh:
        fh.write("%s\t%s\t%s\n" %
                 (time.strftime("%Y-%m-%dT%H:%M:%S"), what, err))


# ---------------------------------------------------------------- transport

def _curl(url, out_path=None, timeout=300):
    """One curl attempt. Returns (http_code, body_or_None, error_or_None)."""
    cmd = ["curl", "-sS", "--max-time", str(timeout), "-A", UA]
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


def fetch(url, out_path=None, timeout=300, label=None):
    """Fetch with retry + exponential backoff. Raises HardStop on 403."""
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

def enumerate_items():
    """Page through the LUNA search API for the full 145-item result set."""
    results, seen, offset, total = [], set(), 0, None
    while True:
        url = SEARCH + "?" + urllib.parse.urlencode({
            "q": QUERY, "lc": COLLECTION, "sort": SORT,
            "os": offset, "pgs": 50, "fullData": "true"})
        data = json.loads(fetch(url, timeout=120, label="search os=%d" % offset))
        total = int(data["totalResults"])
        batch = data["results"]
        if not batch:
            break
        for r in batch:
            if r["id"] not in seen:
                seen.add(r["id"])
                results.append(r)
        print("  index: os=%-4d %d/%d" % (offset, len(results), total))
        offset += len(batch)
        if offset >= total:
            break
        time.sleep(DELAY)
    if total != EXPECTED_ITEMS or len(results) != EXPECTED_ITEMS:
        sys.stderr.write("WARNING: expected %d items, LUNA reports %s, got %d\n"
                         % (EXPECTED_ITEMS, total, len(results)))
    return results


def flatten_fields(record):
    out = {}
    for entry in record.get("fieldValues", []):
        for key, value in entry.items():
            out[key] = value[0] if isinstance(value, list) and value else value
    return out


def slugify(text, maxlen=60):
    s = text.lower()
    s = re.sub(r"[^a-z0-9]+", "-", s).strip("-")
    if len(s) > maxlen:
        s = s[:maxlen].rstrip("-")
    return s or "untitled"


def build_index(records):
    """Normalize records and order them by the atlas's own plate sequence."""
    items = []
    for rec in records:
        f = flatten_fields(rec)
        try:
            series = int(f.get("Series No", "0"))
        except ValueError:
            series = 0
        title = f.get("Short Title") or rec.get("displayName") or f.get("Full Title", "")
        items.append({
            "item_id": rec["id"],
            "series_no": series,
            "list_no": f.get("List No", ""),
            "title": title,
            "full_title": f.get("Full Title", ""),
            "date": f.get("Date", ""),
            "manifest_url": rec["iiifManifest"],
        })
    items.sort(key=lambda i: (i["series_no"], i["list_no"]))
    for n, item in enumerate(items, 1):
        item["sort_order"] = n
        item["filename"] = "%03d_%s.jpg" % (n, slugify(item["title"]))
    return items


# ---------------------------------------------------------------- IIIF

def image_service(manifest_url):
    """Manifest -> (service base URL, declared width, declared height)."""
    m = json.loads(fetch(manifest_url, timeout=120, label="manifest"))
    canvas = m["sequences"][0]["canvases"][0]
    resource = canvas["images"][0]["resource"]
    base = resource["service"]["@id"].rstrip("/")
    return base, resource.get("width"), resource.get("height")


def native_size(service_base):
    """info.json -> (width, height) of the full-resolution source image."""
    info = json.loads(fetch(service_base + "/info.json", timeout=120,
                            label="info.json"))
    return int(info["width"]), int(info["height"])


def full_image_url(service_base, width):
    """Maximum-size IIIF request.

    Rumsey's IIIF server silently caps /full/full/, /full/max/ and
    /full/pct:100/ at the 1536px tile size -- they return a thumbnail, not
    the source image. Explicit sizeByW is honoured at full resolution, so
    ask for the native width read from info.json.
    """
    return "%s/full/%d,/0/default.jpg" % (service_base, width)


# ---------------------------------------------------------------- JPEG probe

def jpeg_size(path):
    """Read width/height from the JPEG SOF marker. No dependencies."""
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
            length_bytes = fh.read(2)
            if len(length_bytes) < 2:
                raise ValueError("truncated JPEG")
            length = struct.unpack(">H", length_bytes)[0]
            if marker in (0xc0, 0xc1, 0xc2, 0xc3, 0xc5, 0xc6, 0xc7,
                          0xc9, 0xca, 0xcb, 0xcd, 0xce, 0xcf):
                fh.read(1)  # sample precision
                h, w = struct.unpack(">HH", fh.read(4))
                return w, h
            fh.seek(length - 2, 1)


def print_size(width, height, dpi):
    return "%.1f x %.1f" % (width / float(dpi), height / float(dpi))


# ---------------------------------------------------------------- driver

def already_have(path):
    """Resume check: a complete-looking JPEG on disk is left alone."""
    if not os.path.exists(path) or os.path.getsize(path) < 50_000:
        return False
    try:
        jpeg_size(path)
    except Exception:
        return False
    with open(path, "rb") as fh:
        fh.seek(-2, 2)
        return fh.read(2) == b"\xff\xd9"  # EOI marker: not truncated


def write_credit():
    with open(CREDIT_TXT, "w") as fh:
        fh.write(CREDIT + "\n\n")
        fh.write("Herbert Bayer, ed., World Geo-Graphic Atlas: A Composite of "
                 "Man's Environment.\n")
        fh.write("Privately printed for Container Corporation of America, "
                 "1953.\n\n")
        fh.write("Images downloaded from the David Rumsey Map Collection IIIF "
                 "Image API\n(https://www.davidrumsey.com/) for personal, "
                 "non-commercial use.\n")


CSV_COLUMNS = ["item_id", "title", "date", "iiif_url", "width_px", "height_px",
               "bytes", "max_print_in_at_300dpi", "max_print_in_at_150dpi"]


def load_csv_rows():
    if not os.path.exists(MANIFEST_CSV):
        return {}
    with open(MANIFEST_CSV, newline="") as fh:
        return {r["item_id"]: r for r in csv.DictReader(fh)}


def write_csv(rows, items):
    """Rewrite manifest.csv in plate order from the accumulated row map."""
    with open(MANIFEST_CSV, "w", newline="") as fh:
        w = csv.DictWriter(fh, fieldnames=CSV_COLUMNS)
        w.writeheader()
        for item in items:
            row = rows.get(item["item_id"])
            if row:
                w.writerow({k: row.get(k, "") for k in CSV_COLUMNS})


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--limit", type=int, default=0,
                    help="stop after N items (0 = all)")
    ap.add_argument("--refresh-index", action="store_true",
                    help="re-enumerate the result set from LUNA")
    args = ap.parse_args()

    os.makedirs(IMAGES, exist_ok=True)
    os.makedirs(CACHE, exist_ok=True)
    write_credit()

    if args.refresh_index or not os.path.exists(ITEMS_RAW):
        print("Enumerating result set from LUNA search API...")
        records = enumerate_items()
        with open(ITEMS_RAW, "w") as fh:
            json.dump(records, fh, indent=1)
    else:
        records = json.load(open(ITEMS_RAW))

    items = build_index(records)
    print("%d items indexed (plate order by Series No).\n" % len(items))

    rows = load_csv_rows()
    todo = items[:args.limit] if args.limit else items
    downloaded = skipped = failed = 0

    for item in todo:
        path = os.path.join(IMAGES, item["filename"])
        tag = "[%3d/%3d] %s" % (item["sort_order"], len(items), item["filename"])

        if already_have(path) and item["item_id"] in rows:
            skipped += 1
            print("%s  skip (on disk)" % tag)
            continue

        print(tag)
        try:
            base, _, _ = image_service(item["manifest_url"])
            time.sleep(DELAY)

            width, height = native_size(base)
            time.sleep(DELAY)

            url = full_image_url(base, width)
            tmp = path + ".part"
            fetch(url, out_path=tmp, timeout=900, label=item["filename"])

            got_w, got_h = jpeg_size(tmp)
            size = os.path.getsize(tmp)

            # Never keep a silently downsampled file -- the whole point is
            # print resolution. Treat a short image as a failure.
            if got_w < width or got_h < height:
                os.remove(tmp)
                raise RuntimeError(
                    "server returned %dx%d but info.json declares %dx%d"
                    % (got_w, got_h, width, height))
            os.replace(tmp, path)

            rows[item["item_id"]] = {
                "item_id": item["item_id"],
                "title": item["title"],
                "date": item["date"],
                "iiif_url": url,
                "width_px": got_w,
                "height_px": got_h,
                "bytes": size,
                "max_print_in_at_300dpi": print_size(got_w, got_h, 300),
                "max_print_in_at_150dpi": print_size(got_w, got_h, 150),
            }
            write_csv(rows, items)
            downloaded += 1
            print("    %dx%d  %.1f MB  ->  %s in @300dpi / %s in @150dpi"
                  % (got_w, got_h, size / 1e6,
                     print_size(got_w, got_h, 300),
                     print_size(got_w, got_h, 150)))

        except HardStop as e:
            log_failure(item["item_id"], "ACCESS REFUSED: %s" % e)
            print("\nACCESS REFUSED: %s" % e)
            print("Stopping immediately -- not routing around it.")
            return 2
        except Exception as e:
            failed += 1
            log_failure(item["item_id"], "%s: %s" % (item["filename"], e))
            print("    FAILED: %s" % e)
            for stale in (path + ".part",):
                if os.path.exists(stale):
                    os.remove(stale)

        time.sleep(DELAY)

    write_csv(rows, items)
    print("\ndownloaded=%d skipped=%d failed=%d  (%d/%d in manifest.csv)"
          % (downloaded, skipped, failed, len(rows), len(items)))
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
