param(
  [int]$ApiPort = 18088
)

$ErrorActionPreference = "Stop"
$repoRoot = $PSScriptRoot
Set-Location $repoRoot

function Ok($message) {
  Write-Host "[OK] $message" -ForegroundColor Green
}

function Fail($message) {
  Write-Host "[FAIL] $message" -ForegroundColor Red
  throw $message
}

function Require-Path($path, $message) {
  if (-not (Test-Path -LiteralPath $path)) {
    Fail $message
  }
  Ok $message
}

function Test-PortInUse($port) {
  $connection = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue | Select-Object -First 1
  return [bool]$connection
}

function Find-FreePort($preferredPort) {
  if (-not (Test-PortInUse $preferredPort)) {
    return $preferredPort
  }

  for ($port = 18089; $port -le 18150; $port++) {
    if (-not (Test-PortInUse $port)) {
      Write-Host "[INFO] Port $preferredPort is in use. Using free port $port for verification." -ForegroundColor Yellow
      return $port
    }
  }

  Fail "No free verification port found in range 18089-18150."
}

$javaExe = Join-Path $repoRoot "runtime\jdk\bin\java.exe"
$javacExe = Join-Path $repoRoot "runtime\jdk\bin\javac.exe"
$nodeExe = Join-Path $repoRoot "runtime\node\node.exe"
$viteJs = Join-Path $repoRoot "frontend\node_modules\vite\bin\vite.js"
$tscJs = Join-Path $repoRoot "frontend\node_modules\typescript\bin\tsc"

Require-Path $javaExe "Bundled Java exists: runtime\jdk\bin\java.exe"
Require-Path $javacExe "Bundled javac exists: runtime\jdk\bin\javac.exe"
Require-Path $nodeExe "Bundled Node.js exists: runtime\node\node.exe"
Require-Path $viteJs "Frontend Vite dependency exists: frontend\node_modules"
Require-Path $tscJs "Frontend TypeScript dependency exists: frontend\node_modules"
Require-Path (Join-Path $repoRoot "lib\gson-2.10.1.jar") "Backend Gson jar exists"
Require-Path (Join-Path $repoRoot "lib\antlr-4.13.2-complete.jar") "Backend ANTLR jar exists"

Write-Host ""
Write-Host "Java version:" -ForegroundColor Cyan
& $javaExe -version
Write-Host ""
Write-Host "Node version:" -ForegroundColor Cyan
& $nodeExe --version

Write-Host ""
Write-Host "Checking frontend build..." -ForegroundColor Cyan
Push-Location (Join-Path $repoRoot "frontend")
try {
  & $nodeExe "node_modules\typescript\bin\tsc" -b
  if ($LASTEXITCODE -ne 0) { Fail "TypeScript build failed" }
  & $nodeExe "node_modules\vite\bin\vite.js" build
  if ($LASTEXITCODE -ne 0) { Fail "Vite build failed" }
  Ok "Frontend build passed"
} finally {
  Pop-Location
}

Write-Host ""
Write-Host "Checking backend API..." -ForegroundColor Cyan
$verifyPort = Find-FreePort $ApiPort

$apiOut = Join-Path $repoRoot "artifacts\frontend-logs\verify-api.log"
$apiErr = Join-Path $repoRoot "artifacts\frontend-logs\verify-api.err.log"
New-Item -ItemType Directory -Force -Path (Split-Path $apiOut -Parent) | Out-Null
Remove-Item -LiteralPath $apiOut, $apiErr -Force -ErrorAction SilentlyContinue

$apiClasspath = @(
  (Join-Path $repoRoot "build-classes"),
  (Join-Path $repoRoot "generated-src"),
  (Join-Path $repoRoot "lib\gson-2.10.1.jar"),
  (Join-Path $repoRoot "lib\antlr-4.13.2-complete.jar")
) -join ";"

& $javacExe -encoding UTF-8 -sourcepath (Join-Path $repoRoot "source") -cp $apiClasspath -d (Join-Path $repoRoot "build-classes") (Join-Path $repoRoot "source\server\FrontendApiServer.java")
if ($LASTEXITCODE -ne 0) { Fail "Backend API compile failed" }

$apiProcess = Start-Process -FilePath $javaExe `
  -ArgumentList @("-Dfile.encoding=UTF-8", "-cp", $apiClasspath, "server.FrontendApiServer", "$verifyPort") `
  -WorkingDirectory $repoRoot `
  -RedirectStandardOutput $apiOut `
  -RedirectStandardError $apiErr `
  -WindowStyle Hidden `
  -PassThru

try {
  $ready = $false
  for ($i = 0; $i -lt 40; $i++) {
    try {
      $health = Invoke-RestMethod -Uri "http://127.0.0.1:$verifyPort/api/health" -TimeoutSec 2
      if ($health.ok) {
        $ready = $true
        break
      }
    } catch {
      Start-Sleep -Milliseconds 500
    }
  }
  if (-not $ready) {
    $errTail = if (Test-Path -LiteralPath $apiErr) { Get-Content -LiteralPath $apiErr -Tail 80 } else { @() }
    $outTail = if (Test-Path -LiteralPath $apiOut) { Get-Content -LiteralPath $apiOut -Tail 80 } else { @() }
    if ($errTail) { Write-Host ($errTail -join [Environment]::NewLine) -ForegroundColor Red }
    if ($outTail) { Write-Host ($outTail -join [Environment]::NewLine) -ForegroundColor Yellow }
    Fail "Backend API did not become ready on port $verifyPort"
  }
  Ok "Backend API health check passed"
} finally {
  if ($apiProcess -and -not $apiProcess.HasExited) {
    Stop-Process -Id $apiProcess.Id -Force
  }
}

Write-Host ""
Ok "Competition package verification passed. Zip the whole project folder."
