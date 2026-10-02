# 下载内置向量化模型 all-MiniLM-L6-v2 到 src/main/resources/onnx/
# 用法: powershell -File download-model.ps1
# 国内走 hf-mirror 镜像；海外可把 hf-mirror.com 换成 huggingface.co
$ErrorActionPreference = 'Stop'
$dir = Join-Path $PSScriptRoot 'src\main\resources\onnx\all-MiniLM-L6-v2'
New-Item -ItemType Directory -Force $dir | Out-Null

$files = @(
  @{ Out = 'tokenizer.json'; Url = 'https://hf-mirror.com/Xenova/all-MiniLM-L6-v2/resolve/main/tokenizer.json' },
  @{ Out = 'model.onnx';     Url = 'https://hf-mirror.com/Xenova/all-MiniLM-L6-v2/resolve/main/onnx/model.onnx' }
)

foreach ($f in $files) {
  $out = Join-Path $dir $f.Out
  if (Test-Path $out) { Write-Host "已存在，跳过: $out"; continue }
  Write-Host "下载 $($f.Url)"
  curl.exe -sSL --retry 2 --connect-timeout 20 -o $out $f.Url
  if ($LASTEXITCODE -ne 0) { throw "下载失败: $($f.Url)" }
  Write-Host ("完成: {0} ({1:N0} bytes)" -f $out, (Get-Item $out).Length)
}
Write-Host '模型就绪，可直接 mvn spring-boot:run'
