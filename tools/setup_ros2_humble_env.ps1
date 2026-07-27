param(
  [string]$RosRoot = "D:\ros2-humble-20260220-windows-release-amd64\ros2-windows",
  [string]$PixiEnv = "C:\pixi_ws\.pixi\envs\default"
)

$ErrorActionPreference = "Stop"
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force

if (Test-Path -LiteralPath (Join-Path $PixiEnv "python.exe")) {
  $env:Path = @(
    $PixiEnv,
    (Join-Path $PixiEnv "Scripts"),
    (Join-Path $PixiEnv "Library\bin"),
    (Join-Path $PixiEnv "Library\usr\bin"),
    $env:Path
  ) -join ";"
  $env:PYTHONHOME = ""
}

if (-not (Test-Path -LiteralPath (Join-Path $RosRoot "local_setup.ps1"))) {
  throw "ROS 2 local_setup.ps1 not found: $RosRoot"
}

. (Join-Path $RosRoot "local_setup.ps1")
$repoRoot = Split-Path -Parent $PSScriptRoot
$projectRosInstall = Join-Path $repoRoot "ros2_ws\install\local_setup.ps1"
if (Test-Path -LiteralPath $projectRosInstall) {
  . $projectRosInstall
  Write-Host "[ROS2-ENV] Project ROS 2 interfaces: $projectRosInstall"
}
$asciiRosInstall = "C:\ros2_urban_runtime_ws\install\local_setup.ps1"
if (Test-Path -LiteralPath $asciiRosInstall) {
  . $asciiRosInstall
  Write-Host "[ROS2-ENV] Project ROS 2 interfaces: $asciiRosInstall"
}
Write-Host "[ROS2-ENV] ROS 2 root: $RosRoot"
Write-Host "[ROS2-ENV] Python: $((Get-Command python -ErrorAction SilentlyContinue | Select-Object -First 1).Source)"
python --version
Write-Host "[ROS2-ENV] ros2: $((Get-Command ros2 -ErrorAction SilentlyContinue | Select-Object -First 1).Source)"
