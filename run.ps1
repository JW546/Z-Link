$ErrorActionPreference = "Stop"
$repoRoot = $PSScriptRoot
$frontendRoot = Join-Path $repoRoot "frontend"
$apiPort = if ([string]::IsNullOrWhiteSpace($env:API_PORT)) { 8080 } else { [int]$env:API_PORT }
$frontendPort = if ([string]::IsNullOrWhiteSpace($env:FRONTEND_PORT)) { 5173 } else { [int]$env:FRONTEND_PORT }
$logDir = Join-Path $repoRoot "artifacts\frontend-logs"
New-Item -ItemType Directory -Force -Path $logDir | Out-Null

function Find-Executable {
  param(
    [string]$Name,
    [string[]]$Candidates
  )

  foreach ($candidate in $Candidates) {
    if (-not [string]::IsNullOrWhiteSpace($candidate) -and (Test-Path -LiteralPath $candidate)) {
      return (Resolve-Path -LiteralPath $candidate).Path
    }
  }

  $command = Get-Command $Name -ErrorAction SilentlyContinue | Select-Object -First 1
  if ($command) {
    return $command.Source
  }

  return $null
}

function Test-ApiHealth {
  param([int]$Port)
  try {
    $response = Invoke-RestMethod -Uri "http://127.0.0.1:$Port/api/health" -Method Get -TimeoutSec 2
    return [bool]$response.ok
  } catch {
    return $false
  }
}

function Get-LastLogText {
  param(
    [string[]]$Paths,
    [int]$Tail = 80
  )

  $chunks = @()
  foreach ($path in $Paths) {
    if (Test-Path -LiteralPath $path) {
      $content = Get-Content -LiteralPath $path -Tail $Tail -ErrorAction SilentlyContinue
      if ($content) {
        $chunks += "---- $path ----"
        $chunks += $content
      }
    }
  }
  return ($chunks -join [Environment]::NewLine)
}

function Test-PortFree {
  param([int]$Port)
  $listener = $null
  try {
    $listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, $Port)
    $listener.Start()
    return $true
  } catch {
    return $false
  } finally {
    if ($listener) {
      $listener.Stop()
    }
  }
}

function Find-FreePort {
  param(
    [int]$PreferredPort,
    [int]$MaxAttempts = 80
  )

  for ($offset = 0; $offset -lt $MaxAttempts; $offset++) {
    $candidate = $PreferredPort + $offset
    if ($candidate -gt 65535) {
      break
    }
    if (Test-PortFree -Port $candidate) {
      return $candidate
    }
  }

  $listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, 0)
  try {
    $listener.Start()
    return ([System.Net.IPEndPoint]$listener.LocalEndpoint).Port
  } finally {
    $listener.Stop()
  }
}

