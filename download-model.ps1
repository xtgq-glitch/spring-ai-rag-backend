# Download the embedding model all-MiniLM-L6-v2 into src/main/resources/onnx/
#
# Usage:
#   powershell -File download-model.ps1            # Hugging Face (default)
#   powershell -File download-model.ps1 -Mirror    # hf-mirror.com (mainland China)
param([switch]$Mirror)

$ErrorActionPreference = 'Stop'
$base = if ($Mirror) { 'https://hf-mirror.com' } else { 'https://huggingface.co' }
$repo = 'Xenova/all-MiniLM-L6-v2'
$dir = Join-Path $PSScriptRoot 'src\main\resources\onnx\all-MiniLM-L6-v2'
New-Item -ItemType Directory -Force $dir | Out-Null

$files = @(
  @{ Out = 'tokenizer.json'; Url = "$base/$repo/resolve/main/tokenizer.json" },
  @{ Out = 'model.onnx';     Url = "$base/$repo/resolve/main/onnx/model.onnx" }
)

foreach ($f in $files) {
  $out = Join-Path $dir $f.Out
  if (Test-Path $out) { Write-Host "Already exists, skipping: $out"; continue }
  Write-Host "Downloading $($f.Url)"
  curl.exe -sSL --retry 2 --connect-timeout 20 -o $out $f.Url
  if ($LASTEXITCODE -ne 0) { throw "Download failed: $($f.Url)" }
  Write-Host ("Done: {0} ({1:N0} bytes)" -f $out, (Get-Item $out).Length)
}
Write-Host 'Model ready. You can now run: mvn spring-boot:run'
