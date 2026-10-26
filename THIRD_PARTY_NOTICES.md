# Third-party notices

## Mozilla Readability

Xeno bundles [Mozilla Readability](https://github.com/mozilla/readability), the article extraction library used by Firefox Reader View, in `app/src/main/assets/readability.js`. The file includes a small Xeno-specific wrapper that extracts text from a cloned DOM and falls back to text already exposed by the page. It does not bypass access controls or paywalls.

Copyright (c) 2010 Arc90 Inc and Mozilla Readability contributors. Licensed under the Apache License, Version 2.0. See `LICENSES/Apache-2.0.txt` for the license text and the bundled file's copyright header.

Upstream source: https://github.com/mozilla/readability/blob/main/Readability.js
