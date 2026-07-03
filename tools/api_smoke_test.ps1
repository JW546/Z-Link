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
$TotalChecks = 35
$StepIndex = 0

function Write-Step {
  param([string]$Label)
  $script:StepIndex += 1
  Write-Host "[$script:StepIndex/$TotalChecks] $Label"
}

Write-Step "GET /health"
$health = Invoke-Json GET "/health"
if (-not $health.ok) { throw "health failed" }

Write-Step "GET /agent/health"
$agentHealth = Invoke-Json GET "/agent/health"
if (-not $agentHealth.ok -or -not $agentHealth.modules -or $agentHealth.modules.Count -lt 1) { throw "agent health missing modules" }

Write-Step "POST /auth/login"
$login = Invoke-Json POST "/auth/login" @{ username = "demo"; password = "demo123" }
if (-not $login.ok -or -not $login.token -or -not $login.user) { throw "login response missing session fields" }

Write-Step "POST /auth/logout"
$logout = Invoke-Json POST "/auth/logout" @{}
if (-not $logout.ok) { throw "logout failed" }

Write-Step "GET /capabilities"
$capabilities = Invoke-Json GET "/capabilities"
if (-not $capabilities.ok -or -not $capabilities.groups -or $capabilities.groups.Count -lt 1) { throw "capabilities missing groups" }

Write-Step "POST /capability/action"
$capabilityAction = Invoke-Json POST "/capability/action" @{ capabilityId = "dds.topicCatalog"; action = "list"; input = "smoke test" }
if (-not $capabilityAction.ok -or -not $capabilityAction.data -or -not $capabilityAction.data.publishTopics) { throw "capability action missing topic catalog" }

Write-Step "POST /nlu/parse"
$nlu = Invoke-Json POST "/nlu/parse" @{ text = $inspectText }
if (-not $nlu.intentSummary -or -not $nlu.entities -or $nlu.entities.Count -lt 1) { throw "nlu parse missing intent fields" }

Write-Step "POST /agent/plan"
$plan = Invoke-Json POST "/agent/plan" @{ text = $missionText }
if (-not $plan.missionId -or -not $plan.structuredDsl -or -not $plan.platformPlan) { throw "plan response missing fields" }
if (-not $plan.platformPlan.riskAssessment) { throw "plan riskAssessment missing" }

Write-Step "GET /agent/devices"
$devices = Invoke-Json GET "/agent/devices"
if (-not $devices.devices -or $devices.devices.Count -lt 1) { throw "devices empty" }

Write-Step "GET /devices"
$legacyDevices = Invoke-Json GET "/devices"
if (-not $legacyDevices -or @($legacyDevices).Count -lt 1) { throw "legacy devices empty" }

Write-Step "GET /agent/topics"
$topics = Invoke-Json GET "/agent/topics"
if (-not $topics.topics -or $topics.topics.Count -lt 1) { throw "topics empty" }

Write-Step "POST /topic/action"
$topicAction = Invoke-Json POST "/topic/action" @{ topicName = $topics.topics[0].name; action = "publish"; payload = "smoke-test-payload" }
if (-not $topicAction.ok) { throw "topic action failed" }

Write-Step "POST /agent/simulate"
$simulation = Invoke-Json POST "/agent/simulate" @{ text = $inspectText }
if (-not $simulation.executionTimeline -or $simulation.executionTimeline.Count -lt 1) { throw "simulation timeline missing" }

Write-Step "GET /simulation/scenarios"
$trafficScenarios = Invoke-Json GET "/simulation/scenarios"
if (-not $trafficScenarios.scenarios -or $trafficScenarios.scenarios.Count -lt 1) { throw "traffic simulation scenarios missing" }

Write-Step "POST /simulation/start"
$trafficStart = Invoke-Json POST "/simulation/start" @{ scenarioId = "hospital-emergency" }
if (-not $trafficStart.running -or -not $trafficStart.world) { throw "traffic simulation did not start" }

Write-Step "POST /simulation/tick"
$trafficTick = Invoke-Json POST "/simulation/tick" @{}
if (-not $trafficTick.ddsMessages -or $trafficTick.ddsMessages.Count -lt 6) { throw "traffic simulation tick missing DDS messages" }
if (-not $trafficTick.finalDecision -or [string]::IsNullOrWhiteSpace([string]$trafficTick.finalDecision.route)) { throw "traffic simulation final route missing" }

Write-Step "GET /simulation/state"
$trafficState = Invoke-Json GET "/simulation/state"
if (-not $trafficState.world -or -not $trafficState.world.devices -or $trafficState.world.devices.Count -lt 4) { throw "traffic simulation state missing devices" }

