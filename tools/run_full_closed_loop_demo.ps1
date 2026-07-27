param(
  [int]$TimeoutMs = 5000
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
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

$portableJdkBin = Join-Path $repoRoot "runtime\jdk\bin"
$javaExe = Find-Executable "java.exe" @(
  (Join-Path $portableJdkBin "java.exe"),
  (Join-Path $repoRoot "jdk\bin\java.exe")
)
$javacExe = Find-Executable "javac.exe" @(
  (Join-Path $portableJdkBin "javac.exe"),
  (Join-Path $repoRoot "jdk\bin\javac.exe")
)
if (-not $javaExe) { throw "java.exe not found." }
if (-not $javacExe) { throw "javac.exe not found." }

$outDir = Join-Path $repoRoot "build-classes"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$classpath = @(
  (Join-Path $repoRoot "build-classes"),
  (Join-Path $repoRoot "lib\gson-2.10.1.jar")
) -join ";"

& $javacExe -encoding UTF-8 -sourcepath (Join-Path $repoRoot "source") -cp $classpath -d $outDir (Join-Path $repoRoot "source\collaboration\runtime\FullClosedLoopDemoMain.java")
if ($LASTEXITCODE -ne 0) { throw "Full closed-loop demo compile failed." }

& $javaExe "-Dfile.encoding=UTF-8" -cp $classpath collaboration.runtime.FullClosedLoopDemoMain $TimeoutMs
if ($LASTEXITCODE -ne 0) { throw "Full closed-loop demo failed." }
