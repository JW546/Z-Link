param(
  [int]$TimeoutMs = 5000,
  [switch]$DryRun
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
$javaExe = Find-Executable "java.exe" @((Join-Path $portableJdkBin "java.exe"), (Join-Path $repoRoot "jdk\bin\java.exe"))
$javacExe = Find-Executable "javac.exe" @((Join-Path $portableJdkBin "javac.exe"), (Join-Path $repoRoot "jdk\bin\javac.exe"))
if (-not $javaExe) { throw "java.exe not found." }
if (-not $javacExe) { throw "javac.exe not found." }

if (-not $DryRun) {
  . (Join-Path $repoRoot "tools\setup_ros2_humble_env.ps1")
}

$outDir = Join-Path $repoRoot "build-classes"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$classpath = @(
  (Join-Path $repoRoot "build-classes"),
  (Join-Path $repoRoot "lib\gson-2.10.1.jar")
) -join ";"

& $javacExe -encoding UTF-8 -sourcepath (Join-Path $repoRoot "source") -cp $classpath -d $outDir (Join-Path $repoRoot "source\collaboration\ros2\Ros2BidirectionalClosedLoopDemoMain.java")
if ($LASTEXITCODE -ne 0) { throw "ROS2 bidirectional closed-loop demo compile failed." }

$mode = if ($DryRun) { "dry-run" } else { "real" }
& $javaExe "-Dfile.encoding=UTF-8" -cp $classpath collaboration.ros2.Ros2BidirectionalClosedLoopDemoMain $TimeoutMs $mode
if ($LASTEXITCODE -ne 0) { throw "ROS2 bidirectional closed-loop demo failed." }
