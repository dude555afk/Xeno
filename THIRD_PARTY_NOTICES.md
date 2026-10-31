# Third-party notices

## Mozilla Readability

Xeno bundles [Mozilla Readability](https://github.com/mozilla/readability), the article extraction library used by Firefox Reader View, in `app/src/main/assets/readability.js`. The file includes a small Xeno-specific wrapper that extracts text from a cloned DOM and falls back to text already exposed by the page. It does not bypass access controls or paywalls.

Copyright (c) 2010 Arc90 Inc and Mozilla Readability contributors. Licensed under the Apache License, Version 2.0. See `LICENSES/Apache-2.0.txt` for the license text and the bundled file's copyright header.

Upstream source: https://github.com/mozilla/readability/blob/main/Readability.js

## AndroidHarness

Selected keyless-search and bounded-research patterns in Xeno's search/research path are adapted from:

- Project: https://github.com/Sanuu7/AndroidHarness
- Files reviewed: `app/src/main/java/com/androidharness/app/tools/SearchBackends.kt` and `SearchBudget.kt`
- License: MIT. The original license text is preserved in `third_party/AndroidHarness-LICENSE.txt`.

Xeno's implementation is a focused Kotlin adaptation using its existing OkHttp and coroutine dependencies. The AndroidHarness application, UI, model runtime, and unrelated tools are not included.

## Vane

Vane (https://github.com/ItzCrazyKns/Vane) was reviewed as an architectural reference for iterative planning, focused follow-up searches, and source-grounded synthesis. No Vane source files or runtime dependencies are included in Xeno.
