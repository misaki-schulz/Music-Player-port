param([string[]]$Versions = @())
$ErrorActionPreference = 'Stop'
$targets = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'targets.json') -Raw | ConvertFrom-Json
foreach ($version in $Versions) {
    if ($version -notin $targets.minecraft) { throw "Unsupported Minecraft version: $version" }
}
foreach ($target in $targets) {
    if ($Versions.Count -gt 0 -and $target.minecraft -notin $Versions) { continue }
    & (Join-Path $PSScriptRoot 'Build-Target.ps1') -Group $target.group -Version $target.minecraft -Tasks assemble
}
Write-Host 'Compilation complete. Minecraft was not launched. Run python ports/package_ports.py to validate and collect 8 grouped release JARs (15 exact builds remain available).'
