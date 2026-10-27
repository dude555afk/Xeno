#!/usr/bin/env python3
"""Draw the manually measured preview boxes over a locally saved source image.

Use only images you are entitled to use. Source URLs and measurements live in
reference_measurements.json. Boxes scale from preview coordinates to the local
image after checking that the image aspect ratio matches.
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

try:
    from PIL import Image, ImageDraw, ImageFont
except ImportError as exc:
    print("Missing dependency. Run: python -m pip install pillow", file=sys.stderr)
    raise SystemExit(2) from exc

HERE = Path(__file__).resolve().parent
MANIFEST = HERE / "reference_measurements.json"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("asset_id", help="ID in reference_measurements.json")
    parser.add_argument("image", type=Path, help="Locally saved source screenshot")
    parser.add_argument("--out", type=Path, help="Output image path")
    parser.add_argument(
        "--allow-aspect-ratio-mismatch",
        action="store_true",
        help="Allow different aspect ratios (coordinates will stretch in both axes)",
    )
    args = parser.parse_args()

    try:
        manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
        item = next((x for x in manifest["items"] if x["id"] == args.asset_id), None)
        if item is None:
            raise ValueError(f"Unknown asset ID: {args.asset_id}")

        with Image.open(args.image) as source:
            image = source.convert("RGB")

        preview_w, preview_h = item["previewCanvasPx"]
        image_w, image_h = image.size
        preview_ratio = preview_w / preview_h
        image_ratio = image_w / image_h
        ratio_error = abs(image_ratio - preview_ratio) / preview_ratio
        if ratio_error > 0.015 and not args.allow_aspect_ratio_mismatch:
            raise ValueError(
                f"Aspect-ratio mismatch: measurement canvas={preview_w}x{preview_h} "
                f"({preview_ratio:.5f}), supplied image={image_w}x{image_h} "
                f"({image_ratio:.5f}), relative difference={ratio_error:.2%}. "
                "Use the matching full screenshot or pass "
                "--allow-aspect-ratio-mismatch deliberately."
            )

        sx = image_w / preview_w
        sy = image_h / preview_h
        draw = ImageDraw.Draw(image, "RGBA")
        font = ImageFont.load_default()
        line_width = max(2, round(min(image_w, image_h) / 320))
        label_h = max(15, round(14 * sy))

        for index, region in enumerate(item.get("regions", []), start=1):
            left, top, right, bottom = region["bbox"]
            box = (
                round(left * sx),
                round(top * sy),
                round(right * sx),
                round(bottom * sy),
            )
            draw.rectangle(box, outline=(255, 56, 112, 235), width=line_width)
            label = f"{index}. {region['name']}"
            text_box = draw.textbbox((0, 0), label, font=font)
            text_w = text_box[2] - text_box[0]
            label_left = min(max(0, box[0]), max(0, image_w - text_w - 8))
            label_top = max(0, box[1] - label_h - 2)
            draw.rectangle(
                (label_left, label_top, label_left + text_w + 8, label_top + label_h),
                fill=(15, 15, 19, 220),
            )
            draw.text(
                (label_left + 4, label_top + 1),
                label,
                fill=(255, 255, 255, 255),
                font=font,
            )

        output = args.out or args.image.with_name(
            f"{args.image.stem}-{args.asset_id.lower()}-annotated.png"
        )
        output.parent.mkdir(parents=True, exist_ok=True)
        image.save(output)
        print(
            json.dumps(
                {
                    "asset_id": item["id"],
                    "state": item["state"],
                    "source_image_size": [image_w, image_h],
                    "preview_reference_size": [preview_w, preview_h],
                    "ratio_error_percent": round(ratio_error * 100, 3),
                    "scaled_box_count": len(item.get("regions", [])),
                    "confidence": item.get("confidence"),
                    "output": str(output.resolve()),
                    "warning": manifest["caveat"],
                },
                indent=2,
            )
        )
        return 0
    except (OSError, KeyError, TypeError, ValueError, json.JSONDecodeError) as exc:
        print(f"Annotation failed: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
