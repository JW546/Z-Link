param(
  [int]$Port = 19090,
  [int]$IntervalMs = 500,
  [int]$Ticks = 120
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

& $javacExe -encoding UTF-8 -sourcepath (Join-Path $repoRoot "source") -cp $classpath -d $outDir (Join-Path $repoRoot "source\collaboration\runtime\CollaborationSystemMain.java")
if ($LASTEXITCODE -ne 0) { throw "Local distributed runtime compile failed." }

$broker = Start-Process -FilePath $javaExe -ArgumentList @("-Dfile.encoding=UTF-8", "-cp", $classpath, "collaboration.runtime.LocalDDSBrokerMain", "$Port") -WorkingDirectory $repoRoot -WindowStyle Hidden -PassThru
Start-Sleep -Milliseconds 600
$coordinator = Start-Process -FilePath $javaExe -ArgumentList @("-Dfile.encoding=UTF-8", "-cp", $classpath, "collaboration.runtime.CoordinatorAgentMain", "127.0.0.1", "$Port", "60000") -WorkingDirectory $repoRoot -WindowStyle Hidden -PassThru
$uav = Start-Process -FilePath $javaExe -ArgumentList @("-Dfile.encoding=UTF-8", "-cp", $classpath, "collaboration.runtime.RuntimeDeviceAgent", "127.0.0.1", "$Port", "UAV", "$IntervalMs", "$Ticks") -WorkingDirectory $repoRoot -WindowStyle Hidden -PassThru
$ego = Start-Process -FilePath $javaExe -ArgumentList @("-Dfile.encoding=UTF-8", "-cp", $classpath, "collaboration.runtime.RuntimeDeviceAgent", "127.0.0.1", "$Port", "EGO_CAR", "$IntervalMs", "$Ticks") -WorkingDirectory $repoRoot -WindowStyle Hidden -PassThru
$peer = Start-Process -FilePath $javaExe -ArgumentList @("-Dfile.encoding=UTF-8", "-cp", $classpath, "collaboration.runtime.RuntimeDeviceAgent", "127.0.0.1", "$Port", "PEER_CAR", "$IntervalMs", "$Ticks") -WorkingDirectory $repoRoot -WindowStyle Hidden -PassThru

Write-Host "[LOCAL-RUNTIME] Started broker=$($broker.Id), coordinator=$($coordinator.Id), uav=$($uav.Id), ego=$($ego.Id), peer=$($peer.Id)"
Write-Host "[LOCAL-RUNTIME] Stop them with: Stop-Process -Id $($broker.Id),$($coordinator.Id),$($uav.Id),$($ego.Id),$($peer.Id)"
