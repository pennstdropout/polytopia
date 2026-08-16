#!/usr/bin/env python3
"""
Download Herbert Bayer's "World Geo-Graphic Atlas" (Container Corporation of
America, 1953) from the David Rumsey Map Collection at full IIIF resolution.

Politeness: strictly sequential, one request at a time, DELAY seconds between
every HTTP request, descriptive User-Agent. This is a nonprofit library.

Resolution: Rumsey's IIIF endpoint caps whole-image requests
(full/full, full/max, full/pct:100) at ~1536 px on the long edge, and silently
downsamples any region larger than ~3000 px. Native pixels are only served for
region requests at or below that threshold, so each plate is fetched as a grid
of TILE-sized regions and stitched back together locally. Every tile is checked
against its expected dimensions; if the server downsamples one, the tile size is
halved and the plate is refetched.

Resume: any plate already on disk is skipped. Per-item IIIF metadata is cached
so a resumed run does not re-hit the server just to rebuild manifest.csv.

Abort: a 403 (or a proxy "host_not_allowed") stops the whole run immediately.
"""

import argparse
import csv
import io
import json
import os
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

from PIL import Image

# These plates are ~90 megapixels; well above Pillow's decompression-bomb guard.
Image.MAX_IMAGE_PIXELS = None

UA = ("Bayer-Atlas-Research/1.0 (personal, non-commercial study and printing of "
      "Herbert Bayer's World Geo-Graphic Atlas 1953; contact wyattacurrie@gmail.com)")

SEARCH = "https://www.davidrumsey.com/luna/servlet/as/search"
IIIF = "https://www.davidrumsey.com/luna/servlet/iiif"

# Facets equivalent to the LUNA listing:
#   /view/all/who/Bayer,+Herbert/Container+Corporation+of+America/what/World+Atlas
QUERY = ('who="Bayer, Herbert" who="Container Corporation of America" '
         'what="World Atlas"')
SORT = "Pub_List_No_InitialSort,Pub_Date,Pub_List_No,Series_No"
EXPECTED = 145

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "rumsey-bayer-atlas")
IMAGES = os.path.join(OUT, "images")
CACHE = os.path.join(OUT, ".cache")
DELAY = 2.0
MAX_RETRIES = 5
TILE = 3000        # largest region Rumsey serves at native resolution
MIN_TILE = 512     # give up if halving gets us below this
JPEG_QUALITY = 95

CREDIT = "David Rumsey Map Collection, David Rumsey Map Center, Stanford Libraries\n"


class Abort(Exception):
    """Hard stop - 403 / blocked host. Do not route around it."""


_last_request = [0.0]


def throttle():
    """Sleep so that consecutive requests are >= DELAY seconds apart."""
    wait = DELAY - (time.time() - _last_request[0])
    if wait > 0:
        time.sleep(wait)
    _last_request[0] = time.time()


def fetch(url, binary=False):
    """One polite HTTP GET. Raises Abort on 403/blocked, else returns bytes/str."""
    throttle()
    req = urllib.request.Request(url, headers={"User-Agent": UA})
    try:
        with urllib.request.urlopen(req, timeout=180) as r:
            body = r.read()
    except urllib.error.HTTPError as e:
        detail = ""
        try:
            detail = e.read()[:400].decode("utf-8", "replace")
        except Exception:
            pass
        if e.code == 403 or "host_not_allowed" in detail:
            raise Abort(f"HTTP 403 / blocked host at {url}\n{detail}")
        raise RuntimeError(f"HTTP {e.code} at {url}") from None
    except urllib.error.URLError as e:
        # A denied CONNECT surfaces as URLError, not HTTPError - the proxy
        # rejects the tunnel before any HTTP response exists. Treat those as
        # hard stops too, otherwise a policy denial looks like a flaky socket
        # and the backoff quietly hammers a host that is refusing us.
        reason = str(e.reason)
        if ("host_not_allowed" in reason
                or "403" in reason
                or "Forbidden" in reason
                or "Tunnel connection failed" in reason):
            raise Abort(f"blocked at {url}: {reason}")
        raise RuntimeError(f"{type(e).__name__}: {reason} at {url}") from None
    return body if binary else body.decode("utf-8", "replace")


def fetch_retry(url, binary=False, label=""):
    """fetch() with exponential backoff. Abort propagates immediately."""
    delay = 2.0
    for attempt in range(1, MAX_RETRIES + 1):
        try:
            return fetch(url, binary=binary)
        except Abort:
            raise
        except Exception as e:
            if attempt == MAX_RETRIES:
                raise RuntimeError(f"{label or url}: giving up after "
                                   f"{MAX_RETRIES} attempts - {e}") from None
            print(f"    ! attempt {attempt}/{MAX_RETRIES} failed ({e}); "
                  f"retrying in {delay:.0f}s", flush=True)
            time.sleep(delay)
            delay *= 2


