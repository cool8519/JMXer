param(
    [Parameter(Mandatory = $true)]
    [string] $JdkHome,
    [Parameter(Mandatory = $true)]
    [string] $OutClasses,
    [Parameter(Mandatory = $true)]
    [string] $LauncherSource
)

$ErrorActionPreference = 'Stop'
$JdkHome = [IO.Path]::GetFullPath($JdkHome.Trim())
if ([string]::IsNullOrWhiteSpace($JdkHome)) {
    throw 'JDK 8 home path is empty. Set JAVA8_HOME or run ensure-jdk8.ps1 successfully.'
}
$javac = Join-Path $JdkHome 'bin\javac.exe'
if (-not (Test-Path $javac)) {
    $javac = Join-Path $JdkHome 'bin/javac'
}
if (-not (Test-Path $javac)) { throw "javac not found under $JdkHome" }
if (-not (Test-Path $LauncherSource)) { throw "Launcher source not found: $LauncherSource" }

& $javac -encoding UTF-8 -source 8 -target 8 -d $OutClasses $LauncherSource
