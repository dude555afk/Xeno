# Xeno experimental browser base: Midori Android

This branch contains an imported snapshot of Midori Android for isolated evaluation as a GeckoView browser foundation.

- Upstream: https://github.com/goastian/midori-android
- Imported upstream commit: 049027a744f7296e4353dcfe08fb10a6845103d0
- License: Mozilla Public License 2.0. Review the upstream LICENSE and all bundled third-party notices before distribution.
- Engine: GeckoView through Mozilla Android Components.
- The source still has Midori package identity and branding. No Xeno rebranding has been applied.
- The upstream Gradle build includes a task that strips Mozilla Glean telemetry and verifies its absence.
- The stable Xeno branches are unchanged.
- This branch is a build-tested candidate, not yet adopted as the final Xeno product.
