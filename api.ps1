param(
  [int]$Port = 8080,
  [switch]$NoCompile
)

$ErrorActionPreference = "Stop"
$repoRoot = $PSScriptRoot
Set-Location $repoRoot

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
  throw "Java runtime was not found. Install JDK 17+ or copy a portable JDK to runtime\jdk."
}

$outDir = Join-Path $repoRoot "build-classes"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

$serverClass = Join-Path $outDir "server\FrontendApiServer.class"
$classpath = @(
  (Join-Path $repoRoot "build-classes"),
  (Join-Path $repoRoot "generated-src"),
  (Join-Path $repoRoot "lib\gson-2.10.1.jar"),
  (Join-Path $repoRoot "lib\antlr-4.13.2-complete.jar")
) -join ";"

if (-not $NoCompile) {
  if ($javacExe) {
    & $javacExe -encoding UTF-8 -sourcepath (Join-Path $repoRoot "source") -cp $classpath -d $outDir (Join-Path $repoRoot "source\server\FrontendApiServer.java")
    if ($LASTEXITCODE -ne 0) {
      throw "javac.exe failed while compiling the API server."
    }
  } elseif (-not (Test-Path -LiteralPath $serverClass)) {
    throw "javac.exe was not found and compiled classes are missing. Install JDK 17+ or copy a portable JDK to runtime\jdk."
  } else {
    Write-Host "[api] javac.exe not found; using existing compiled classes in build-classes\."
  }
}

if (-not (Test-PortFree -Port $Port)) {
  $requestedPort = $Port
  $Port = Find-FreePort -PreferredPort ($Port + 1)
  Write-Host "[api] Port $requestedPort is busy; using free port $Port."
}

& $javaExe "-Dfile.encoding=UTF-8" -cp $classpath server.FrontendApiServer $Port
