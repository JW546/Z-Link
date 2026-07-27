param(
  [switch]$Clean,
  [string]$BuildRoot = "C:\ros2_urban_runtime_ws"
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
$projectWsRoot = Join-Path $repoRoot "ros2_ws"
$wsRoot = $BuildRoot
Set-Location $repoRoot

. (Join-Path $repoRoot "tools\setup_ros2_humble_env.ps1")

function Import-VisualStudioBuildEnv {
  if ($env:VisualStudioVersion) {
    return
  }

  $vswhere = "C:\Program Files (x86)\Microsoft Visual Studio\Installer\vswhere.exe"
  $installPath = $null
  if (Test-Path -LiteralPath $vswhere) {
    $installPath = & $vswhere -latest -products * -requires Microsoft.VisualStudio.Component.VC.Tools.x86.x64 -property installationPath
  }
  if (-not $installPath) {
    $known = @(
      "C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools",
      "C:\Program Files\Microsoft Visual Studio\2022\BuildTools",
      "C:\VS\Community"
    )
    foreach ($item in $known) {
      if (Test-Path -LiteralPath (Join-Path $item "Common7\Tools\VsDevCmd.bat")) {
        $installPath = $item
        break
      }
    }
  }
  if (-not $installPath) {
    throw "Visual Studio C++ Build Tools not found. Install Desktop development with C++ / MSVC v143, then rerun this script."
  }

  $vsDevCmd = Join-Path $installPath "Common7\Tools\VsDevCmd.bat"
  if (-not (Test-Path -LiteralPath $vsDevCmd)) {
    throw "VsDevCmd.bat not found: $vsDevCmd"
  }

  Write-Host "[ROS2-IFACE] Loading Visual Studio build environment: $vsDevCmd"
  $envDump = cmd /c "`"$vsDevCmd`" -arch=x64 -host_arch=x64 >nul && set"
  foreach ($line in $envDump) {
    $idx = $line.IndexOf("=")
    if ($idx -gt 0) {
      [Environment]::SetEnvironmentVariable($line.Substring(0, $idx), $line.Substring($idx + 1), "Process")
    }
  }
}

Import-VisualStudioBuildEnv
$env:PYTHONIOENCODING = "utf-8"
$env:PYTHONUTF8 = "1"
$env:COMPUTERNAME = "LOCALROS2"
$env:USERDOMAIN = "LOCALROS2"
$env:USERDOMAIN_ROAMINGPROFILE = "LOCALROS2"
$env:LOGONSERVER = "\\LOCALROS2"

if ($Clean) {
  $resolvedBuildRoot = [System.IO.Path]::GetFullPath($wsRoot)
  if (-not $resolvedBuildRoot.StartsWith("C:\ros2_urban_runtime_ws", [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "Refusing to clean unexpected ROS2 build root: $resolvedBuildRoot"
  }
  Remove-Item -LiteralPath (Join-Path $wsRoot "build"), (Join-Path $wsRoot "install"), (Join-Path $wsRoot "log") -Recurse -Force -ErrorAction SilentlyContinue
}

$srcPackage = Join-Path $projectWsRoot "src\urban_runtime_msgs"
$dstPackage = Join-Path $wsRoot "src\urban_runtime_msgs"
New-Item -ItemType Directory -Force -Path (Split-Path $dstPackage) | Out-Null
Remove-Item -LiteralPath $dstPackage -Recurse -Force -ErrorAction SilentlyContinue
Copy-Item -LiteralPath $srcPackage -Destination $dstPackage -Recurse -Force
Write-Host "[ROS2-IFACE] Copied interface package to ASCII build root: $wsRoot"

Push-Location $wsRoot
try {
  colcon build --packages-select urban_runtime_msgs
  if ($LASTEXITCODE -ne 0) { throw "urban_runtime_msgs colcon build failed." }
} finally {
  Pop-Location
}

. (Join-Path $wsRoot "install\local_setup.ps1")
Write-Host "[ROS2-IFACE] Built and sourced urban_runtime_msgs."
ros2 interface show urban_runtime_msgs/msg/RuntimeAgentStatus
ros2 interface show urban_runtime_msgs/msg/RouteDecision
