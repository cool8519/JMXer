param(
    [Parameter(Mandatory = $true)]
    [string] $JdkHome,
    [Parameter(Mandatory = $true)]
    [string] $LanternaJar,
    [Parameter(Mandatory = $true)]
    [string] $SnakeYamlJar,
    [Parameter(Mandatory = $true)]
    [string] $OutClasses,
    [Parameter(Mandatory = $true)]
    [string] $SrcRoot
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
$tj = Join-Path $JdkHome 'lib\tools.jar'
if (-not (Test-Path $tj)) {
    $tj = Join-Path $JdkHome 'lib/tools.jar'
}
if (-not (Test-Path $javac)) { throw "javac not found under $JdkHome" }
if (-not (Test-Path $tj)) { throw "tools.jar not found under $JdkHome" }

$cp = "$LanternaJar;$SnakeYamlJar;$tj"
$sources = Get-ChildItem -Recurse -Path $SrcRoot -Filter '*.java' | ForEach-Object { $_.FullName }
& $javac -encoding UTF-8 -source 8 -target 8 -cp $cp -d $OutClasses @sources
