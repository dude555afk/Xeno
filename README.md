# Xeno Browser

**Xeno is rebuilding its browser core on an existing GeckoView browser implementation.**

This experimental branch pins [Omni Browser by RebelRoot](https://github.com/REBEL-ROOT/omni-browser) as the browser core and applies Xeno's package name, launcher mark, and branding in CI. This avoids pretending a small homegrown shell is a complete browser. The core includes GeckoView, real tab/session handling, download management, bookmarks/history, WebExtensions, content blocking, site permissions, and browser utilities.

The core is licensed under **GNU GPL v3.0**. Original copyright headers are retained, and the app's About screen shows attribution to the upstream project. Xeno is an independent fork, not endorsed by RebelRoot.

## How this branch works

- `browser-core` is a pinned Git submodule, so the underlying browser code remains traceable to the exact upstream commit.
- `scripts/rebrand_xeno.py` applies Xeno's package ID, app name, launcher icon, and in-app attribution to the checked-out source before the build.
- The Android CI builds and tests the GeckoView core and uploads an ARM64 debug APK.

This is a **baseline for experiments**, not a claim that every feature has been independently tested on Xeno. Successful CI confirms the source compiles and its unit tests run; it does not replace real-device website, download, login, and crash testing.

## Build locally

Clone with submodules, then:

```bash
git clone --recurse-submodules https://github.com/dude555afk/Xeno.git
cd Xeno
python3 scripts/rebrand_xeno.py browser-core
cd browser-core
./gradlew compileAarch64DebugKotlin testUniversalDebugUnitTest assembleAarch64Debug
```

Requires JDK 17 and Android SDK 36.

## Attribution and license

- Upstream project: [Omni Browser by RebelRoot](https://github.com/REBEL-ROOT/omni-browser)
- License: [GPL-3.0](https://github.com/REBEL-ROOT/omni-browser/blob/main/LICENSE)
- Upstream branding/trademark requirements: [TRADEMARK.md](https://github.com/REBEL-ROOT/omni-browser/blob/main/TRADEMARK.md)

The app must be distributed with corresponding source and the GPL notices. No association or endorsement by RebelRoot is implied.
