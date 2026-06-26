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
    throw 'JDK 17 home path is empty. Set JAVA17_HOME or run ensure-jdk17.ps1 successfully.'
}
$javac = Join-Path $JdkHome 'bin\javac.exe'
if (-not (Test-Path $javac)) {
    $javac = Join-Path $JdkHome 'bin/javac'
}
if (-not (Test-Path $javac)) { throw "javac not found under $JdkHome" }

$cp = "$LanternaJar;$SnakeYamlJar"
$sources = Get-ChildItem -Recurse -Path $SrcRoot -Filter '*.java' |
    Where-Object { $_.Name -ne 'JmxLauncher.java' } |
    ForEach-Object { $_.FullName }
& $javac -encoding UTF-8 -source 17 -target 17 -cp $cp -d $OutClasses @sources