Write-Step "GET /simulation/dds-events"
$trafficEvents = Invoke-Json GET "/simulation/dds-events"
if (-not $trafficEvents.events -or $trafficEvents.events.Count -lt 6) { throw "traffic simulation DDS events missing" }

Write-Step "POST /agent/execute"
$execution = Invoke-Json POST "/agent/execute" @{ missionId = $plan.missionId }
if (-not $execution.runId -or -not $execution.timelineSummary) { throw "execute timeline summary missing" }

Write-Step "POST /mission/action"
$missionAction = Invoke-Json POST "/mission/action" @{ missionId = $plan.missionId; action = "pause" }
if (-not $missionAction.ok) { throw "mission action failed" }

Write-Step "GET /missions"
$missions = Invoke-Json GET "/missions"
$pausedMission = @($missions) | Where-Object { $_.id -eq $plan.missionId -and $_.status -eq "paused" } | Select-Object -First 1
if (-not $pausedMission) { throw "mission action did not update mission status" }

Write-Step "GET /agent/logs"
$logs = Invoke-Json GET "/agent/logs?missionId=$($plan.missionId)&runId=$($execution.runId)"
if ($null -eq $logs.logs -or $null -eq $logs.executionLogs) { throw "logs response missing arrays" }

Write-Step "GET /agent/scenarios"
$scenarios = Invoke-Json GET "/agent/scenarios"
if (-not $scenarios.scenarios -or $scenarios.scenarios.Count -lt 6) { throw "scenarios missing" }

Write-Step "GET /agent/scenario"
$scenarioId = $scenarios.scenarios[0].scenarioId
$scenario = Invoke-Json GET "/agent/scenario?id=$scenarioId"
if (-not $scenario.scenario -or $scenario.scenario.scenarioId -ne $scenarioId) { throw "scenario detail mismatch" }

Write-Step "POST /agent/scenario/plan"
$scenarioPlan = Invoke-Json POST "/agent/scenario/plan" @{ scenarioId = $scenarioId }
if (-not $scenarioPlan.ok -or -not $scenarioPlan.missionId -or -not $scenarioPlan.structuredDsl -or -not $scenarioPlan.platformPlan) { throw "scenario plan missing fields" }
if ($scenarioPlan.scenario.scenarioId -ne $scenarioId) { throw "scenario plan id mismatch" }

Write-Step "GET /agent/faults"
$faults = Invoke-Json GET "/agent/faults"
if ($null -eq $faults.faults) { throw "faults response missing" }

Write-Step "POST /agent/fault/inject"
$inject = Invoke-Json POST "/agent/fault/inject" @{ faultType = "LINK_DEGRADED"; target = "UAV-CQ-007" }
if (-not $inject.ok -or -not $inject.fault) { throw "fault injection failed" }

Write-Step "POST /agent/fault/clear"
$clear = Invoke-Json POST "/agent/fault/clear" @{}
if (-not $clear.ok) { throw "fault clear failed" }

Write-Step "POST /device/action unbind"
$unbind = Invoke-Json POST "/device/action" @{ deviceId = "UAV-CQ-007"; action = "unbind" }
if (-not $unbind.ok -or $unbind.device.status -ne "OFFLINE") { throw "device unbind failed" }

Write-Step "POST /device/action reconnect"
$reconnect = Invoke-Json POST "/device/action" @{ deviceId = "UAV-CQ-007"; action = "reconnect" }
if (-not $reconnect.ok -or $reconnect.device.status -ne "ONLINE") { throw "device reconnect failed" }

Write-Step "GET /agent/topic-topology"
$topology = Invoke-Json GET "/agent/topic-topology"
if (-not $topology.topology -or -not $topology.topology.nodes) { throw "topic topology missing" }

Write-Step "GET /agent/history"
$history = Invoke-Json GET "/agent/history"
if ($null -eq $history.history) { throw "history response missing" }

Write-Step "GET /agent/dashboard"
$dashboard = Invoke-Json GET "/agent/dashboard"
if (-not $dashboard.dashboard) { throw "dashboard missing" }

Write-Step "GET /agent/report"
$report = Invoke-Json GET "/agent/report?missionId=$($plan.missionId)"
if (-not $report.markdown) { throw "report missing" }

Write-Step "POST /agent/history/clear"
$historyClear = Invoke-Json POST "/agent/history/clear" @{}
if (-not $historyClear.ok) { throw "history clear failed" }

Write-Host "Smoke test passed. missionId=$($plan.missionId), runId=$($execution.runId)"
