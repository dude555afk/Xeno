# Arc reference capture and comparison

This folder supports evidence-based UI comparison. It does not download Arc or automate its private services.

## Capture a reference

1. Install/launch the Arc Search Android app while it remains available.
2. Choose a repeatable state and wait for dynamic page content to settle.
3. Record Arc version, device model, Android version, display size/density, font scale, refresh rate, navigation mode, keyboard state and tab count.
4. From PowerShell on Windows, with the phone connected and authorized by ADB:

   ```powershell
   adb devices
   adb shell wm size
   adb shell wm density
   adb shell settings get system font_scale
   adb exec-out screencap -p > arc-home.png
   ```

5. Repeat the same state in Xeno and save as xeno-home.png. Do not crop or resize one side only.
6. For motion, make a screen recording of a single interaction and log the trigger. Android's built-in screen recorder or ADB screenrecord can be used; verify actual frame rate rather than assuming it captured 120 fps.

Use a harmless deterministic test page for browser-content comparisons. Hide passwords, personal notifications, open private tabs and other personal information before sharing captures.

## Compare two captures

Install Python 3, Pillow and NumPy:

```powershell
python -m pip install pillow numpy
python tools/arc-reference/compare_screenshots.py arc-home.png xeno-home.png --out-dir artifacts/arc-home
```

The tool requires matching screenshot dimensions and fails rather than silently resizing an image. It writes:
- overlay.png — 50/50 blend of reference and candidate
- diff_x4.png — amplified absolute pixel difference
- metrics.json — image dimensions and basic pixel-error statistics, including equal-size horizontal bands

These metrics are diagnostic, not a substitute for examining the overlay. Dynamic webpage text, cursors, ads, images and status-bar content can dominate the diff; annotate and mask non-target regions in a later measured comparison rather than treating raw MAE as a design score.


## Inspect and annotate measured regions

The manifest contains 30 inspected assets and 123 hand-measured preview-coordinate regions. Region boxes are approximate and explicitly labelled as coordinate-space estimates. They are useful for comparing layout ratios and structuring capture, not for claiming exact dp or source-image dimensions.

To draw the boxes over a locally saved, matching source image:

```powershell
python tools/arc-reference/annotate_measurements.py AA-BFM-01 arc-bfm-1.jpg --out artifacts/arc-bfm-1-annotated.png
```

The tool rejects aspect-ratio mismatches by default rather than silently stretching the measurements.

## Sample exact RGB values from local images

The browser-rendered remote previews were not available as raw local files to the analysis runtime, so no exact source HEX values were fabricated. Once the real image bytes are saved locally, sample exact points or interior rectangles:

```powershell
python tools/arc-reference/sample_pixels.py arc-bfm-1.png --point 25 80 --box "background" 10 100 18 140 --out artifacts/arc-colors.json
```

If the source image has the matching preview aspect ratio, a named manifest region can also be sampled:

```powershell
python tools/arc-reference/sample_pixels.py arc-bfm-1.jpg --asset-id AA-BFM-01 --region bottom-dock --region query-pill --out artifacts/arc-colors.json
```

Outputs include image dimensions and SHA-256, exact-point RGB/HEX, median/mean RGB for rectangles, standard deviation, p10/p90, extrema and most-common exact RGB values. Select interior regions that exclude text, shadows, icons and gradients when trying to identify a flat UI token. The script measures the local bytes supplied to it; it does not claim those bytes are original device pixels if they are from a cropped, resized or promotional image.

## Known evidence limitation

This environment can inspect the visual output of public image links, but the source binaries for those web-rendered previews were not mounted into the local image-processing workspace. Therefore the current repository has:
- individually inspected screens and composite classifications
- preview-coordinate bounding boxes, pixel sizes and normalized ratios
- tools to annotate captures, sample raw RGB and compare matched Arc/Xeno screenshots

It does **not** yet have raw Android captures from the same device, verified source-native resolution/density, exact sampled app colors or frame-timed motion measurements. The next high-confidence pass requires obtaining local original image/video bytes or recording the app directly with ADB. No estimate should be presented as a measured native dp or exact animation value.


## Exact point probes and raw source pixel analyzer

The measurement manifest supports per-asset `samplePoints` in encoded file pixel coordinates. The analyzer stores exact RGB/HEX at each probe alongside ROI center/median/mean/std/quantiles, quantized palette, encoded dimensions, byte count and SHA-256. It intentionally keeps a point's descriptive name separate from a claim that it is a universal design token.

Latest successful pixel run: https://github.com/dude555afk/Xeno/actions/runs/40559673319  
Run artifact: `arc-reference-source-pixel-analysis` (7-day retention). This includes 31 decoded source images, 191 region ROI measurements and 45 exact point probes.

The analyst must still reject points that land on text, antialiasing or a gradient boundary. The manifest's coordinates and ROI selection are inspectable. No raw image binaries are stored in the generated analysis artifact.
