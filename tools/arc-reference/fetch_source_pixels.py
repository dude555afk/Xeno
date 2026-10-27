#!/usr/bin/env python3
"""Download public image references and measure their actual encoded pixels.

The source files are processed in a temporary directory and are not uploaded.
Only provenance, image metadata, RGB statistics and region measurements are
written to the output JSON artifact.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import sys
import tempfile
import time
import urllib.error
import urllib.request
from datetime import datetime, timezone
from pathlib import Path
from typing import Any
from urllib.parse import urlparse

import numpy as np
from PIL import Image, ImageOps

HERE = Path(__file__).resolve().parent
DEFAULT_MANIFEST = HERE / "reference_measurements.json"
USER_AGENT = (
    "Mozilla/5.0 (compatible; XenoArcReferenceResearch/1.0; "
    "+https://github.com/dude555afk/Xeno)"
)
MAX_BYTES_DEFAULT = 30 * 1024 * 1024
REQUEST_TIMEOUT = 25
REQUEST_DELAY_SECONDS = 0.15


def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def rgb_hex(rgb: list[int] | tuple[int, ...]) -> str:
    return "#{:02X}{:02X}{:02X}".format(*(int(v) for v in rgb[:3]))


def box_stats(array: np.ndarray, bbox: list[int]) -> dict[str, Any]:
    left, top, right, bottom = bbox
    height, width = array.shape[:2]
    if not (0 <= left < right <= width and 0 <= top < bottom <= height):
        return {
            "status": "invalid-scaled-box",
            "bbox_xyxy_px": bbox,
            "image_px": [width, height],
        }

    crop = array[top:bottom, left:right]
    crop_height, crop_width = crop.shape[:2]
    inset_x = min(max(0, round(crop_width * 0.05)), max(0, (crop_width - 1) // 2))
    inset_y = min(max(0, round(crop_height * 0.05)), max(0, (crop_height - 1) // 2))
    core = crop[inset_y:crop_height-inset_y or crop_height,
                inset_x:crop_width-inset_x or crop_width]
    if core.size == 0:
        core = crop

    flat = core.reshape(-1, 3)
    median = np.median(flat, axis=0).round().astype(int)
    mean = flat.mean(axis=0)
    std = flat.std(axis=0)
    p10 = np.percentile(flat, 10, axis=0).round().astype(int)
    p90 = np.percentile(flat, 90, axis=0).round().astype(int)
    center = array[(top + bottom - 1) // 2, (left + right - 1) // 2].astype(int)
    return {
        "status": "measured-from-downloaded-file",
        "bbox_xyxy_px": bbox,
        "box_size_px": [right-left, bottom-top],
        "inset_core_bbox_xyxy_px": [
            left+inset_x, top+inset_y, right-inset_x, bottom-inset_y
        ],
        "sampled_pixel_count": int(flat.shape[0]),
        "center_pixel_rgb": [int(v) for v in center],
        "center_pixel_hex": rgb_hex(center),
        "core_median_rgb": [int(v) for v in median],
        "core_median_hex": rgb_hex(median),
        "core_mean_rgb": [round(float(v), 4) for v in mean],
        "core_standard_deviation_rgb": [round(float(v), 4) for v in std],
        "core_p10_rgb": [int(v) for v in p10],
        "core_p90_rgb": [int(v) for v in p90],
    }


def representative_palette(image: Image.Image, colors: int = 12) -> list[dict[str, Any]]:
    # A quantized palette helps identify dominant colors. These are palette
    # representatives, not exact design tokens; exact local samples are in ROI stats.
    sample = image.copy()
    sample.thumbnail((500, 500), Image.Resampling.LANCZOS)
    quantized = sample.convert("RGB").quantize(colors=colors, method=Image.Quantize.MEDIANCUT)
    palette = quantized.getpalette()
    counts = quantized.getcolors(maxcolors=colors + 1) or []
    total = sum(count for count, _ in counts)
    results = []
    for count, index in sorted(counts, reverse=True):
        pos = index * 3
        rgb = [int(palette[pos]), int(palette[pos+1]), int(palette[pos+2])]
        results.append({
            "rgb": rgb,
            "hex": rgb_hex(rgb),
            "sample_fraction": round(count / total, 6) if total else 0.0,
            "sample_pixel_count": int(count),
        })
    return results


def analyze_one(item: dict[str, Any], max_bytes: int) -> dict[str, Any]:
    url = item.get("sourceUrl", "")
    base: dict[str, Any] = {
        "id": item.get("id"),
        "category": item.get("category"),
        "state": item.get("state"),
        "source_url": url,
        "preview_canvas_px": item.get("coordinateCanvasPx"),
        "preview_coordinate_confidence": item.get("confidence"),
    }
    request = urllib.request.Request(
        url,
        headers={
            "User-Agent": USER_AGENT,
            "Accept": "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8",
        },
        method="GET",
    )
    try:
        with urllib.request.urlopen(request, timeout=REQUEST_TIMEOUT) as response:
            content_type = response.headers.get("Content-Type", "")
            data = response.read(max_bytes + 1)
            final_url = response.geturl()
            status_code = getattr(response, "status", 200)
        if len(data) > max_bytes:
            raise ValueError(f"response exceeded {max_bytes} bytes")
        if not data:
            raise ValueError("empty response body")

        digest = sha256_bytes(data)
        with Image.open(__import__("io").BytesIO(data)) as opened:
            image_format = opened.format
            original_size = opened.size
            original_mode = opened.mode
            # Normalize EXIF orientation before coordinates are applied.
            image = ImageOps.exif_transpose(opened).convert("RGB")
        width, height = image.size
        array = np.asarray(image, dtype=np.uint8)

        preview_w, preview_h = item.get("coordinateCanvasPx", [0, 0])
        preview_ratio = (preview_w / preview_h) if preview_h else None
        image_ratio = (width / height) if height else None
        ratio_error = (
            abs(image_ratio - preview_ratio) / preview_ratio
            if preview_ratio and image_ratio else None
        )

        # Exact per-point probes complement broad ROI statistics. These
        # are exact bytes at named file coordinates; names describe intended
        # locations, not a guarantee that a point is free of text or edges.
        sample_points = []
        for probe in item.get("samplePoints", []):
            x, y = int(probe["x"]), int(probe["y"])
            if 0 <= x < width and 0 <= y < height:
                pixel = [int(v) for v in array[y, x]]
                sample_points.append({
                    "name": probe["name"],
                    "x_px": x,
                    "y_px": y,
                    "rgb": pixel,
                    "hex": rgb_hex(pixel),
                    "status": "measured-from-downloaded-file",
                    "coordinate_basis": probe.get("coordinateBasis", "downloaded-source-pixel-space"),
                })
            else:
                sample_points.append({
                    "name": probe.get("name", "unnamed"),
                    "x_px": x,
                    "y_px": y,
                    "status": "invalid-point",
                    "image_px": [width, height],
                })

        regions = []
        if ratio_error is not None and ratio_error <= 0.015:
            scale_x = width / preview_w
            scale_y = height / preview_h
            for region in item.get("regions", []):
                l, t, r, b = region["bbox"]
                scaled = [
                    round(l * scale_x),
                    round(t * scale_y),
                    round(r * scale_x),
                    round(b * scale_y),
                ]
                regions.append({
                    "name": region["name"],
                    "preview_bbox_xyxy": region["bbox"],
                    **box_stats(array, scaled),
                })
        elif item.get("regions"):
            regions = [{
                "name": region["name"],
                "preview_bbox_xyxy": region["bbox"],
                "status": "skipped-aspect-ratio-mismatch",
                "reason": (
                    "Do not stretch manually measured preview boxes onto a "
                    "source with a different aspect ratio."
                ),
            } for region in item.get("regions", [])]

        return {
            **base,
            "fetch": {
                "status": "ok",
                "http_status": status_code,
                "content_type": content_type,
                "final_url": final_url,
                "downloaded_bytes": len(data),
                "sha256": digest,
            },
            "image": {
                "format": image_format,
                "encoded_dimensions_px": list(original_size),
                "oriented_rgb_dimensions_px": [width, height],
                "encoded_mode": original_mode,
                "aspect_ratio": round(image_ratio, 7) if image_ratio else None,
                "preview_aspect_ratio": round(preview_ratio, 7) if preview_ratio else None,
                "relative_aspect_ratio_error": round(ratio_error, 7) if ratio_error is not None else None,
                "palette_12_quantized": representative_palette(image),
            },
            "point_samples": sample_points,
            "region_measurements": regions,
            "limits": [
                "RGB statistics are exact for the downloaded image bytes; the image may already be rescaled, compressed, cropped, or promotional artwork.",
                "A region statistic aggregates the full rectangle and may include text, icons, gradients or multiple surfaces. It is not automatically a design token.",
                "No display density is assumed, so these pixels are not converted to Android dp.",
            ],
        }
    except (urllib.error.URLError, urllib.error.HTTPError, TimeoutError, OSError, ValueError) as exc:
        return {
            **base,
            "fetch": {
                "status": "error",
                "error_type": type(exc).__name__,
                "error": str(exc)[:500],
                "host": urlparse(url).netloc,
            },
            "region_measurements": [],
        }
    except Exception as exc:  # Keep individual source failures isolated.
        return {
            **base,
            "fetch": {
                "status": "error",
                "error_type": type(exc).__name__,
                "error": str(exc)[:500],
                "host": urlparse(url).netloc,
            },
            "region_measurements": [],
        }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--manifest", type=Path, default=DEFAULT_MANIFEST)
    parser.add_argument("--out", type=Path, required=True)
    parser.add_argument("--max-bytes", type=int, default=MAX_BYTES_DEFAULT)
    parser.add_argument("--delay-ms", type=int, default=150)
    parser.add_argument("--include-excluded-platforms", action="store_true")
    args = parser.parse_args()

    if args.max_bytes <= 0:
        parser.error("--max-bytes must be positive")
    if args.delay_ms < 0:
        parser.error("--delay-ms cannot be negative")

    try:
        manifest = json.loads(args.manifest.read_text(encoding="utf-8"))
        items = manifest.get("items", [])
        # Don't try to measure iOS screenshots as Android references unless asked.
        if not args.include_excluded_platforms:
            items = [
                item for item in items
                if not str(item.get("category", "")).startswith("excluded-other-platform")
            ]
        unique: dict[str, dict[str, Any]] = {}
        for item in items:
            url = item.get("sourceUrl")
            if url:
                unique.setdefault(url, item)

        results = []
        total = len(unique)
        for index, item in enumerate(unique.values(), start=1):
            print(f"[{index}/{total}] {item.get('id')}: fetching {urlparse(item['sourceUrl']).netloc}", flush=True)
            results.append(analyze_one(item, args.max_bytes))
            if index < total and args.delay_ms:
                time.sleep(args.delay_ms / 1000)

        ok = sum(1 for r in results if r.get("fetch", {}).get("status") == "ok")
        errors = [r for r in results if r.get("fetch", {}).get("status") != "ok"]
        measured_regions = sum(
            1 for r in results for region in r.get("region_measurements", [])
            if region.get("status") == "measured-from-downloaded-file"
        )
        report = {
            "generated_at_utc": datetime.now(timezone.utc).isoformat(),
            "manifest_path": str(args.manifest),
            "manifest_asset_records": len(manifest.get("items", [])),
            "unique_source_urls_attempted": total,
            "successful_image_downloads": ok,
            "failed_or_non_image_sources": len(errors),
            "source_native_region_boxes_measured": measured_regions,
            "assets": results,
            "errors": [
                {
                    "id": r.get("id"),
                    "url": r.get("source_url"),
                    **r.get("fetch", {}),
                }
                for r in errors
            ],
        }
        args.out.parent.mkdir(parents=True, exist_ok=True)
        args.out.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
        print(
            json.dumps(
                {
                    "manifest_records": report["manifest_asset_records"],
                    "unique_urls_attempted": total,
                    "successful_images": ok,
                    "errors": len(errors),
                    "source_native_region_boxes_measured": measured_regions,
                    "report": str(args.out),
                    "error_ids": [r.get("id") for r in errors],
                },
                indent=2,
            ),
            flush=True,
        )
        if ok == 0:
            print("No source images could be downloaded and decoded.", file=sys.stderr)
            return 1
        return 0
    except (OSError, json.JSONDecodeError, KeyError, TypeError, ValueError) as exc:
        print(f"Source analysis failed: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
