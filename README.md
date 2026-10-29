# Xeno Browser

Xeno is an Android browser based on the Nira Browser GeckoView implementation.
This migration replaces the previous prototype browser engine/UI plumbing with an
established GeckoView browser core before further Xeno-specific UI and AI work.

## Current foundation
- GeckoView rendering engine and Mozilla Android Components
- Real tab/session/navigation state and browser back/forward handling
- Firefox WebExtension support
- Private browsing, tab groups, multiple profiles and PWAs
- Existing browser settings, downloads, reader and privacy controls from the upstream base

## Build
- JDK 17
- Android SDK for API 37
- `./gradlew testDebugUnitTest assembleDebug`

## Upstream and license
This project is derived from Nira Browser, licensed under the Mozilla Public License 2.0.
See [UPSTREAM.md](UPSTREAM.md) and [LICENSE](LICENSE) for attribution and license terms.
Xeno-specific branding and changes are maintained in this repository.
