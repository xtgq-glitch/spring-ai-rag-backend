#!/usr/bin/env bash
# Download the embedding model all-MiniLM-L6-v2 into src/main/resources/onnx/
# Usage: bash download-model.sh
# Fetches from the hf-mirror.com mirror; replace with huggingface.co if reachable directly.
set -euo pipefail

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

download "$DIR/tokenizer.json" "https://hf-mirror.com/Xenova/all-MiniLM-L6-v2/resolve/main/tokenizer.json"
download "$DIR/model.onnx"     "https://hf-mirror.com/Xenova/all-MiniLM-L6-v2/resolve/main/onnx/model.onnx"

echo "Model ready. You can now run: mvn spring-boot:run"
