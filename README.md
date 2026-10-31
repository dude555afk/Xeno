# Xeno Browser

This branch removes the previous Xeno browser experiments and pins the complete Android browser source from [Candy Browser](https://github.com/sk2andy/candy-browser).

## Pinned upstream source

- Repository: https://github.com/sk2andy/candy-browser
- Revision: `4d8a2277943b0524e08d86004b495525a074ee18`
- Submodule path: `vendor/candy-browser`

The source is currently referenced as a Git submodule, not yet copied into Xeno's own tracked files. The Android build workflow checks out that exact revision with sparse checkout, excluding the unrelated design-reference archive.

## Get a working Android source tree

To avoid pulling the upstream repository's unrelated 489 MB design-reference collection, clone it sparsely:

```bash
git clone --depth 1 --filter=blob:none --sparse https://github.com/sk2andy/candy-browser.git candy-browser
cd candy-browser
git sparse-checkout set app shared pocket-runtime gradle scripts distribution release-notes docs
git fetch --depth 1 origin 4d8a2277943b0524e08d86004b495525a074ee18
git checkout --detach 4d8a2277943b0524e08d86004b495525a074ee18
```

Open that folder in Android Studio, or build from a terminal:

```bash
./gradlew testFullDebugUnitTest lintFullDebug assembleFullDebug
```

Requirements: JDK 17, Android SDK 37.1, Android 13 (API 33) or newer.

## Licensing

The upstream Candy Browser source is distributed under the **PolyForm Noncommercial License 1.0.0**. Its original license and licensing-scope document are retained here. Third-party code, filter data, and bundled assets retain their own licenses and notices. This source can only be used and redistributed for permitted non-commercial purposes unless the upstream licensor grants additional rights.

## Next stage

First verify the upstream Android build. Then make a proper Xeno fork/import and rebrand the app ID, launcher identity, logo, and user-facing strings without deleting upstream notices or changing third-party licenses.
