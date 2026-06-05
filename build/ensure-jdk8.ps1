param(
    [Parameter(Mandatory = $true)]
    [string] $RepoRoot
)

$ErrorActionPreference = 'Stop'
$r = [IO.Path]::GetFullPath($RepoRoot)
$runtime = Join-Path $r 'build\jdk8-runtime'
New-Item -ItemType Directory -Force -Path $runtime | Out-Null

function Test-Jdk8Home([string] $jdkHome) {
    if ([string]::IsNullOrWhiteSpace($jdkHome)) {
        return $false
    }
    $javac = Join-Path $jdkHome 'bin\javac.exe'
    if (-not (Test-Path -LiteralPath $javac)) {
        $javac = Join-Path $jdkHome 'bin/javac'
    }
    $toolsJar = Join-Path $jdkHome 'lib\tools.jar'
    if (-not (Test-Path -LiteralPath $toolsJar)) {
        $toolsJar = Join-Path $jdkHome 'lib/tools.jar'
    }
    if (-not (Test-Path -LiteralPath $javac)) {
        return $false
    }
    if (-not (Test-Path -LiteralPath $toolsJar)) {
        return $false
    }
    # javac writes version to stderr; avoid treating that as a terminating error.
    $prevEap = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $v = (cmd /c "`"$javac`" -version 2>&1") | Out-String
    } finally {
        $ErrorActionPreference = $prevEap
    }
    return $v -match '1\.8\.'
}

function Find-Jdk8Home([string] $searchRoot) {
    if (Test-Jdk8Home $searchRoot) {
        return $searchRoot
    }
    $dirs = Get-ChildItem -LiteralPath $searchRoot -Directory -ErrorAction SilentlyContinue
    foreach ($d in $dirs) {
        $found = Find-Jdk8Home $d.FullName
        if ($found) {
            return $found
        }
    }
    return $null
}

$found = Find-Jdk8Home $runtime
if ($found) {
    Write-Output $found
    exit 0
}

$win = ($env:OS -eq 'Windows_NT')
$url = if ($win) {
    'https://api.adoptium.net/v3/binary/latest/8/ga/windows/x64/jdk/hotspot/normal/eclipse?project=jdk'
}
else {
    'https://api.adoptium.net/v3/binary/latest/8/ga/linux/x64/jdk/hotspot/normal/eclipse?project=jdk'
}

$zip = Join-Path $runtime '_jdk8_download.zip'
Invoke-WebRequest -Uri $url -OutFile $zip -MaximumRedirection 5 -UseBasicParsing
Expand-Archive -LiteralPath $zip -DestinationPath $runtime -Force
Remove-Item -LiteralPath $zip -Force

$found = Find-Jdk8Home $runtime
if (-not $found) {
    throw 'JDK 8 unpack failed under build/jdk8-runtime (javac.exe or tools.jar not found).'
}

Write-Output $found