def slugify(text, maxlen=70):
    s = re.sub(r"[^a-z0-9]+", "-", text.lower()).strip("-")
    if len(s) > maxlen:
        s = s[:maxlen].rsplit("-", 1)[0]
    return s or "untitled"


def field(rec, name):
    """Pull a single value out of LUNA's list-of-dicts fieldValues."""
    for entry in rec.get("fieldValues", []):
        if name in entry:
            vals = entry[name]
            return "; ".join(vals) if isinstance(vals, list) else str(vals)
    return ""


def enumerate_items():
    """Page through the result set and return records in the listing's sort order."""
    cached = os.path.join(CACHE, "items.json")
    if os.path.exists(cached):
        with open(cached) as f:
            items = json.load(f)
        print(f"Using cached listing: {len(items)} items")
        return items

    items, os_ = [], 0
    total = None
    while True:
        url = SEARCH + "?" + urllib.parse.urlencode(
            {"q": QUERY, "sort": SORT, "os": os_, "pgs": 50, "fullData": "true"})
        data = json.loads(fetch_retry(url, label=f"listing os={os_}"))
        if total is None:
            total = int(data.get("totalResults", 0))
            print(f"Result set reports {total} items (expected {EXPECTED})")
        batch = data.get("results", [])
        if not batch:
            break
        items.extend(batch)
        print(f"  fetched {len(items)}/{total}")
        # The server caps page size below the requested pgs, so advance by the
        # number of records actually returned rather than by the requested size.
        os_ += len(batch)
        if os_ >= total:
            break

    if total != EXPECTED:
        print(f"WARNING: expected {EXPECTED} items, listing reports {total}")
    if len(items) != total:
        print(f"WARNING: collected {len(items)} records but total says {total}")

    with open(cached, "w") as f:
        json.dump(items, f)
    return items


def process(idx, rec, failures):
    """Fetch info.json + the max-size image for one plate. Returns a manifest row."""
    item_id = rec["id"]
    title = rec.get("displayName", "").strip()
    date = field(rec, "Date")
    order = f"{idx:03d}"
    name = f"{order}_{slugify(title)}.jpg"
    path = os.path.join(IMAGES, name)
    meta_path = os.path.join(CACHE, f"{item_id.replace('~', '_')}.json")

    print(f"[{order}/{EXPECTED}] {title}")

    # --- Resume: image on disk + cached dimensions means nothing to do.
    if os.path.exists(path) and os.path.getsize(path) > 0 and os.path.exists(meta_path):
        with open(meta_path) as f:
            meta = json.load(f)
        print(f"    - already on disk ({os.path.getsize(path):,} bytes), skipping")
        return row(order, name, item_id, title, date, meta["iiif_url"],
                   meta["width"], meta["height"], os.path.getsize(path))

    base = f"{IIIF}/{urllib.parse.quote(item_id, safe='~')}"
    info = json.loads(fetch_retry(base + "/info.json", label=f"{item_id} info.json"))
    width, height = int(info["width"]), int(info["height"])
    # The IIIF image service base. Whole-image requests off this base are capped
    # at ~1536px, so full resolution is assembled from region requests of the
    # form {base}/{x},{y},{w},{h}/full/0/default.jpg
    iiif_url = base
    with open(meta_path, "w") as f:
        json.dump({"iiif_url": iiif_url, "width": width, "height": height}, f)

    if os.path.exists(path) and os.path.getsize(path) > 0:
        print(f"    - already on disk ({os.path.getsize(path):,} bytes), skipping")
        return row(order, name, item_id, title, date, iiif_url, width, height,
                   os.path.getsize(path))

    print(f"    {width} x {height} px -> {name}")
    fetch_tiled(base, width, height, path)
    size = os.path.getsize(path)
    print(f"    saved {size:,} bytes")
    return row(order, name, item_id, title, date, iiif_url, width, height, size)


def fetch_tile(base, x, y, w, h):
    """Fetch one native-resolution region. Returns (PIL image, actual w, h)."""
    url = f"{base}/{x},{y},{w},{h}/full/0/default.jpg"
    blob = fetch_retry(url, binary=True, label=f"tile {x},{y},{w},{h}")
    if not blob.startswith(b"\xff\xd8"):
        raise RuntimeError(f"tile {x},{y} is not a JPEG ({len(blob)} bytes)")
    img = Image.open(io.BytesIO(blob))
    img.load()
    return img


