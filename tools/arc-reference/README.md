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
