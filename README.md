# Xeno Browser

Xeno is being reset on the complete Android implementation from [Candy Browser](https://github.com/sk2andy/candy-browser).

## Imported baseline

- Upstream repository: https://github.com/sk2andy/candy-browser
- Pinned upstream revision: `4d8a2277943b0524e08d86004b495525a074ee18`
- Android app and shared modules are now tracked directly in this repository.
- Upstream documentation is preserved in `UPSTREAM_README.md` and `docs/`.

This baseline is intentionally kept close to upstream until its Android build and core browser flows are verified. Branding, application ID, and Xeno-specific features come after that.

## Build

Requirements: JDK 17, Android SDK 37.1, Android 13 (API 33) or newer.

```bash
./gradlew testFullDebugUnitTest lintFullDebug assembleFullDebug
```

## License

The imported upstream source is distributed under the **PolyForm Noncommercial License 1.0.0**. The original `LICENSE`, `LICENSING.md`, and all third-party notices are retained. Use and redistribution are restricted to the purposes permitted by that license unless the upstream licensor grants additional rights.
