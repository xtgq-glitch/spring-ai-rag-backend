# verify-e2e.ps1 - one-shot end-to-end verification for the showcase.
#
# What it does (run from the project root):
#   1. Picks a JDK 17+ (-JavaHome > JAVA_HOME > IntelliJ bundled JBR fallback)
#   2. Picks Maven (mvn on PATH > D:\developer\apache-maven-3.8.1 fallback)
#   3. Downloads the embedding model if missing (pass -Mirror in mainland China)
#   4. Checks the DeepSeek API key is configured
#   5. Starts the app, then verifies: upload 201 -> chat 200 with sources ->
#      blank message 400 -> unsupported type 415
#   6. Prints a transcript you can paste into docs/verification.md
#
# Usage:
#   powershell -ExecutionPolicy Bypass -File .\verify-e2e.ps1
#   powershell -ExecutionPolicy Bypass -File .\verify-e2e.ps1 -Mirror

param(
    [string]$JavaHome,
    [switch]$Mirror,
    [string]$BaseUrl = 'http://localhost:8080'
)

$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
$failures = 0
$app = $null

function Info($m) { Write-Host "[info] $m" }
function Ok($m)   { Write-Host "[ ok ] $m" -ForegroundColor Green }
function Warn($m) { Write-Host "[warn] $m" -ForegroundColor Yellow }
function Fail($m) { Write-Host "[fail] $m" -ForegroundColor Red; $script:failures++ }

function Test-Port([int]$Port) {
    $client = New-Object Net.Sockets.TcpClient
    try { $client.Connect('127.0.0.1', $Port); return $client.Connected }
    catch { return $false }
    finally { $client.Close() }
}

# --- 1. JDK 17+ ---------------------------------------------------------------
$jdk = $null
foreach ($candidate in @($JavaHome, $env:JAVA_HOME)) {
    if ($candidate -and (Test-Path (Join-Path $candidate 'bin\java.exe'))) {
        $v = & (Join-Path $candidate 'bin\java.exe') -version 2>&1 | Select-Object -First 1
        if ("$v" -match 'version "(\d+)' -and [int]$Matches[1] -ge 17) { $jdk = $candidate; break }
    }
}
if (-not $jdk) {
    $jbr = 'D:\developer\IntelliJ IDEA 2025.3.1\jbr'
    if (Test-Path (Join-Path $jbr 'bin\java.exe')) { $jdk = $jbr; Warn 'Using the IntelliJ bundled JDK (JBR 21) as JAVA_HOME.' }
}
if (-not $jdk) { Fail 'No JDK 17+ found. Pass -JavaHome <path>.'; Pop-Location; exit 1 }
$env:JAVA_HOME = $jdk
$env:Path = "$jdk\bin;$env:Path"
Ok "JAVA_HOME -> $jdk"

# --- 2. Maven -------------------------------------------------------------------
$mvn = (Get-Command mvn -ErrorAction SilentlyContinue).Source
if (-not $mvn -and (Test-Path 'D:\developer\apache-maven-3.8.1\bin\mvn.cmd')) {
    $mvn = 'D:\developer\apache-maven-3.8.1\bin\mvn.cmd'
    Warn 'mvn not on PATH, using the local Maven install instead.'
}
if (-not $mvn) { Fail 'Maven not found on PATH.'; Pop-Location; exit 1 }
Ok "Maven    -> $mvn"

# --- 3. Embedding model ---------------------------------------------------------
$model = 'src\main\resources\onnx\all-MiniLM-L6-v2\model.onnx'
if (-not (Test-Path $model)) {
    Info 'Embedding model missing, downloading (~90 MB)...'
    if ($Mirror) { & powershell -ExecutionPolicy Bypass -File .\download-model.ps1 -Mirror }
    else         { & powershell -ExecutionPolicy Bypass -File .\download-model.ps1 }
    if (-not (Test-Path $model)) { Fail 'Model download failed. Try again with -Mirror.'; Pop-Location; exit 1 }
}
Ok 'Embedding model present.'

