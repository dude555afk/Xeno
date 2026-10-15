# Signing Xeno

A release APK signed with the debug key cannot be updated later. Android refuses
to install over a different signing certificate, so the first real release would
have to ship under a new application ID — new listing, new install, and every
existing copy orphaned with no way to migrate. There is no undo for that.

You do this once. Everything below runs on your phone in Termux; no laptop
needed.



## 1. Generate the keystore

```bash
pkg install openjdk-17 -y

keytool -genkeypair -v \
  -keystore ~/xeno-release.jks \
  -alias xeno \
  -keyalg RSA -keysize 4096 \
  -validity 10000 \
  -storepass 'PICK-A-PASSWORD' \
  -keypass  'PICK-A-PASSWORD' \
  -dname "CN=Xeno, OU=Dev, O=Xeno, L=Kolkata, ST=West Bengal, C=IN"
```

Replace **both** `PICK-A-PASSWORD` occurrences with the same string. Keep
`-alias xeno` verbatim if you want the secret names in step 3 to match.

`-validity 10000` is about 27 years. Google Play requires an upload certificate
valid past 2033, so anything shorter comes back to bite you.

Back this file up somewhere that is not this phone. Lose it and you lose the
ability to ever update the app — that is the whole reason this page exists.


## 2. Base64 it for CI

```bash
base64 -w0 ~/xeno-release.jks > ~/xeno.jks.b64
termux-clipboard-set < ~/xeno.jks.b64
```

That puts one very long line on your clipboard. `cat ~/xeno.jks.b64` works too
if you would rather copy it by hand.


## 3. Add four repository secrets

`github.com/dude555afk/Xeno` → Settings → Secrets and variables → Actions →
New repository secret

| Secret               | Value                          |
|----------------------|--------------------------------|
| `KEYSTORE_BASE64`    | the long base64 line, step 2   |
| `KEYSTORE_PASSWORD`  | the password you picked        |
| `KEY_ALIAS`          | `xeno`                         |
| `KEY_PASSWORD`       | the same password              |

After pasting, clear your clipboard. The keystore is now in two places that
matter: your phone and GitHub.


## 4. Tag a release

```bash
git tag v0.1.0
git push origin v0.1.0
```

The workflow decodes the keystore, signs the release APK, and verifies the
signature before uploading. If the secrets are missing or wrong, the
`Verify release APK signature` step **fails the job** rather than quietly
uploading an unsigned or debug-signed APK. That is intentional — a green build
that shipped something uninstallable is the exact failure this project keeps
paying for.


## Local signed builds

CI signs on every tag, so you normally never need this. When you do:

- copy `keystore.properties.example` to `keystore.properties`, fill it in, and
  run `./gradlew assembleRelease`; **or**
- export `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD`.

Both the properties file and any `*.jks` are gitignored, so the key cannot be
committed by accident.
