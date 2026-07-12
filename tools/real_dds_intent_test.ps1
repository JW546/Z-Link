param(
  [string]$RtiHome = "D:\DDS\rti_connext_dds-7.6.0",
  [ValidateSet("loopback", "receive", "send")]
  [string]$Mode = "loopback",
  [int]$DomainId = 43,
  [string]$SourceId = "UAV-CQ-007",
  [string]$TargetId = "CAR-CQ-001"
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

$javaExe = Find-Executable "java.exe" @(
  (Join-Path $repoRoot "runtime\jdk\bin\java.exe"),
  (Join-Path $repoRoot "jdk\bin\java.exe")
)
$javacExe = Find-Executable "javac.exe" @(
  (Join-Path $repoRoot "runtime\jdk\bin\javac.exe"),
  (Join-Path $repoRoot "jdk\bin\javac.exe")
)
$rtiddsgen = Find-Executable "rtiddsgen.bat" @(
  (Join-Path $RtiHome "bin\rtiddsgen.bat")
)
$rtiLib = Join-Path $RtiHome "lib\x64Win64VS2017"

if (-not $javaExe) { throw "java.exe not found." }
if (-not $javacExe) { throw "javac.exe not found." }
if (-not $rtiddsgen) { throw "rtiddsgen.bat not found. Pass -RtiHome to your RTI Connext installation." }
if (-not (Test-Path -LiteralPath (Join-Path $rtiLib "nddsc.dll"))) {
  throw "RTI native library path missing nddsc.dll: $rtiLib"
}

Copy-Item -Force (Join-Path $RtiHome "lib\java\nddsjava.jar") (Join-Path $repoRoot "lib\nddsjava.jar")
Copy-Item -Force (Join-Path $rtiLib "nddscore.dll") (Join-Path $repoRoot "lib\nddscore.dll")
Copy-Item -Force (Join-Path $rtiLib "nddsc.dll") (Join-Path $repoRoot "lib\nddsc.dll")
Copy-Item -Force (Join-Path $rtiLib "nddsjava.dll") (Join-Path $repoRoot "lib\nddsjava.dll")

Write-Host "[REAL-DDS] Generate Java IDL types"
if (Test-Path -LiteralPath (Join-Path $repoRoot "interfaces\intentdds")) {
  Remove-Item -Recurse -Force (Join-Path $repoRoot "interfaces\intentdds")
}
& $rtiddsgen -language Java -package intentdds -replace -d (Join-Path $repoRoot "interfaces") (Join-Path $repoRoot "idl\urban_intent_collaboration.idl")
if ($LASTEXITCODE -ne 0) { throw "rtiddsgen failed." }

$outDir = Join-Path $repoRoot "build-classes"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$classpath = @(
  (Join-Path $repoRoot "build-classes"),
  (Join-Path $repoRoot "generated-src"),
  (Join-Path $repoRoot "interfaces"),
  (Join-Path $repoRoot "lib\*")
) -join ";"

Write-Host "[REAL-DDS] Compile DDSManager and real DDS demos"
& $javacExe -encoding UTF-8 -cp $classpath -sourcepath "$repoRoot\source;$repoRoot\interfaces" -d $outDir `
  (Join-Path $repoRoot "source\integration\RealDDSIntentTypesTest.java") `
  (Join-Path $repoRoot "source\integration\RealDDSIntentPeerDemo.java")
if ($LASTEXITCODE -ne 0) { throw "compile failed." }

$env:PATH = (Join-Path $repoRoot "lib") + ";" + $rtiLib + ";" + $env:PATH
$javaArgs = @(
  "-Dfile.encoding=UTF-8",
  "-Djava.library.path=$(Join-Path $repoRoot 'lib');$rtiLib",
  "-cp",
  $classpath,
  $(if ($Mode -eq "loopback") { "integration.RealDDSIntentTypesTest" } else { "integration.RealDDSIntentPeerDemo" })
)

if ($Mode -ne "loopback") {
  $javaArgs += @($Mode, "$DomainId", $SourceId, $TargetId)
}

Write-Host "[REAL-DDS] Run real RTI DDS mode=$Mode"
& $javaExe @javaArgs
if ($LASTEXITCODE -ne 0) { throw "real DDS intent test failed." }
