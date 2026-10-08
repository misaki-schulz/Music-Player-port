param(
    [Parameter(Mandatory=$true)][string]$Group,
    [Parameter(Mandatory=$true)][string]$Version,
    [string[]]$Tasks = @('assemble')
)
$ErrorActionPreference = 'Stop'
$projectPath = Join-Path $PSScriptRoot $Group
if (!(Test-Path -LiteralPath (Join-Path $projectPath 'gradlew.bat'))) { throw "Unknown port group: $Group" }
$buildMutex = [System.Threading.Mutex]::new($false, 'Local\MusicPlayerPort7Build')
$acquired = $false
$previousJavaHome = $env:JAVA_HOME
try {
    Write-Host "Waiting for build slot: Minecraft $Version ($Group)"
    try { $acquired = $buildMutex.WaitOne() } catch [System.Threading.AbandonedMutexException] { $acquired = $true }
    Push-Location $projectPath
    try {
        $legacyLoader = $Group.StartsWith('forge-') -and $Version -ne '1.20.1'
        $gradleJava = if ($legacyLoader) { 21 } else { 25 }
        $javaCandidates = @()
        if ($env:JAVA_HOME) { $javaCandidates += Get-Item -LiteralPath $env:JAVA_HOME -ErrorAction SilentlyContinue }
        $javaCandidates += Get-ChildItem -LiteralPath (Join-Path $env:ProgramFiles 'Java') -Directory -ErrorAction SilentlyContinue
        $javaCandidates += Get-ChildItem -LiteralPath (Join-Path $env:USERPROFILE '.jdks') -Directory -ErrorAction SilentlyContinue
        $selectedJava = $javaCandidates | Where-Object {
            $releaseFile = Join-Path $_.FullName 'release'
            (Test-Path -LiteralPath $releaseFile) -and ((Get-Content -LiteralPath $releaseFile -Raw) -match ('JAVA_VERSION="' + $gradleJava + '[." ]'))
        } | Select-Object -First 1
        if (!$selectedJava) { throw "Install JDK $gradleJava to run this Gradle wrapper" }
        $env:JAVA_HOME = $selectedJava.FullName
        & .\gradlew.bat @Tasks "-PtargetVersion=$Version" '--no-daemon' '--max-workers=1' '-Dorg.gradle.jvmargs=-Xmx1200m -Dfile.encoding=UTF-8' '--console=plain'
        if ($LASTEXITCODE -ne 0) { throw "Gradle failed for Minecraft $Version (exit $LASTEXITCODE)" }
    } finally { Pop-Location }
} finally {
    $env:JAVA_HOME = $previousJavaHome
    if ($acquired) { $buildMutex.ReleaseMutex() }
    $buildMutex.Dispose()
}
