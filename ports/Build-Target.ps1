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
try {
    Write-Host "Waiting for build slot: Minecraft $Version ($Group)"
    try { $acquired = $buildMutex.WaitOne() } catch [System.Threading.AbandonedMutexException] { $acquired = $true }
    Push-Location $projectPath
    try {
        & .\gradlew.bat @Tasks "-PtargetVersion=$Version" '--no-daemon' '--max-workers=1' '-Dorg.gradle.jvmargs=-Xmx1200m -Dfile.encoding=UTF-8' '--console=plain'
        if ($LASTEXITCODE -ne 0) { throw "Gradle failed for Minecraft $Version (exit $LASTEXITCODE)" }
    } finally { Pop-Location }
} finally {
    if ($acquired) { $buildMutex.ReleaseMutex() }
    $buildMutex.Dispose()
}