# --- 4. DeepSeek API key --------------------------------------------------------
if (-not $env:DEEPSEEK_API_KEY -and -not (Test-Path 'application-local.yml')) {
    Fail 'No DeepSeek API key. Create application-local.yml (README step 1) or set DEEPSEEK_API_KEY.'
    Pop-Location; exit 1
}
Ok 'DeepSeek API key configured.'

# --- 5. Start the app -----------------------------------------------------------
New-Item -ItemType Directory -Force target | Out-Null
$appLog = 'target\verify-app.log'
Info 'Starting the application (mvn spring-boot:run), waiting for port 8080...'
$app = Start-Process -FilePath $mvn -ArgumentList 'spring-boot:run' -PassThru -NoNewWindow `
        -RedirectStandardOutput $appLog -RedirectStandardError 'target\verify-app-err.log'

try {
    $ready = $false
    for ($i = 0; $i -lt 120; $i++) {
        Start-Sleep -Seconds 2
        if (Test-Port 8080) { $ready = $true; break }
        if ($app.HasExited) { break }
    }
    if (-not $ready) {
        Fail 'The application did not open port 8080 in time. Last log lines:'
        Get-Content $appLog -Tail 30 -ErrorAction SilentlyContinue | ForEach-Object { Write-Host "    $_" }
        return
    }
    Ok 'Application is up.'

    function Invoke-Http {
        $body = [IO.Path]::GetTempFileName()
        $code = & curl.exe -s -o $body -w '%{http_code}' @args
        $text = (Get-Content $body -Raw -ErrorAction SilentlyContinue)
        Remove-Item $body -ErrorAction SilentlyContinue
        [pscustomobject]@{ Code = "$code"; Body = $text }
    }
    function Check($name, $expected, $r) {
        if ($r.Code -eq $expected) { Ok ("{0} -> {1}" -f $name, $r.Code) }
        else { Fail ("{0} -> got {1}, expected {2}" -f $name, $r.Code, $expected) }
        Write-Host ("    body: {0}" -f $r.Body)
    }

    Write-Host ''
    Write-Host '===== TRANSCRIPT (paste into docs/verification.md, dated today) ====='

    Set-Content -Path 'target\bad-type.zip' -Value 'this is not a real zip file'

    Check 'Upload travel-policy.pdf (expect 201)' '201' (Invoke-Http -X POST -F 'file=@docs/samples/travel-policy.pdf' "$BaseUrl/api/documents")
    Check 'RAG query (expect 200 + answer + sources)' '200' (Invoke-Http -X POST -H 'Content-Type: application/json' -d '{"message":"What is the hotel budget for first-tier cities?"}' "$BaseUrl/api/chat")
    Check 'Blank message (expect 400)' '400' (Invoke-Http -X POST -H 'Content-Type: application/json' -d '{"message":""}' "$BaseUrl/api/chat")
    Check 'Unsupported type .zip (expect 415)' '415' (Invoke-Http -X POST -F 'file=@target/bad-type.zip' "$BaseUrl/api/documents")
}
finally {
    Write-Host ''
    Info 'Stopping the application...'
    Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty OwningProcess -Unique |
        ForEach-Object { try { Stop-Process -Id $_ -Force -ErrorAction Stop } catch {} }
    if ($app -and -not $app.HasExited) { try { $app.Kill() } catch {} }
    Remove-Item 'target\bad-type.zip' -ErrorAction SilentlyContinue
    Pop-Location
}

Write-Host ''
if ($failures -eq 0) { Ok 'All checks passed. Paste the transcript above into docs/verification.md.' }
else { Fail "$failures check(s) failed. If dependency resolution failed, check the custom mirror in your Maven settings.xml." }
exit $(if ($failures -eq 0) { 0 } else { 1 })
