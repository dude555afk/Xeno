# Xeno Browser

This branch resets the previous browser experiments and pins the complete Android browser source from [Candy Browser](https://github.com/sk2andy/candy-browser).

## Pinned source

- Upstream: https://github.com/sk2andy/candy-browser
- Revision: `4d8a2277943b0524e08d86004b495525a074ee18`
- Location after checkout: `vendor/candy-browser/`

The upstream project is linked as a Git submodule for now. Its source, Android app, shared Kotlin modules, assets, and build scripts are preserved at that exact revision. This is not yet a vendored copy or an Xeno rebrand; those changes come after the upstream baseline builds.

## Get the Android source

From a fresh clone of this branch:

```bash
git submodule update --init --depth 1 --filter=blob:none
git -C vendor/candy-browser sparse-checkout init --cone
git -C vendor/candy-browser sparse-checkout set app shared pocket-runtime gradle scripts distribution release-notes
```

Sparse checkout keeps the unrelated design-reference archive out of the working tree.

Open `vendor/candy-browser` in Android Studio, or build from a terminal:

```bash
cd vendor/candy-browser
./gradlew testFullDebugUnitTest lintFullDebug assembleFullDebug
```

Requires JDK 17 and Android SDK 37.1. The upstream project targets Android 13 (API 33) and newer.

## Licensing

Candy Browser is distributed under the **PolyForm Noncommercial License 1.0.0**. The original license and licensing scope are retained in this repository. Third-party code, filter data, and bundled assets retain their own licenses and notices. This port is suitable only for permitted non-commercial use unless the upstream licensor grants additional rights.
