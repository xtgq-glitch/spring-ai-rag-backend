#!/usr/bin/env bash
# Download the embedding model all-MiniLM-L6-v2 into src/main/resources/onnx/
#
# Usage:
#   bash download-model.sh           # Hugging Face (default)
#   bash download-model.sh mirror    # hf-mirror.com (mainland China)
set -euo pipefail

SOURCE="${1:-huggingface}"
case "$SOURCE" in
  mirror) BASE="https://hf-mirror.com" ;;
  *)      BASE="https://huggingface.co" ;;
esac
REPO="Xenova/all-MiniLM-L6-v2"
DIR="$(cd "$(dirname "$0")" && pwd)/src/main/resources/onnx/all-MiniLM-L6-v2"
mkdir -p "$DIR"

download() {
  local out="$1"
  local url="$2"
  if [ -f "$out" ]; then
    echo "Already exists, skipping: $out"
    return
  fi
  echo "Downloading $url"
  curl -L --fail --retry 2 --connect-timeout 20 -o "$out" "$url"
  echo "Done: $out ($(wc -c < "$out") bytes)"
}

download "$DIR/tokenizer.json" "$BASE/$REPO/resolve/main/tokenizer.json"
download "$DIR/model.onnx"     "$BASE/$REPO/resolve/main/onnx/model.onnx"

echo "Model ready. You can now run: mvn spring-boot:run"
