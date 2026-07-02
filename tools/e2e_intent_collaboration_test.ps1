param(
  [string]$BaseUrl = "http://localhost:8080/api",
  [string]$MissionText = ""
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($MissionText)) {
  $MissionText = -join ([char[]]@(
    0x5C3D, 0x5FEB, 0x53BB, 0x533B, 0x9662, 0xFF0C,
    0x4F18, 0x5148, 0x5B89, 0x5168, 0xFF0C,
    0x907F, 0x5F00, 0x62E5, 0x5835
  ))
}

function Invoke-Json {
  param(
    [string]$Method,
    [string]$Path,
    [object]$Body = $null
  )

  $uri = "$BaseUrl$Path"
  if ($null -eq $Body) {
    return Invoke-RestMethod -Method $Method -Uri $uri
  }
  return Invoke-RestMethod -Method $Method -Uri $uri -ContentType "application/json; charset=utf-8" -Body ($Body | ConvertTo-Json -Depth 30)
}

function Assert-True {
  param(
    [bool]$Condition,
    [string]$Message
  )
  if (-not $Condition) {
    throw "[FAIL] $Message"
  }
  Write-Host "[PASS] $Message"
}

function Has-Text {
  param(
    [object]$Value
  )
  return ($null -ne $Value) -and (-not [string]::IsNullOrWhiteSpace([string]$Value))
}

Write-Host "[E2E] Input: $MissionText"

$parse = Invoke-Json POST "/nlu/parse" @{ text = $MissionText }
Assert-True (Has-Text $parse.intentSummary) "parsed intent exists"

$plan = Invoke-Json POST "/agent/plan" @{ text = $MissionText }
Assert-True (Has-Text $plan.missionId) "missionId exists"
Assert-True ($null -ne $plan.structuredDsl) "DSL task contract exists"
Assert-True (($plan.subscribeTopics.Count + $plan.publishTopics.Count) -gt 0 -or $plan.platformPlan.idlBindings.Count -gt 0) "at least one DDS topic mapping exists"

$advice = @($plan.collaboration.deviceAdvice)
Assert-True (@($advice | Where-Object { "$($_.nodeId)" -match "CAR" }).Count -gt 0) "at least one unmanned vehicle advice exists"
Assert-True (@($advice | Where-Object { "$($_.nodeId)" -match "UAV" }).Count -gt 0) "at least one UAV advice exists"
Assert-True (@($advice | Where-Object { "$($_.nodeId)" -match "RSU" }).Count -gt 0) "at least one roadside node advice exists"

Assert-True (Has-Text $plan.collaboration.finalDecision.route) "final route exists"
Assert-True (Has-Text $plan.collaboration.finalDecision.reason) "decision explanation exists"
Assert-True ($null -ne $plan.dashboardTrace) "dashboard trace exists"
Assert-True (@($plan.dashboardTrace.ddsTimeline).Count -gt 0) "dashboard DDS timeline exists"
Assert-True ($null -ne $plan.dashboardTrace.conflictDetection) "dashboard conflict detection exists"
Assert-True ($null -ne $plan.dashboardTrace.conflictResolution) "dashboard conflict resolution exists"
Assert-True (@($plan.dashboardTrace.plannerComparison.algorithms).Count -ge 4) "dashboard planner comparison exists"
Assert-True (@($plan.dashboardTrace.policyComparison.policyResults).Count -ge 3) "dashboard policy comparison exists"
Assert-True ($null -ne $plan.dashboardTrace.policyOptimization.improvement) "dashboard policy optimization comparison exists"
Assert-True (@($plan.dashboardTrace.experimentMetricCharts).Count -gt 0) "dashboard experiment metrics exist"
Assert-True (@($plan.dashboardTrace.replanningTimeline).Count -gt 0) "dashboard replanning timeline exists"

Write-Host "[E2E] missionId=$($plan.missionId)"
Write-Host "[E2E] finalRoute=$($plan.collaboration.finalDecision.route)"
Write-Host "[E2E] explanation=$($plan.collaboration.finalDecision.reason)"
