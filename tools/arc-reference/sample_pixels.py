#!/usr/bin/env python3
"""Sample exact RGB pixels from a locally available screenshot.

This is deliberately separate from the web-preview estimates: exact color
claims are possible only when the image bytes being sampled are available.
Coordinates are in the supplied file's own pixel space.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import sys
from pathlib import Path
from typing import Sequence

try:
    import numpy as np
    from PIL import Image
except ImportError as exc:
    print("Missing dependencies. Run: python -m pip install pillow numpy", file=sys.stderr)
    raise SystemExit(2) from exc

HERE = Path(__file__).resolve().parent
MANIFEST = HERE / "reference_measurements.json"


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def hex_rgb(values: Sequence[int]) -> str:
    return "#{:02X}{:02X}{:02X}".format(*(int(v) for v in values))


def describe_roi(
    rgb: np.ndarray,
    box: tuple[int, int, int, int],
    inset: int,
) -> dict:
    left, top, right, bottom = box
    width, height = rgb.shape[1], rgb.shape[0]
    if not (0 <= left < right <= width and 0 <= top < bottom <= height):
        raise ValueError(
            f"Box {box} is outside image bounds {width}x{height}"
        )

    effective = (
        left + inset,
        top + inset,
        right - inset,
        bottom - inset,
    )
    el, et, er, eb = effective
    if el >= er or et >= eb:
        effective = box
        el, et, er, eb = effective

    roi = rgb[et:eb, el:er].astype(np.uint8)
    flat = roi.reshape(-1, 3)
    median = np.median(flat, axis=0).round().astype(int)
    mean = flat.mean(axis=0)
    std = flat.std(axis=0)
    p10 = np.percentile(flat, 10, axis=0).round().astype(int)
    p90 = np.percentile(flat, 90, axis=0).round().astype(int)
    minimum = flat.min(axis=0)
    maximum = flat.max(axis=0)

    # Most common exact RGB colors can expose a flat token, but anti-aliasing
    # and compression create many one-off values, so this is only descriptive.
    unique, counts = np.unique(flat, axis=0, return_counts=True)
    order = np.argsort(counts)[::-1][:8]
    dominant = [
        {
            "rgb": [int(c) for c in unique[index]],
            "hex": hex_rgb(unique[index]),
            "count": int(counts[index]),
            "fraction": round(float(counts[index] / flat.shape[0]), 6),
        }
        for index in order
    ]

    return {
        "requested_box_xyxy": list(box),
        "sampled_box_xyxy_after_inset": list(effective),
        "sampled_pixel_count": int(flat.shape[0]),
        "median_rgb": [int(v) for v in median],
        "median_hex": hex_rgb(median),
        "mean_rgb": [round(float(v), 3) for v in mean],
        "standard_deviation_rgb": [round(float(v), 3) for v in std],
        "p10_rgb": [int(v) for v in p10],
        "p90_rgb": [int(v) for v in p90],
        "min_rgb": [int(v) for v in minimum],
        "max_rgb": [int(v) for v in maximum],
        "most_common_exact_colors": dominant,
        "warning": (
            "For flat surface tokens, choose an interior ROI that excludes "
            "text, shadows, gradients, icons, transparency and anti-aliased edges."
        ),
    }


def load_manifest_boxes(
    asset_id: str,
    region_names: list[str] | None,
    image_size: tuple[int, int],
    allow_ratio_mismatch: bool,
) -> list[tuple[str, tuple[int, int, int, int]]]:
    manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
    asset = next((item for item in manifest["items"] if item["id"] == asset_id), None)
    if asset is None:
        raise ValueError(f"Unknown asset ID in manifest: {asset_id}")

    base_w, base_h = asset["previewCanvasPx"]
    image_w, image_h = image_size
    base_ratio = base_w / base_h
    image_ratio = image_w / image_h
    ratio_error = abs(image_ratio - base_ratio) / base_ratio
    if ratio_error > 0.015 and not allow_ratio_mismatch:
        raise ValueError(
            f"Aspect ratio mismatch for {asset_id}: manifest canvas "
            f"{base_w}x{base_h} ({base_ratio:.5f}), image {image_w}x{image_h} "
            f"({image_ratio:.5f}), difference {ratio_error:.2%}. Use the exact "
            "matching screenshot or pass --allow-aspect-ratio-mismatch deliberately."
        )

    known_names = [r["name"] for r in asset.get("regions", [])]
    if region_names:
        unknown = sorted(set(region_names) - set(known_names))
        if unknown:
            raise ValueError(
                f"Unknown region(s) for {asset_id}: {unknown}. Known: {known_names}"
            )
    chosen = [
        r for r in asset.get("regions", [])
        if not region_names or r["name"] in region_names
    ]
    if not chosen:
        raise ValueError(f"No region boxes available for asset {asset_id}")

    scale_x = image_w / base_w
    scale_y = image_h / base_h
    output = []
    for region in chosen:
        l, t, r, b = region["bbox"]
        box = (
            round(l * scale_x),
            round(t * scale_y),
            round(r * scale_x),
            round(b * scale_y),
        )
        output.append((region["name"], box))
    return output


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("image", type=Path, help="Locally available PNG/JPEG screenshot")
    parser.add_argument(
        "--box",
        nargs=5,
        action="append",
        metavar=("NAME", "LEFT", "TOP", "RIGHT", "BOTTOM"),
        help="Sample a rectangular region; can be repeated",
    )
    parser.add_argument(
        "--point",
        nargs=2,
        type=int,
        action="append",
        metavar=("X", "Y"),
        help="Sample an exact pixel; can be repeated",
    )
    parser.add_argument(
        "--asset-id",
        help="Also sample named regions from reference_measurements.json",
    )
    parser.add_argument(
        "--region",
        action="append",
        help="Region name to sample for --asset-id; can be repeated",
    )
    parser.add_argument(
        "--inset",
        type=int,
        default=3,
        help="Inset rectangle edges by this many source pixels (default 3)",
    )
    parser.add_argument(
        "--allow-aspect-ratio-mismatch",
        action="store_true",
        help="Permit scaling boxes even if source aspect ratio differs",
    )
    parser.add_argument("--out", type=Path, help="Optional JSON output path")
    args = parser.parse_args()

    if args.inset < 0:
        parser.error("--inset cannot be negative")

    try:
        image_path = args.image.resolve()
        if not image_path.is_file():
            raise FileNotFoundError(f"Image not found: {image_path}")
        with Image.open(image_path) as source:
            image = source.convert("RGB")
        width, height = image.size
        rgb = np.asarray(image, dtype=np.uint8)

        regions: list[tuple[str, tuple[int, int, int, int]]] = []
        for entry in args.box or []:
            name, *coordinates = entry
            x0, y0, x1, y1 = map(int, coordinates)
            regions.append((name, (x0, y0, x1, y1)))

        if args.asset_id:
            regions.extend(
                load_manifest_boxes(
                    args.asset_id,
                    args.region,
                    image.size,
                    args.allow_aspect_ratio_mismatch,
                )
            )
        elif args.region:
            raise ValueError("--region requires --asset-id")

        sampled_points = []
        for x, y in args.point or []:
            if not (0 <= x < width and 0 <= y < height):
                raise ValueError(f"Point {(x, y)} is outside image bounds {width}x{height}")
            pixel = [int(v) for v in rgb[y, x]]
            sampled_points.append(
                {"xy_px": [x, y], "rgb": pixel, "hex": hex_rgb(pixel)}
            )

        if not regions and not sampled_points:
            parser.error("Specify at least one --box, --point, or --asset-id.")

        report = {
            "image": {
                "path": str(image_path),
                "sha256": sha256_file(image_path),
                "width_px": width,
                "height_px": height,
                "mode_sampled": "RGB",
            },
            "inset_px": args.inset,
            "exact_points": sampled_points,
            "regions": [
                {
                    "name": name,
                    **describe_roi(rgb, box, args.inset),
                }
                for name, box in regions
            ],
            "method_note": (
                "Values are exact samples from this local file only. They do not "
                "establish source-device density, and composite assets may include "
                "marketing artwork rather than raw app pixels."
            ),
        }
        payload = json.dumps(report, indent=2) + "\n"
        if args.out:
            args.out.parent.mkdir(parents=True, exist_ok=True)
            args.out.write_text(payload, encoding="utf-8")
        print(payload, end="")
        return 0
    except (OSError, ValueError, KeyError, TypeError, json.JSONDecodeError) as exc:
        print(f"Sampling failed: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
