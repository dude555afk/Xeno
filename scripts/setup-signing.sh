#!/usr/bin/env bash
# Xeno release signing — one command, Termux only.
#
# Run from anywhere:   bash <(curl -sL https://raw.githubusercontent.com/dude555afk/Xeno/main/scripts/setup-signing.sh)
# Or from the repo:     bash scripts/setup-signing.sh
#
# What it does:
#   1. Installs OpenJDK 17 (keytool lives there)
#   2. Clones Xeno if you don't have it
#   3. Generates ~/xeno-release.jks if it does not exist
#   4. Writes keystore.properties for local builds
#   5. Prints the base64 you paste into four GitHub secrets
#
# The keystore and both passwords never leave your phone. Nothing here is
# committed — keystore.properties is gitignored.
set -euo pipefail

REPO_DIR="$HOME/Xeno"
ALIAS="xeno"

if ! command -v git >/dev/null 2>&1; then
  echo "==> Installing git"
  pkg install git -y
fi

if [ ! -d "$REPO_DIR" ]; then
  echo "==> Cloning Xeno into $REPO_DIR"
  git clone https://github.com/dude555afk/Xeno.git "$REPO_DIR"
fi
cd "$REPO_DIR"

echo "==> Installing OpenJDK 17"
pkg install openjdk-17 -y

KEYSTORE="$HOME/xeno-release.jks"

if [ ! -f "$KEYSTORE" ]; then
  echo "==> Generating $KEYSTORE"
  echo "Pick a strong password. You will need it again for updates — write it down."
  read -s -p "Store password: " STOREPASS; echo
  read -s -p "Confirm store password: " CONFIRM; echo
  if [ "$STOREPASS" != "$CONFIRM" ]; then
    echo "Passwords do not match."; exit 1
  fi
  read -s -p "Key password (Enter to reuse the store password): " KEYPASS; echo
  KEYPASS="${KEYPASS:-$STOREPASS}"

  keytool -genkeypair -v \
    -keystore "$KEYSTORE" \
    -alias "$ALIAS" \
    -keyalg RSA -keysize 4096 \
    -validity 10000 \
    -storepass "$STOREPASS" \
    -keypass "$KEYPASS" \
    -dname "CN=Xeno, OU=Dev, O=Xeno, L=Kolkata, ST=West Bengal, C=IN"
else
  echo "==> $KEYSTORE exists, reusing it"
  read -s -p "Store password: " STOREPASS; echo
  read -s -p "Key password (Enter to reuse the store password): " KEYPASS; echo
  KEYPASS="${KEYPASS:-$STOREPASS}"
fi

cat > keystore.properties <<EOF
storeFile=$KEYSTORE
storePassword=$STOREPASS
keyAlias=$ALIAS
keyPassword=$KEYPASS
EOF

echo ""
echo "==> Back up $KEYSTORE. Lose it and no future update installs — there is no recovery."
echo ""
echo "==> Base64 (one long line — copy it whole):"
echo ""
base64 -w0 "$KEYSTORE"
echo ""
echo ""
echo "==> Next: github.com/dude555afk/Xeno → Settings → Secrets and variables → Actions → New repository secret"
echo "    KEYSTORE_BASE64    = the line above"
echo "    KEYSTORE_PASSWORD = your store password"
echo "    KEY_ALIAS         = xeno"
echo "    KEY_PASSWORD      = your key password"
echo ""
echo "==> Then:  git tag v0.1.0 && git push origin v0.1.0"
echo ""
echo "DONE. CI will decode the keystore, sign the release APK, and apksigner-verify it before uploading."
