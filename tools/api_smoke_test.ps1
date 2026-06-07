param(
  [string]$BaseUrl = "http://localhost:8080/api"
)

$ErrorActionPreference = "Stop"

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
  return Invoke-RestMethod -Method $Method -Uri $uri -ContentType "application/json; charset=utf-8" -Body ($Body | ConvertTo-Json -Depth 20)
}

$missionText = "Emergency dispatch an unmanned vehicle to hospital, coordinate UAV, RSU and wide-area link."
$inspectText = "Dispatch UAV to inspect an accident road segment and sync traffic risk."

Write-Host "[1/15] GET /health"
$health = Invoke-Json GET "/health"
if (-not $health.ok) { throw "health failed" }

Write-Host "[2/15] POST /agent/plan"
$plan = Invoke-Json POST "/agent/plan" @{ text = $missionText }
if (-not $plan.missionId -or -not $plan.structuredDsl -or -not $plan.platformPlan) { throw "plan response missing fields" }
if (-not $plan.platformPlan.riskAssessment) { throw "plan riskAssessment missing" }

Write-Host "[3/15] GET /agent/devices"
$devices = Invoke-Json GET "/agent/devices"
if (-not $devices.devices -or $devices.devices.Count -lt 1) { throw "devices empty" }

Write-Host "[4/15] GET /agent/topics"
$topics = Invoke-Json GET "/agent/topics"
if (-not $topics.topics -or $topics.topics.Count -lt 1) { throw "topics empty" }

Write-Host "[5/15] POST /agent/simulate"
$simulation = Invoke-Json POST "/agent/simulate" @{ text = $inspectText }
if (-not $simulation.executionTimeline -or $simulation.executionTimeline.Count -lt 1) { throw "simulation timeline missing" }

Write-Host "[6/15] POST /agent/execute"
$execution = Invoke-Json POST "/agent/execute" @{ missionId = $plan.missionId }
if (-not $execution.runId -or -not $execution.timelineSummary) { throw "execute timeline summary missing" }

Write-Host "[7/15] GET /agent/logs"
$logs = Invoke-Json GET "/agent/logs?missionId=$($plan.missionId)&runId=$($execution.runId)"
if ($null -eq $logs.logs -or $null -eq $logs.executionLogs) { throw "logs response missing arrays" }

Write-Host "[8/15] GET /agent/scenarios"
$scenarios = Invoke-Json GET "/agent/scenarios"
if (-not $scenarios.scenarios -or $scenarios.scenarios.Count -lt 6) { throw "scenarios missing" }

Write-Host "[9/15] GET /agent/faults"
$faults = Invoke-Json GET "/agent/faults"
if ($null -eq $faults.faults) { throw "faults response missing" }

Write-Host "[10/15] POST /agent/fault/inject"
$inject = Invoke-Json POST "/agent/fault/inject" @{ faultType = "LINK_DEGRADED"; target = "UAV-CQ-007" }
if (-not $inject.ok -or -not $inject.fault) { throw "fault injection failed" }

Write-Host "[11/15] POST /agent/fault/clear"
$clear = Invoke-Json POST "/agent/fault/clear" @{}
if (-not $clear.ok) { throw "fault clear failed" }

Write-Host "[12/15] GET /agent/topic-topology"
$topology = Invoke-Json GET "/agent/topic-topology"
if (-not $topology.topology -or -not $topology.topology.nodes) { throw "topic topology missing" }

Write-Host "[13/15] GET /agent/history"
$history = Invoke-Json GET "/agent/history"
if ($null -eq $history.history) { throw "history response missing" }

Write-Host "[14/15] GET /agent/dashboard"
$dashboard = Invoke-Json GET "/agent/dashboard"
if (-not $dashboard.dashboard) { throw "dashboard missing" }

Write-Host "[15/15] GET /agent/report"
$report = Invoke-Json GET "/agent/report?missionId=$($plan.missionId)"
if (-not $report.markdown) { throw "report missing" }

Write-Host "Smoke test passed. missionId=$($plan.missionId), runId=$($execution.runId)"
