param(
    [Parameter(Mandatory = $true)]
    [string] $RepoRoot
)

$ErrorActionPreference = 'Stop'
$r = [IO.Path]::GetFullPath($RepoRoot)
$runtime = Join-Path $r 'build\jdk17-runtime'
New-Item -ItemType Directory -Force -Path $runtime | Out-Null

function Test-Jdk17Home([string] $jdkHome) {
    if ([string]::IsNullOrWhiteSpace($jdkHome)) {
        return $false
    }
    $javac = Join-Path $jdkHome 'bin\javac.exe'
    if (-not (Test-Path -LiteralPath $javac)) {
        $javac = Join-Path $jdkHome 'bin/javac'
    }
    if (-not (Test-Path -LiteralPath $javac)) {
        return $false
    }
    $prevEap = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $v = (cmd /c "`"$javac`" -version 2>&1") | Out-String
    } finally {
        $ErrorActionPreference = $prevEap
    }
    return $v -match '\b17\.'
}

function Find-Jdk17Home([string] $searchRoot) {
    if (Test-Jdk17Home $searchRoot) {
        return $searchRoot
    }
    $dirs = Get-ChildItem -LiteralPath $searchRoot -Directory -ErrorAction SilentlyContinue
    foreach ($d in $dirs) {
        $found = Find-Jdk17Home $d.FullName
        if ($found) {
            return $found
        }
    }
    return $null
}

$found = Find-Jdk17Home $runtime
if ($found) {
    Write-Output $found
    exit 0
}

$win = ($env:OS -eq 'Windows_NT')
$url = if ($win) {
    'https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse?project=jdk'
}
else {
    'https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse?project=jdk'
}

$zip = Join-Path $runtime '_jdk17_download.zip'
Invoke-WebRequest -Uri $url -OutFile $zip -MaximumRedirection 5 -UseBasicParsing
Expand-Archive -LiteralPath $zip -DestinationPath $runtime -Force
Remove-Item -LiteralPath $zip -Force

$found = Find-Jdk17Home $runtime
if (-not $found) {
    throw 'JDK 17 unpack failed under build/jdk17-runtime (javac.exe not found).'
}

Write-Output $found
