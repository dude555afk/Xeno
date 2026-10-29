#!/usr/bin/env python3
"""Apply Xeno branding to the pinned upstream GeckoView browser checkout."""
from __future__ import annotations

import re
import shutil
import sys
from pathlib import Path

if len(sys.argv) != 2:
    raise SystemExit("usage: rebrand_xeno.py <browser-core-checkout>")

core = Path(sys.argv[1]).resolve()
if not (core / "app/build.gradle.kts").is_file():
    raise SystemExit(f"not an upstream browser checkout: {core}")

repo_url = "https://github.com/dude555afk/Xeno"
upstream_url = "https://github.com/REBEL-ROOT/omni-browser"
branding = Path(__file__).resolve().parents[1] / "branding" / "xeno_mark.xml"
app_res = core / "app/src/main/res"
mark = app_res / "drawable/xeno_mark.xml"
mark.parent.mkdir(parents=True, exist_ok=True)
shutil.copyfile(branding, mark)

text_suffixes = {
    ".kt", ".java", ".xml", ".gradle", ".kts", ".properties", ".json",
    ".js", ".css", ".html", ".txt", ".md", ".yml", ".yaml", ".toml",
    ".pro", ".svg", ".sh", ".py", ".sql", ".ini", ".conf", ".xml.in",
}
skip_names = {"README.md", "LICENSE", "TRADEMARK.md"}
replacements = [
    ("com.rebelroot.omni", "com.dude555afk.xeno"),
    ("Omni Browser", "Xeno Browser"),
    ("Omni Sync", "Xeno Sync"),
    ("Omni Player", "Xeno Player"),
    ("Omni Config", "Xeno Config"),
    ("https://www.rebelroot.xyz/omnibrowser/privacy-policy", repo_url),
    ("https://www.rebelroot.xyz/omnibrowser", repo_url),
    ("https://github.com/REBEL-ROOT/omni-browser", repo_url),
    ("REBEL-ROOT/omni-browser", "dude555afk/Xeno"),
]
drawable_replacements = [
    ("R.drawable.omni_home_logo_light", "R.drawable.xeno_mark"),
    ("R.drawable.omni_home_logo", "R.drawable.xeno_mark"),
    ("R.drawable.ic_omni_logo_dark", "R.drawable.xeno_mark"),
    ("R.drawable.ic_omni_logo_white", "R.drawable.xeno_mark"),
    ("R.drawable.ic_omni_ring_dark", "R.drawable.xeno_mark"),
    ("R.drawable.ic_omni_ring_light", "R.drawable.xeno_mark"),
    ("R.drawable.ic_omni_logo", "R.drawable.xeno_mark"),
    ("R.drawable.ic_launcher_fg_ring", "R.drawable.xeno_mark"),
    ("@drawable.omni_home_logo_light", "@drawable/xeno_mark"),
    ("@drawable.omni_home_logo", "@drawable/xeno_mark"),
    ("@drawable/ic_omni_logo_dark", "@drawable/xeno_mark"),
    ("@drawable/ic_omni_logo_white", "@drawable/xeno_mark"),
    ("@drawable/ic_omni_ring_dark", "@drawable/xeno_mark"),
    ("@drawable/ic_omni_ring_light", "@drawable/xeno_mark"),
    ("@drawable/ic_omni_logo", "@drawable/xeno_mark"),
    ("@drawable/ic_launcher_fg_ring", "@drawable/xeno_mark"),
]
for path in core.rglob("*"):
    if not path.is_file() or ".git" in path.parts or path.name in skip_names:
        continue
    if path.suffix.lower() not in text_suffixes and path.name not in {
        ".gitignore", "gradlew", "gradlew.bat"
    }:
        continue
    try:
        source = path.read_text(encoding="utf-8")
    except (UnicodeDecodeError, OSError):
        continue
    updated = source
    for old, new in replacements:
        updated = updated.replace(old, new)
    for old, new in drawable_replacements:
        updated = updated.replace(old, new)

    if path.name == "strings.xml":
        updated = re.sub(
            r'(<string name="app_name">).*?(</string>)',
            r'\1Xeno Browser\2', updated, flags=re.S
        )
        updated = re.sub(
            r'(<string name="website_omnibrowser">).*?(</string>)',
            r'\1Xeno on GitHub\2', updated, flags=re.S
        )
        updated = re.sub(
            r'(<string name="website_omnibrowser_desc">).*?(</string>)',
            r'\1Project source, releases, and issue tracker\2', updated, flags=re.S
        )
    if path.name == "settings.gradle.kts":
        updated = updated.replace('rootProject.name = "Omni Browser"', 'rootProject.name = "Xeno Browser"')
    if path.name == "build.gradle.kts" and path.parent.name == "app":
        updated = updated.replace('val baseVersionName = "1.3.5.1"', 'val baseVersionName = "0.1.0"')
        updated = re.sub(
            r'debug \{\s*\n\s*isMinifyEnabled = false',
            'debug {\n            applicationIdSuffix = ".debug"\n            versionNameSuffix = "-debug"\n            isMinifyEnabled = false',
            updated, count=1
        )
        if 'applicationId = "com.dude555afk.xeno"' not in updated:
            raise SystemExit("Xeno applicationId was not applied to app/build.gradle.kts")

    # Replace the upstream logo art in app icons and splash screens with Xeno's mark.
    if path.name in {"ic_splash_logo.xml", "ic_launcher_foreground_dark.xml", "ic_launcher_foreground_default.xml"}:
        updated = branding.read_text(encoding="utf-8")

    if updated != source:
        path.write_text(updated, encoding="utf-8")

