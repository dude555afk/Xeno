from __future__ import annotations

import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

TOOLS = Path(__file__).resolve().parent
MANIFEST_PATH = TOOLS / "reference_measurements.json"
PYTHON = sys.executable


class ArcReferenceToolTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.manifest = json.loads(MANIFEST_PATH.read_text(encoding="utf-8"))

    def setUp(self) -> None:
        self.temp_dir = tempfile.TemporaryDirectory(prefix="arc-reference-test-")
        self.work = Path(self.temp_dir.name)

    def tearDown(self) -> None:
        self.temp_dir.cleanup()

    def run_script(self, script: str, *args: str) -> subprocess.CompletedProcess[str]:
        return subprocess.run(
            [PYTHON, str(TOOLS / script), *args],
            check=False,
            capture_output=True,
            text=True,
            timeout=30,
        )

    def test_manifest_all_boxes_are_valid_and_normalized(self) -> None:
        seen_ids: set[str] = set()
        for item in self.manifest["items"]:
            self.assertNotIn(item["id"], seen_ids)
            seen_ids.add(item["id"])
            width, height = item["previewCanvasPx"]
            for region in item.get("regions", []):
                left, top, right, bottom = region["bbox"]
                self.assertGreaterEqual(left, 0, (item["id"], region["name"]))
                self.assertGreaterEqual(top, 0, (item["id"], region["name"]))
                self.assertLess(left, right, (item["id"], region["name"]))
                self.assertLess(top, bottom, (item["id"], region["name"]))
                self.assertLessEqual(right, width, (item["id"], region["name"]))
                self.assertLessEqual(bottom, height, (item["id"], region["name"]))
                self.assertEqual(
                    region["sizePreviewPx"],
                    [right - left, bottom - top],
                    (item["id"], region["name"]),
                )
                normalized = [
                    round(left / width, 4),
                    round(top / height, 4),
                    round(right / width, 4),
                    round(bottom / height, 4),
                ]
                self.assertEqual(
                    region["bboxNormalized"],
                    normalized,
                    (item["id"], region["name"]),
                )

    def test_pixel_sampler_reports_exact_local_rgb(self) -> None:
        image_path = self.work / "pixels.png"
        image = Image.new("RGB", (40, 40), (200, 40, 10))
        draw = ImageDraw.Draw(image)
        draw.rectangle((20, 0, 39, 39), fill=(8, 90, 180))
        image.save(image_path)
        output_path = self.work / "sample.json"
        result = self.run_script(
            "sample_pixels.py",
            str(image_path),
            "--point", "2", "2",
            "--box", "warm", "1", "1", "18", "18",
            "--box", "cool", "22", "2", "38", "38",
            "--inset", "0",
            "--out", str(output_path),
        )
        self.assertEqual(result.returncode, 0, result.stderr)
        payload = json.loads(output_path.read_text(encoding="utf-8"))
        self.assertEqual(payload["exact_points"][0]["hex"], "#C8280A")
        color_map = {region["name"]: region for region in payload["regions"]}
        self.assertEqual(color_map["warm"]["median_hex"], "#C8280A")
        self.assertEqual(color_map["cool"]["median_hex"], "#085AB4")

    def test_pixel_sampler_refuses_unknown_aspect_ratio(self) -> None:
        image_path = self.work / "wrong-ratio.png"
        Image.new("RGB", (100, 100), "white").save(image_path)
        result = self.run_script(
            "sample_pixels.py",
            str(image_path),
            "--asset-id", "AA-BFM-01",
            "--region", "bottom-dock",
        )
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("Aspect ratio mismatch", result.stderr)

    def test_annotator_generates_region_overlay(self) -> None:
        image_path = self.work / "reference.png"
        out_path = self.work / "annotated.png"
        Image.new("RGB", (428, 959), (30, 30, 30)).save(image_path)
        result = self.run_script(
            "annotate_measurements.py",
            "AA-BFM-01",
            str(image_path),
            "--out", str(out_path),
        )
        self.assertEqual(result.returncode, 0, result.stderr)
        report = json.loads(result.stdout)
        self.assertEqual(report["asset_id"], "AA-BFM-01")
        self.assertGreaterEqual(report["scaled_box_count"], 10)
        self.assertTrue(out_path.is_file())
        with Image.open(out_path) as rendered:
            self.assertEqual(rendered.size, (428, 959))

    def test_compare_outputs_overlay_diff_and_metrics(self) -> None:
        ref_path = self.work / "arc.png"
        candidate_path = self.work / "xeno.png"
        out_dir = self.work / "comparison"
        reference = Image.new("RGB", (30, 40), (20, 30, 40))
        candidate = reference.copy()
        draw = ImageDraw.Draw(candidate)
        draw.rectangle((0, 0, 4, 4), fill=(220, 200, 180))
        reference.save(ref_path)
        candidate.save(candidate_path)

        result = self.run_script(
            "compare_screenshots.py",
            str(ref_path),
            str(candidate_path),
            "--out-dir",
            str(out_dir),
        )
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertTrue((out_dir / "overlay.png").is_file())
        self.assertTrue((out_dir / "diff_x4.png").is_file())
        metrics = json.loads((out_dir / "metrics.json").read_text(encoding="utf-8"))
        self.assertEqual(metrics["comparison"]["width_px"], 30)
        self.assertEqual(metrics["comparison"]["height_px"], 40)
        self.assertLess(metrics["comparison"]["identical_pixel_fraction"], 1.0)
        self.assertGreater(metrics["comparison"]["mean_absolute_error_0_255"], 0)

    def test_compare_refuses_mismatched_dimensions(self) -> None:
        ref_path = self.work / "arc.png"
        candidate_path = self.work / "xeno.png"
        Image.new("RGB", (20, 20), "black").save(ref_path)
        Image.new("RGB", (21, 20), "black").save(candidate_path)
        result = self.run_script(
            "compare_screenshots.py",
            str(ref_path),
            str(candidate_path),
            "--out-dir",
            str(self.work / "out"),
        )
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("identical dimensions", result.stderr)


if __name__ == "__main__":
    unittest.main(verbosity=2)
