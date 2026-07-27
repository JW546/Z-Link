param(
  [int]$IntervalMs = 500,
  [int]$Ticks = 4,
  [int]$MaxCommands = 12,
  [ValidateSet("string", "custom")]
  [string]$MessageMode = "string"
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

$javaExe = Join-Path $repoRoot "runtime\jdk\bin\java.exe"
$javacExe = Join-Path $repoRoot "runtime\jdk\bin\javac.exe"
if (-not (Test-Path -LiteralPath $javaExe)) { $javaExe = "java.exe" }
if (-not (Test-Path -LiteralPath $javacExe)) { $javacExe = "javac.exe" }

$outDir = Join-Path $repoRoot "build-classes"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$classpath = @(
  (Join-Path $repoRoot "build-classes"),
  (Join-Path $repoRoot "generated-src"),
  (Join-Path $repoRoot "lib\gson-2.10.1.jar"),
  (Join-Path $repoRoot "lib\antlr-4.13.2-complete.jar")
) -join ";"

& $javacExe -encoding UTF-8 -sourcepath (Join-Path $repoRoot "source") -cp $classpath -d $outDir (Join-Path $repoRoot "source\collaboration\ros2\Ros2BridgePlanMain.java")
if ($LASTEXITCODE -ne 0) { throw "ROS2 bridge plan compile failed." }

if ($MessageMode -eq "custom") {
  $env:ROS2_MESSAGE_MODE = "custom"
} else {
  $env:ROS2_MESSAGE_MODE = "string"
}

& $javaExe "-Dfile.encoding=UTF-8" -cp $classpath collaboration.ros2.Ros2BridgePlanMain $IntervalMs $Ticks $MaxCommands
if ($LASTEXITCODE -ne 0) { throw "ROS2 bridge plan failed." }
