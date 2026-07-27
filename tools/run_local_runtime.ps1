param(
  [int]$IntervalMs = 500,
  [int]$Ticks = 8
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
if ($LASTEXITCODE -ne 0) { throw "Local runtime compile failed." }

& $javaExe "-Dfile.encoding=UTF-8" -cp $classpath collaboration.runtime.CollaborationSystemMain $IntervalMs $Ticks
if ($LASTEXITCODE -ne 0) { throw "Local runtime failed." }
