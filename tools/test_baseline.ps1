param(
  [int]$ApiPort = 18108,
  [switch]$WithFrontendBuild
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

function Test-PortFree {
  param([int]$Port)
  $listener = $null
  try {
    $listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, $Port)
    $listener.Start()
    return $true
  } catch {
    return $false
  } finally {
    if ($listener) {
      $listener.Stop()
    }
  }
}

function Find-FreePort {
  param([int]$PreferredPort)
  for ($port = $PreferredPort; $port -le ($PreferredPort + 80); $port++) {
    if (Test-PortFree -Port $port) {
      return $port
    }
  }
  throw "No free API test port found."
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
$nodeExe = Find-Executable "node.exe" @(
  (Join-Path $repoRoot "runtime\node\node.exe"),
  (Join-Path $repoRoot "node\node.exe")
)

if (-not $javaExe) { throw "java.exe not found." }
if (-not $javacExe) { throw "javac.exe not found." }

if ($WithFrontendBuild) {
  if (-not $nodeExe) { throw "node.exe not found." }
  Push-Location (Join-Path $repoRoot "frontend")
  try {
    Write-Host "[BASELINE] Frontend build: npm script equivalent"
    & $nodeExe "node_modules\typescript\bin\tsc" -b
    if ($LASTEXITCODE -ne 0) { throw "TypeScript build failed." }
    & $nodeExe "node_modules\vite\bin\vite.js" build
    if ($LASTEXITCODE -ne 0) { throw "Vite build failed." }
  } finally {
    Pop-Location
  }
}

$port = Find-FreePort -PreferredPort $ApiPort
$logDir = Join-Path $repoRoot "artifacts\test-logs"
New-Item -ItemType Directory -Force -Path $logDir | Out-Null
$apiOut = Join-Path $logDir "baseline-api.out.log"
$apiErr = Join-Path $logDir "baseline-api.err.log"
Remove-Item -LiteralPath $apiOut, $apiErr -Force -ErrorAction SilentlyContinue

$outDir = Join-Path $repoRoot "build-classes"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$classpath = @(
  (Join-Path $repoRoot "build-classes"),
  (Join-Path $repoRoot "generated-src"),
  (Join-Path $repoRoot "lib\gson-2.10.1.jar"),
  (Join-Path $repoRoot "lib\antlr-4.13.2-complete.jar")
) -join ";"

Write-Host "[BASELINE] Compile backend API"
& $javacExe -encoding UTF-8 -sourcepath (Join-Path $repoRoot "source") -cp $classpath -d $outDir (Join-Path $repoRoot "source\server\FrontendApiServer.java")
if ($LASTEXITCODE -ne 0) { throw "Backend API compile failed." }

Write-Host "[BASELINE] Compile architecture module smoke test"
& $javacExe -encoding UTF-8 -sourcepath (Join-Path $repoRoot "source") -cp $classpath -d $outDir (Join-Path $repoRoot "source\integration\ArchitectureModuleSmokeTest.java")
if ($LASTEXITCODE -ne 0) { throw "Architecture module smoke test compile failed." }

Write-Host "[BASELINE] Run architecture module smoke test"
& $javaExe "-Dfile.encoding=UTF-8" -cp $classpath integration.ArchitectureModuleSmokeTest
if ($LASTEXITCODE -ne 0) { throw "Architecture module smoke test failed." }

Write-Host "[BASELINE] Compile DDS message bus test"
& $javacExe -encoding UTF-8 -sourcepath (Join-Path $repoRoot "source") -cp $classpath -d $outDir (Join-Path $repoRoot "source\integration\DDSMessageBusTest.java")
if ($LASTEXITCODE -ne 0) { throw "DDS message bus test compile failed." }

Write-Host "[BASELINE] Run DDS message bus test"
& $javaExe "-Dfile.encoding=UTF-8" -cp $classpath integration.DDSMessageBusTest
if ($LASTEXITCODE -ne 0) { throw "DDS message bus test failed." }

Write-Host "[BASELINE] Compile virtual traffic simulation test"
& $javacExe -encoding UTF-8 -sourcepath (Join-Path $repoRoot "source") -cp $classpath -d $outDir (Join-Path $repoRoot "source\integration\VirtualTrafficSimulationServiceTest.java")
if ($LASTEXITCODE -ne 0) { throw "Virtual traffic simulation test compile failed." }

Write-Host "[BASELINE] Run virtual traffic simulation test"
& $javaExe "-Dfile.encoding=UTF-8" -cp $classpath integration.VirtualTrafficSimulationServiceTest
if ($LASTEXITCODE -ne 0) { throw "Virtual traffic simulation test failed." }

Write-Host "[BASELINE] Compile Agent runtime policy test"
& $javacExe -encoding UTF-8 -sourcepath (Join-Path $repoRoot "source") -cp $classpath -d $outDir (Join-Path $repoRoot "source\integration\AgentRuntimePolicyTest.java")
if ($LASTEXITCODE -ne 0) { throw "Agent runtime policy test compile failed." }

Write-Host "[BASELINE] Run Agent runtime policy test"
& $javaExe "-Dfile.encoding=UTF-8" -cp $classpath integration.AgentRuntimePolicyTest
if ($LASTEXITCODE -ne 0) { throw "Agent runtime policy test failed." }

Write-Host "[BASELINE] Compile Agent policy model test"
& $javacExe -encoding UTF-8 -sourcepath (Join-Path $repoRoot "source") -cp $classpath -d $outDir (Join-Path $repoRoot "source\integration\AgentPolicyModelTest.java")
if ($LASTEXITCODE -ne 0) { throw "Agent policy model test compile failed." }

Write-Host "[BASELINE] Run Agent policy model test"
& $javaExe "-Dfile.encoding=UTF-8" -cp $classpath integration.AgentPolicyModelTest
if ($LASTEXITCODE -ne 0) { throw "Agent policy model test failed." }

Write-Host "[BASELINE] Compile Agent policy variants test"
& $javacExe -encoding UTF-8 -sourcepath (Join-Path $repoRoot "source") -cp $classpath -d $outDir (Join-Path $repoRoot "source\integration\AgentPolicyVariantsTest.java")
if ($LASTEXITCODE -ne 0) { throw "Agent policy variants test compile failed." }

Write-Host "[BASELINE] Run Agent policy variants test"
& $javaExe "-Dfile.encoding=UTF-8" -cp $classpath integration.AgentPolicyVariantsTest
if ($LASTEXITCODE -ne 0) { throw "Agent policy variants test failed." }

Write-Host "[BASELINE] Compile PolicyEvaluator test"
& $javacExe -encoding UTF-8 -sourcepath (Join-Path $repoRoot "source") -cp $classpath -d $outDir (Join-Path $repoRoot "source\integration\PolicyEvaluatorTest.java")
if ($LASTEXITCODE -ne 0) { throw "PolicyEvaluator test compile failed." }

Write-Host "[BASELINE] Run PolicyEvaluator test"
& $javaExe "-Dfile.encoding=UTF-8" -cp $classpath integration.PolicyEvaluatorTest
if ($LASTEXITCODE -ne 0) { throw "PolicyEvaluator test failed." }

Write-Host "[BASELINE] Compile PolicyOptimizationService test"
& $javacExe -encoding UTF-8 -sourcepath (Join-Path $repoRoot "source") -cp $classpath -d $outDir (Join-Path $repoRoot "source\integration\PolicyOptimizationServiceTest.java")
if ($LASTEXITCODE -ne 0) { throw "PolicyOptimizationService test compile failed." }

Write-Host "[BASELINE] Run PolicyOptimizationService test"
& $javaExe "-Dfile.encoding=UTF-8" -cp $classpath integration.PolicyOptimizationServiceTest
if ($LASTEXITCODE -ne 0) { throw "PolicyOptimizationService test failed." }

Write-Host "[BASELINE] Compile RL interface reservation test"
& $javacExe -encoding UTF-8 -sourcepath (Join-Path $repoRoot "source") -cp $classpath -d $outDir (Join-Path $repoRoot "source\integration\RLInterfaceReservationTest.java")
if ($LASTEXITCODE -ne 0) { throw "RL interface reservation test compile failed." }

Write-Host "[BASELINE] Run RL interface reservation test"
& $javaExe "-Dfile.encoding=UTF-8" -cp $classpath integration.RLInterfaceReservationTest
if ($LASTEXITCODE -ne 0) { throw "RL interface reservation test failed." }

Write-Host "[BASELINE] Compile WorldModel test"
& $javacExe -encoding UTF-8 -sourcepath (Join-Path $repoRoot "source") -cp $classpath -d $outDir (Join-Path $repoRoot "source\integration\WorldModelTest.java")
if ($LASTEXITCODE -ne 0) { throw "WorldModel test compile failed." }

Write-Host "[BASELINE] Run WorldModel test"
& $javaExe "-Dfile.encoding=UTF-8" -cp $classpath integration.WorldModelTest
if ($LASTEXITCODE -ne 0) { throw "WorldModel test failed." }

Write-Host "[BASELINE] Compile route planner test"
& $javacExe -encoding UTF-8 -sourcepath (Join-Path $repoRoot "source") -cp $classpath -d $outDir (Join-Path $repoRoot "source\integration\RoutePlannerTest.java")
if ($LASTEXITCODE -ne 0) { throw "Route planner test compile failed." }

Write-Host "[BASELINE] Run route planner test"
& $javaExe "-Dfile.encoding=UTF-8" -cp $classpath integration.RoutePlannerTest
if ($LASTEXITCODE -ne 0) { throw "Route planner test failed." }

Write-Host "[BASELINE] Compile route feature selection test"
& $javacExe -encoding UTF-8 -sourcepath (Join-Path $repoRoot "source") -cp $classpath -d $outDir (Join-Path $repoRoot "source\integration\RouteFeatureSelectionTest.java")
if ($LASTEXITCODE -ne 0) { throw "Route feature selection test compile failed." }

Write-Host "[BASELINE] Run route feature selection test"
& $javaExe "-Dfile.encoding=UTF-8" -cp $classpath integration.RouteFeatureSelectionTest
if ($LASTEXITCODE -ne 0) { throw "Route feature selection test failed." }

Write-Host "[BASELINE] Compile conflict resolver test"
& $javacExe -encoding UTF-8 -sourcepath (Join-Path $repoRoot "source") -cp $classpath -d $outDir (Join-Path $repoRoot "source\integration\ConflictResolverTest.java")
if ($LASTEXITCODE -ne 0) { throw "Conflict resolver test compile failed." }

Write-Host "[BASELINE] Run conflict resolver test"
& $javaExe "-Dfile.encoding=UTF-8" -cp $classpath integration.ConflictResolverTest
if ($LASTEXITCODE -ne 0) { throw "Conflict resolver test failed." }

Write-Host "[BASELINE] Compile scenario library test"
& $javacExe -encoding UTF-8 -sourcepath (Join-Path $repoRoot "source") -cp $classpath -d $outDir (Join-Path $repoRoot "source\integration\ScenarioLibraryTest.java")
if ($LASTEXITCODE -ne 0) { throw "Scenario library test compile failed." }

Write-Host "[BASELINE] Run scenario library test"
& $javaExe "-Dfile.encoding=UTF-8" -cp $classpath integration.ScenarioLibraryTest
if ($LASTEXITCODE -ne 0) { throw "Scenario library test failed." }

Write-Host "[BASELINE] Compile evaluation service test"
& $javacExe -encoding UTF-8 -sourcepath (Join-Path $repoRoot "source") -cp $classpath -d $outDir (Join-Path $repoRoot "source\integration\EvaluationServiceTest.java")
if ($LASTEXITCODE -ne 0) { throw "Evaluation service test compile failed." }

Write-Host "[BASELINE] Run evaluation service test"
& $javaExe "-Dfile.encoding=UTF-8" -cp $classpath integration.EvaluationServiceTest
if ($LASTEXITCODE -ne 0) { throw "Evaluation service test failed." }

Write-Host "[BASELINE] Start backend API on http://127.0.0.1:$port/api"
$apiProcess = Start-Process -FilePath $javaExe `
  -ArgumentList @("-Dfile.encoding=UTF-8", "-cp", $classpath, "server.FrontendApiServer", "$port") `
  -WorkingDirectory $repoRoot `
  -RedirectStandardOutput $apiOut `
  -RedirectStandardError $apiErr `
  -WindowStyle Hidden `
  -PassThru

try {
  $ready = $false
  for ($i = 0; $i -lt 50; $i++) {
    try {
      $health = Invoke-RestMethod -Uri "http://127.0.0.1:$port/api/health" -Method Get -TimeoutSec 2
      if ($health.ok) {
        $ready = $true
        break
      }
    } catch {
      Start-Sleep -Milliseconds 400
    }
  }
  if (-not $ready) {
    if (Test-Path -LiteralPath $apiErr) { Get-Content -LiteralPath $apiErr -Tail 80 }
    if (Test-Path -LiteralPath $apiOut) { Get-Content -LiteralPath $apiOut -Tail 80 }
    throw "Backend API did not become ready."
  }

  Write-Host "[BASELINE] Run API smoke test"
  & powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $repoRoot "tools\api_smoke_test.ps1") -BaseUrl "http://127.0.0.1:$port/api"
  if ($LASTEXITCODE -ne 0) { throw "API smoke test failed." }

  Write-Host "[BASELINE] Run intent collaboration E2E test"
  & powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $repoRoot "tools\e2e_intent_collaboration_test.ps1") -BaseUrl "http://127.0.0.1:$port/api"
  if ($LASTEXITCODE -ne 0) { throw "Intent collaboration E2E test failed." }

  Write-Host "[BASELINE] Test baseline passed."
} finally {
  if ($apiProcess -and -not $apiProcess.HasExited) {
    Stop-Process -Id $apiProcess.Id -Force
  }
}