def fetch_tiled(base, width, height, path):
    """Fetch the plate as a grid of native-resolution regions and stitch it."""
    tile = TILE
    while True:
        cols = list(range(0, width, tile))
        rows_ = list(range(0, height, tile))
        total = len(cols) * len(rows_)
        print(f"    fetching {total} tile(s) at {tile}px "
              f"({len(cols)}x{len(rows_)} grid)")
        canvas = Image.new("RGB", (width, height))
        n, downsampled = 0, False
        for y in rows_:
            for x in cols:
                w, h = min(tile, width - x), min(tile, height - y)
                img = fetch_tile(base, x, y, w, h)
                if img.size != (w, h):
                    # Server downsampled this region; retry the plate smaller.
                    print(f"    ! tile {x},{y} came back {img.size}, "
                          f"expected {(w, h)} - halving tile size")
                    downsampled = True
                    break
                canvas.paste(img, (x, y))
                img.close()
                n += 1
                if n % 4 == 0 or n == total:
                    print(f"      tile {n}/{total}", flush=True)
            if downsampled:
                break
        if not downsampled:
            tmp = path + ".part"
            canvas.save(tmp, "JPEG", quality=JPEG_QUALITY, subsampling=0,
                        optimize=True)
            canvas.close()
            os.replace(tmp, path)
            return
        canvas.close()
        tile //= 2
        if tile < MIN_TILE:
            raise RuntimeError(f"server downsamples even {tile * 2}px regions")


def row(order, name, item_id, title, date, iiif_url, w, h, size):
    return {
        "sort_order": order,
        "filename": name,
        "item_id": item_id,
        "title": title,
        "date": date,
        "iiif_url": iiif_url,
        "width_px": w,
        "height_px": h,
        "bytes": size,
        "max_print_in_at_300dpi": f"{w/300:.2f} x {h/300:.2f}",
        "max_print_in_at_150dpi": f"{w/150:.2f} x {h/150:.2f}",
    }


def write_manifest(rows):
    cols = ["sort_order", "filename", "item_id", "title", "date", "iiif_url",
            "width_px", "height_px", "bytes",
            "max_print_in_at_300dpi", "max_print_in_at_150dpi"]
    with open(os.path.join(OUT, "manifest.csv"), "w", newline="") as f:
        w = csv.DictWriter(f, fieldnames=cols)
        w.writeheader()
        for r in sorted(rows, key=lambda r: r["sort_order"]):
            w.writerow(r)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--limit", type=int, default=0,
                    help="stop after N plates (0 = all)")
    ap.add_argument("--rebuild-manifest", action="store_true",
                    help="rebuild manifest.csv from cached metadata and files "
                         "already on disk, without making any request")
    args = ap.parse_args()

    for d in (OUT, IMAGES, CACHE):
        os.makedirs(d, exist_ok=True)
    with open(os.path.join(OUT, "CREDIT.txt"), "w") as f:
        f.write(CREDIT)

    failures_path = os.path.join(OUT, "failures.log")
    rows, failures = [], []

    if args.rebuild_manifest:
        with open(os.path.join(CACHE, "items.json")) as f:
            items = json.load(f)
        missing = []
        for i, rec in enumerate(items, start=1):
            title = rec.get("displayName", "").strip()
            name = f"{i:03d}_{slugify(title)}.jpg"
            path = os.path.join(IMAGES, name)
            meta_path = os.path.join(
                CACHE, f"{rec['id'].replace('~', '_')}.json")
            if not (os.path.exists(path) and os.path.exists(meta_path)):
                missing.append(f"{i:03d}\t{rec['id']}\t{title}\tnot downloaded")
                continue
            with open(meta_path) as f:
                meta = json.load(f)
            rows.append(row(f"{i:03d}", name, rec["id"], title,
                            field(rec, "Date"), meta["iiif_url"],
                            meta["width"], meta["height"],
                            os.path.getsize(path)))
        write_manifest(rows)
        print(f"Rebuilt manifest.csv from disk: {len(rows)} plate(s), "
              f"{len(missing)} still missing.")
        return 0

    try:
        items = enumerate_items()
        for i, rec in enumerate(items, start=1):
            if args.limit and i > args.limit:
                print(f"\nStopping after {args.limit} item(s) as requested.")
                break
            try:
                rows.append(process(i, rec, failures))
            except Abort:
                raise
            except Exception as e:
                msg = f"{i:03d}\t{rec.get('id')}\t{rec.get('displayName')}\t{e}"
                print(f"    FAILED: {e}", flush=True)
                failures.append(msg)
    except Abort as e:
        write_manifest(rows)
        with open(failures_path, "a") as f:
            f.write(f"ABORTED: {e}\n")
        print(f"\n*** ABORTING: {e}", file=sys.stderr)
        print("*** Access was refused. Stopping - not routing around it.",
              file=sys.stderr)
        return 2
    except KeyboardInterrupt:
        write_manifest(rows)
        print("\nInterrupted; progress saved (rerun to resume).")
        return 130

    write_manifest(rows)
    with open(failures_path, "w") as f:
        f.write("\n".join(failures) + ("\n" if failures else ""))
    print(f"\nDone: {len(rows)} plate(s) on disk, {len(failures)} failure(s).")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
