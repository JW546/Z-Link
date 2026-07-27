param(
  [string]$AgentId = "UAV-REAL-001",
  [string]$Role = "UAV",
  [string]$Route = "route_C_aerial_scout",
  [int]$WaitSubscribers = 1
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

. (Join-Path $repoRoot "tools\setup_ros2_humble_env.ps1")

$data = "topic=runtime/agent/status|source=$AgentId|agent=$AgentId|role=$Role|route=$Route"
Write-Host "[ROS2-EXTERNAL] publishing external device status: $data"
& ros2 topic pub --once --wait-matching-subscriptions $WaitSubscribers --keep-alive 3.0 /agent/status std_msgs/msg/String "{data: `"$data`"}"
if ($LASTEXITCODE -ne 0) { throw "External ROS2 status publish failed." }