# Ensure the theme and all launcher variants point at the Xeno mark.
for path in (core / "app/src/main/res").rglob("*.xml"):
    try:
        text = path.read_text(encoding="utf-8")
    except (UnicodeDecodeError, OSError):
        continue
    if "mipmap-anydpi-v26" in str(path) or "ic_splash_logo" in path.name:
        new = text.replace("@drawable/ic_launcher_fg_ring", "@drawable/xeno_mark")
        new = new.replace("@drawable/ic_omni_logo_dark", "@drawable/xeno_mark")
        new = new.replace("@drawable/ic_omni_logo_white", "@drawable/xeno_mark")
        if path.name == "ic_splash_logo.xml":
            new = branding.read_text(encoding="utf-8")
        if new != text:
            path.write_text(new, encoding="utf-8")

# The upstream raster logos are no longer used. Remove them from the build input
# so no Omni-branded artwork is packaged into Xeno.
for relative in [
    "app/src/main/assets/ic_omni_logo.svg",
    "app/src/main/res/drawable-nodpi/ic_launcher_fg_ring.png",
    "app/src/main/res/drawable-nodpi/omni_home_logo.webp",
    "app/src/main/res/drawable-nodpi/omni_home_logo_light.webp",
    "app/src/main/res/drawable/ic_omni_logo.webp",
    "app/src/main/res/drawable/ic_omni_ring_dark.png",
    "app/src/main/res/drawable/ic_omni_ring_light.png",
    "app/src/main/res/drawable/ic_omni_logo_dark.xml",
    "app/src/main/res/drawable/ic_omni_logo_white.xml",
]:
    file_path = core / relative
    if file_path.exists():
        file_path.unlink()

# Keep visible upstream attribution in the app's About/Community card.
settings = core / "app/src/main/java/com/rebelroot/omni/settings/SettingsScreen.kt"
if not settings.exists():
    settings = next(core.glob("app/src/main/java/**/SettingsScreen.kt"), None)
if settings is None:
    raise SystemExit("SettingsScreen.kt was not found; refusing to ship without in-app attribution.")
text = settings.read_text(encoding="utf-8")
support_line = 'NavRow(Icons.Rounded.Code, stringResource(id = R.string.support_github), stringResource(id = R.string.support_github_desc), onClick = { onOpenUrl("https://github.com/dude555afk/Xeno") })'
credit_line = 'NavRow(Icons.Rounded.Info, "Upstream project credit", "Based on Omni Browser by RebelRoot", onClick = { onOpenUrl("https://github.com/REBEL-ROOT/omni-browser") })\n                    HorizontalDivider(color = dividerColor, modifier = Modifier.padding(horizontal = 16.dp))\n                    ' + support_line
if "Based on Omni Browser by RebelRoot" not in text:
    if support_line not in text:
        raise SystemExit("GitHub row anchor changed; cannot add the required upstream credit safely.")
    text = text.replace(support_line, credit_line, 1)
settings.write_text(text, encoding="utf-8")

# Hard checks: fail CI rather than silently emitting an incorrectly branded package.
gradle = (core / "app/build.gradle.kts").read_text(encoding="utf-8")
strings = (core / "app/src/main/res/values/strings.xml").read_text(encoding="utf-8")
manifest = (core / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
if 'namespace = "com.dude555afk.xeno"' not in gradle:
    raise SystemExit("Xeno namespace missing")
if 'applicationId = "com.dude555afk.xeno"' not in gradle:
    raise SystemExit("Xeno applicationId missing")
if '<string name="app_name">Xeno Browser</string>' not in strings:
    raise SystemExit("Xeno launcher label missing")
if "com.rebelroot.omni" in manifest:
    raise SystemExit("Old package name remains in the manifest")
if "Based on Omni Browser by RebelRoot" not in settings.read_text(encoding="utf-8"):
    raise SystemExit("In-app upstream credit missing")
print("Xeno rebrand applied: applicationId, namespace, launcher label, Xeno mark, source attribution.")