function Start-ApiIfNeeded {
  if (Test-ApiHealth -Port $apiPort) {
    $requestedPort = $apiPort
    $script:apiPort = Find-FreePort -PreferredPort ($apiPort + 1)
    Write-Host "[start_frontend] API port $requestedPort already has a server; starting a fresh API on port $apiPort."
  } elseif (-not (Test-PortFree -Port $apiPort)) {
    $requestedPort = $apiPort
    $script:apiPort = Find-FreePort -PreferredPort ($apiPort + 1)
    Write-Host "[start_frontend] API port $requestedPort is busy; using free port $apiPort."
  }

  $apiOutLog = Join-Path $logDir "api-server.log"
  $apiErrLog = Join-Path $logDir "api-server.err.log"
  Remove-Item -LiteralPath $apiOutLog, $apiErrLog -Force -ErrorAction SilentlyContinue

  $portableJdkBin = Join-Path $repoRoot "runtime\jdk\bin"
  $javaCandidates = @(
    (Join-Path $portableJdkBin "java.exe"),
    (Join-Path $repoRoot "jdk\bin\java.exe")
  )
  $javacCandidates = @(
    (Join-Path $portableJdkBin "javac.exe"),
    (Join-Path $repoRoot "jdk\bin\javac.exe")
  )
  if (-not [string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
    $javaHomeBin = Join-Path $env:JAVA_HOME "bin"
    $javaCandidates += (Join-Path $javaHomeBin "java.exe")
    $javacCandidates += (Join-Path $javaHomeBin "javac.exe")
  }
  $javaExe = Find-Executable "java.exe" $javaCandidates
  $javacExe = Find-Executable "javac.exe" $javacCandidates

  if (-not $javaExe) {
    throw "Java runtime was not found. Install JDK 17+ or keep bundled runtime\jdk."
  }

  $outDir = Join-Path $repoRoot "build-classes"
  New-Item -ItemType Directory -Force -Path $outDir | Out-Null
  $classpath = @(
    (Join-Path $repoRoot "build-classes"),
    (Join-Path $repoRoot "generated-src"),
    (Join-Path $repoRoot "lib\gson-2.10.1.jar"),
    (Join-Path $repoRoot "lib\antlr-4.13.2-complete.jar")
  ) -join ";"

  if ($javacExe) {
    Write-Host "[start_frontend] Compiling Java API server..."
    & $javacExe -encoding UTF-8 -sourcepath (Join-Path $repoRoot "source") -cp $classpath -d $outDir (Join-Path $repoRoot "source\server\FrontendApiServer.java")
    if ($LASTEXITCODE -ne 0) {
      throw "Java API compile failed. The server was not started."
    }
  } elseif (-not (Test-Path -LiteralPath (Join-Path $outDir "server\FrontendApiServer.class"))) {
    throw "javac.exe was not found and compiled API classes are missing. Keep bundled runtime\jdk or install JDK 17+."
  } else {
    Write-Host "[start_frontend] javac.exe not found; using existing compiled classes in build-classes\."
  }

  Write-Host "[start_frontend] Starting Java API server on http://127.0.0.1:$apiPort/api ..."
  $apiArgs = @(
    "-Dfile.encoding=UTF-8",
    "-cp",
    $classpath,
    "server.FrontendApiServer",
    "$apiPort"
  )
  $apiProcess = Start-Process -FilePath $javaExe `
    -ArgumentList $apiArgs `
    -WorkingDirectory $repoRoot `
    -RedirectStandardOutput $apiOutLog `
    -RedirectStandardError $apiErrLog `
    -WindowStyle Hidden `
    -PassThru

  for ($i = 0; $i -lt 80; $i++) {
    if (Test-ApiHealth -Port $apiPort) {
      Write-Host "[start_frontend] API is ready on http://127.0.0.1:$apiPort/api"
      return
    }
    if ($apiProcess.HasExited) {
      $logs = Get-LastLogText -Paths @($apiErrLog, $apiOutLog)
      throw "Java API server exited early. $logs"
    }
    Start-Sleep -Milliseconds 500
  }

  $logs = Get-LastLogText -Paths @($apiErrLog, $apiOutLog)
  throw "Java API server did not become ready on port $apiPort. $logs"
}

$nodePath = Find-Executable "node.exe" @(
  (Join-Path $repoRoot "runtime\node\node.exe"),
  (Join-Path $repoRoot "node\node.exe"),
  (Join-Path $env:USERPROFILE ".cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe")
)

if (-not $nodePath) {
  throw "Node.js was not found. Install Node.js 18+ or copy portable Node.js to runtime\node."
}

$amapConfigPath = Join-Path $PSScriptRoot "frontend\amap.config.json"
if (Test-Path $amapConfigPath) {
  try {
    $amapConfig = Get-Content $amapConfigPath -Raw | ConvertFrom-Json
    if ([string]::IsNullOrWhiteSpace($env:VITE_AMAP_WEB_KEY)) {
      $candidateWebKey = $amapConfig.VITE_AMAP_WEB_KEY
      if ([string]::IsNullOrWhiteSpace($candidateWebKey)) {
        $candidateWebKey = $amapConfig.webKey
      }
      if (-not [string]::IsNullOrWhiteSpace($candidateWebKey)) {
        $env:VITE_AMAP_WEB_KEY = $candidateWebKey
      }
    }
    if ([string]::IsNullOrWhiteSpace($env:VITE_AMAP_SECURITY_JS_CODE)) {
      $candidateSecurityCode = $amapConfig.VITE_AMAP_SECURITY_JS_CODE
      if ([string]::IsNullOrWhiteSpace($candidateSecurityCode)) {
        $candidateSecurityCode = $amapConfig.securityJsCode
      }
      if (-not [string]::IsNullOrWhiteSpace($candidateSecurityCode)) {
        $env:VITE_AMAP_SECURITY_JS_CODE = $candidateSecurityCode
      }
    }
  } catch {
    throw "Failed to parse $amapConfigPath. Use valid JSON with webKey and optional securityJsCode."
  }
}

if ([string]::IsNullOrWhiteSpace($env:VITE_AMAP_WEB_KEY) -and -not [string]::IsNullOrWhiteSpace($env:AMAP_API_KEY)) {
  $env:VITE_AMAP_WEB_KEY = $env:AMAP_API_KEY
}

if ([string]::IsNullOrWhiteSpace($env:VITE_AMAP_SECURITY_JS_CODE) -and -not [string]::IsNullOrWhiteSpace($env:AMAP_SECURITY_JS_CODE)) {
  $env:VITE_AMAP_SECURITY_JS_CODE = $env:AMAP_SECURITY_JS_CODE
}

$env:VITE_API_BASE_URL = "/api"
Start-ApiIfNeeded
$env:API_PORT = "$apiPort"
$env:VITE_API_PROXY_TARGET = "http://127.0.0.1:$apiPort"

Set-Location $frontendRoot

if (-not (Test-Path "node_modules\vite\bin\vite.js")) {
  throw "Frontend dependencies are missing. Run npm install in the frontend directory first."
}

if (-not (Test-PortFree -Port $frontendPort)) {
  $requestedFrontendPort = $frontendPort
  $frontendPort = Find-FreePort -PreferredPort ($frontendPort + 1)
  Write-Host "[start_frontend] Frontend port $requestedFrontendPort is busy; using free port $frontendPort."
}

Write-Host "[start_frontend] Opening frontend on http://127.0.0.1:$frontendPort"
Write-Host "[start_frontend] API proxy target is $env:VITE_API_PROXY_TARGET"
& $nodePath "node_modules\vite\bin\vite.js" --host 127.0.0.1 --port $frontendPort --strictPort --open
