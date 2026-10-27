#!/usr/bin/env python3
"""Compare two same-size Android screenshots without silently resizing them."""

from __future__ import annotations

import argparse
import hashlib
import json
import math
import sys
from pathlib import Path

try:
    import numpy as np
    from PIL import Image
except ImportError as exc:
    print(
        "Missing dependency. Run: python -m pip install pillow numpy",
        file=sys.stderr,
    )
    raise SystemExit(2) from exc


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def load_image(path: Path) -> Image.Image:
    if not path.is_file():
        raise FileNotFoundError(f"Image does not exist: {path}")
    with Image.open(path) as source:
        return source.convert("RGB")


def crop_pair(
    reference: Image.Image,
    candidate: Image.Image,
    crop: tuple[int, int, int, int] | None,
) -> tuple[Image.Image, Image.Image]:
    if crop is None:
        return reference, candidate

    left, top, right, bottom = crop
    width, height = reference.size
    if not (0 <= left < right <= width and 0 <= top < bottom <= height):
        raise ValueError(
            f"Crop {crop} is outside image bounds {width}x{height}"
        )
    return reference.crop(crop), candidate.crop(crop)


def band_stats(diff: np.ndarray) -> list[dict[str, float | int | str]]:
    height = diff.shape[0]
    boundaries = [0, height // 3, (2 * height) // 3, height]
    labels = ["top", "middle", "bottom"]
    bands: list[dict[str, float | int | str]] = []
    for index, label in enumerate(labels):
        start, end = boundaries[index], boundaries[index + 1]
        band = diff[start:end]
        if band.size == 0:
            mae = 0.0
        else:
            mae = float(band.mean())
        bands.append(
            {
                "name": label,
                "top_px": start,
                "bottom_px_exclusive": end,
                "mean_absolute_error": round(mae, 4),
            }
        )
    return bands


def main() -> int:
    parser = argparse.ArgumentParser(
        description=(
            "Create an overlay, amplified pixel diff and JSON metrics for "
            "two same-sized screenshots."
        )
    )
    parser.add_argument("reference", type=Path, help="Arc reference screenshot")
    parser.add_argument("candidate", type=Path, help="Xeno screenshot")
    parser.add_argument(
        "--out-dir",
        type=Path,
        default=Path("artifacts/arc-comparison"),
        help="Output directory (default: artifacts/arc-comparison)",
    )
    parser.add_argument(
        "--crop",
        type=int,
        nargs=4,
        metavar=("LEFT", "TOP", "RIGHT", "BOTTOM"),
        help="Optional identical crop rectangle in screenshot pixels",
    )
    parser.add_argument(
        "--gain",
        type=float,
        default=4.0,
        help="Amplification factor for diff_xN.png (default: 4)",
    )
    args = parser.parse_args()

    if args.gain <= 0:
        parser.error("--gain must be greater than zero")

    try:
        reference_path = args.reference.resolve()
        candidate_path = args.candidate.resolve()
        reference = load_image(reference_path)
        candidate = load_image(candidate_path)

        if reference.size != candidate.size:
            raise ValueError(
                "Screenshots must have identical dimensions; refusing to "
                f"resize silently. Reference={reference.size}, "
                f"candidate={candidate.size}"
            )

        crop = tuple(args.crop) if args.crop is not None else None
        reference, candidate = crop_pair(reference, candidate, crop)
        ref = np.asarray(reference, dtype=np.int16)
        cand = np.asarray(candidate, dtype=np.int16)
        diff = np.abs(ref - cand)
        height, width = diff.shape[:2]

        args.out_dir.mkdir(parents=True, exist_ok=True)
        overlay = Image.blend(reference, candidate, 0.5)
        amplified = np.clip(diff.astype(np.float32) * args.gain, 0, 255)
        amplified_image = Image.fromarray(amplified.astype(np.uint8), mode="RGB")

        overlay.save(args.out_dir / "overlay.png")
        amplified_image.save(args.out_dir / "diff_x4.png")

        mse = float(np.square(diff.astype(np.float64)).mean())
        psnr = math.inf if mse == 0 else 20 * math.log10(255 / math.sqrt(mse))
        pixel_max = diff.max(axis=2)
        channel_mae = diff.mean(axis=(0, 1))

        report = {
            "reference": {
                "path": str(reference_path),
                "sha256": sha256(reference_path),
            },
            "candidate": {
                "path": str(candidate_path),
                "sha256": sha256(candidate_path),
            },
            "crop_xyxy_px": list(crop) if crop is not None else None,
            "comparison": {
                "width_px": width,
                "height_px": height,
                "mean_absolute_error_0_255": round(float(diff.mean()), 4),
                "channel_mae_rgb": {
                    "red": round(float(channel_mae[0]), 4),
                    "green": round(float(channel_mae[1]), 4),
                    "blue": round(float(channel_mae[2]), 4),
                },
                "identical_pixel_fraction": round(
                    float(np.mean(pixel_max == 0)), 6
                ),
                "pixel_fraction_max_channel_delta_le_2": round(
                    float(np.mean(pixel_max <= 2)), 6
                ),
                "psnr_db": None if math.isinf(psnr) else round(psnr, 4),
                "horizontal_bands": band_stats(diff),
                "warning": (
                    "Raw pixel error is not a perceptual score. Dynamic page "
                    "content, system bars and cursor/IME states can dominate."
                ),
            },
        }
        (args.out_dir / "metrics.json").write_text(
            json.dumps(report, indent=2) + "\n",
            encoding="utf-8",
        )
        print(json.dumps(report["comparison"], indent=2))
        print(f"Outputs written to: {args.out_dir.resolve()}")
        return 0
    except (OSError, ValueError) as exc:
        print(f"Comparison failed: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
