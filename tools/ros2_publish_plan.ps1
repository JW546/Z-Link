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

. (Join-Path $repoRoot "tools\setup_ros2_humble_env.ps1")

$pixiEnv = "C:\pixi_ws\.pixi\envs\default"
if (Test-Path -LiteralPath (Join-Path $pixiEnv "python.exe")) {
  $env:Path = @(
    $pixiEnv,
    (Join-Path $pixiEnv "Scripts"),
    (Join-Path $pixiEnv "Library\bin"),
    (Join-Path $pixiEnv "Library\usr\bin"),
    $env:Path
  ) -join ";"
  $env:PYTHONHOME = ""
}

$ros2 = Get-Command ros2 -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $ros2) {
  throw "ros2 CLI not found. Install ROS 2 and source/setup its environment before running this script."
}

$json = powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $repoRoot "tools\ros2_bridge_plan.ps1") -IntervalMs $IntervalMs -Ticks $Ticks -MaxCommands $MaxCommands -MessageMode $MessageMode
$plan = $json | ConvertFrom-Json
if (-not $plan.publishCommands) {
  throw "No ROS2 publish commands generated."
}

$count = 0
foreach ($item in $plan.publishCommands) {
  if ($count -ge $MaxCommands) { break }
  $parts = @($item.command)
  Write-Host "[ROS2-PUB] $($item.semanticTopic) -> $($item.rosTopic)"
  $exe = [string]$parts[0]
  $args = @()
  if ($parts.Count -gt 1) {
    $args = @($parts[1..($parts.Count - 1)] | ForEach-Object { [string]$_ })
  }
  & $exe @args
  if ($LASTEXITCODE -ne 0) { throw "ros2 publish failed for $($item.rosTopic)" }
  $count += 1
}
