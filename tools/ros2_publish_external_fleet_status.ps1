param(
  [int]$WaitSubscribers = 1
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

. (Join-Path $repoRoot "tools\setup_ros2_humble_env.ps1")

function Publish-Status {
  param(
    [string]$Source,
    [string]$LogicalAgent,
    [string]$Protocol,
    [string]$Role,
    [string]$Route,
    [double]$X,
    [double]$Y,
    [double]$Z,
    [double]$Speed,
    [double]$Battery
  )

  $data = "topic=runtime/agent/status|source=$Source|logicalAgent=$LogicalAgent|protocol=$Protocol|role=$Role|route=$Route|x=$X|y=$Y|z=$Z|speed=$Speed|battery=$Battery|rateHz=2.0|missionStatus=EXECUTING"
  Write-Host "[ROS2-FLEET] publishing $LogicalAgent via $Protocol"
  & ros2 topic pub --once --wait-matching-subscriptions $WaitSubscribers --keep-alive 3.0 /agent/status std_msgs/msg/String "{data: `"$data`"}"
  if ($LASTEXITCODE -ne 0) { throw "External ROS2 fleet status publish failed for $LogicalAgent." }
}

Publish-Status -Source "UAV-ROS2-EXT-001" -LogicalAgent "UAV-CQ-007" -Protocol "DJI-STYLE" -Role "UAV" -Route "route_C_aerial_scout" -X 13.0 -Y 9.0 -Z 80.0 -Speed 16.0 -Battery 77.0
Publish-Status -Source "CAR-ROS2-EXT-002" -LogicalAgent "CAR-CQ-001" -Protocol "ROS2-DDS" -Role "EGO_CAR" -Route "route_A_main_road" -X 10.0 -Y 6.0 -Z 0.0 -Speed 8.5 -Battery 84.0
Publish-Status -Source "PEER-ROS2-EXT-003" -LogicalAgent "PEER-CAR-031" -Protocol "GENERIC-PRIVATE-JSON" -Role "PEER_CAR" -Route "route_A_main_road" -X 11.0 -Y 7.0 -Z 0.0 -Speed 7.0 -Battery 91.0
