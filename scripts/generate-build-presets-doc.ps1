param(
    [string]$OutputPath = (Join-Path $PSScriptRoot 'BUILD_PRESETS.md')
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$modeDefinitionsPath = Join-Path $PSScriptRoot 'build-modes.ps1'
$profileDefinitionsPath = Join-Path $PSScriptRoot 'build-profiles.ps1'

if (-not (Test-Path $modeDefinitionsPath)) {
    throw "Build mode definitions file not found: $modeDefinitionsPath"
}
if (-not (Test-Path $profileDefinitionsPath)) {
    throw "Build profile definitions file not found: $profileDefinitionsPath"
}

. $modeDefinitionsPath
. $profileDefinitionsPath

$buildModes = Get-BuildModes
$buildProfiles = Get-BuildProfiles

function Format-Overrides {
    param(
        [hashtable]$Overrides
    )

    if ($null -eq $Overrides -or $Overrides.Count -eq 0) {
        return 'none'
    }

    $items = @()
    foreach ($entry in $Overrides.GetEnumerator() | Sort-Object Name) {
        $items += ($entry.Key + '=' + [string]([bool]$entry.Value))
    }
    return $items -join ', '
}

function Append-Line {
    param(
        [System.Collections.Generic.List[string]]$Buffer,
        [string]$Text = ''
    )

    $Buffer.Add($Text) | Out-Null
}

$lines = New-Object 'System.Collections.Generic.List[string]'
$generatedAt = Get-Date -Format 'yyyy-MM-dd HH:mm:ss zzz'

Append-Line -Buffer $lines -Text '# Build Presets Reference'
Append-Line -Buffer $lines
Append-Line -Buffer $lines -Text '> Generated from `scripts/build-modes.ps1` and `scripts/build-profiles.ps1`.'
Append-Line -Buffer $lines -Text ("> Generated at: " + $generatedAt)
Append-Line -Buffer $lines -Text '> Regenerate with: `powershell.exe -ExecutionPolicy Bypass -File scripts/generate-build-presets-doc.ps1`'
Append-Line -Buffer $lines
Append-Line -Buffer $lines -Text '## Purpose'
Append-Line -Buffer $lines
Append-Line -Buffer $lines -Text 'This document is a static quick-reference for the Windows-friendly build and verification presets used by `scripts/verified-build.ps1`.'
Append-Line -Buffer $lines
Append-Line -Buffer $lines -Text '## Modes'
Append-Line -Buffer $lines
Append-Line -Buffer $lines -Text '| Mode | Description | Overrides |'
Append-Line -Buffer $lines -Text '| --- | --- | --- |'

foreach ($modeName in ($buildModes.Keys | Sort-Object)) {
    $definition = $buildModes[$modeName]
    $description = if ($definition.Description) { $definition.Description } else { '' }
    $overrides = Format-Overrides -Overrides $definition.Overrides
    Append-Line -Buffer $lines -Text ('| `{0}` | {1} | `{2}` |' -f $modeName, $description, $overrides)
}

Append-Line -Buffer $lines
Append-Line -Buffer $lines -Text '## Profiles'

foreach ($profileName in ($buildProfiles.Keys | Sort-Object)) {
    $definition = $buildProfiles[$profileName]
    Append-Line -Buffer $lines
    Append-Line -Buffer $lines -Text ('### `{0}`' -f $profileName)
    Append-Line -Buffer $lines
    Append-Line -Buffer $lines -Text $definition.Description
    Append-Line -Buffer $lines
    Append-Line -Buffer $lines -Text '| Plan | Module | Tests |'
    Append-Line -Buffer $lines -Text '| --- | --- | --- |'

    foreach ($plan in @($definition.Plans)) {
        $tests = @($plan.Tests) -join ', '
        Append-Line -Buffer $lines -Text ('| `{0}` | `{1}` | `{2}` |' -f $plan.Name, $plan.Module, $tests)
    }
}

Append-Line -Buffer $lines
Append-Line -Buffer $lines -Text '## Discovery Commands'
Append-Line -Buffer $lines
Append-Line -Buffer $lines -Text '```powershell'
Append-Line -Buffer $lines -Text 'Get-Help .\scripts\verified-build.ps1 -Detailed'
Append-Line -Buffer $lines -Text 'powershell.exe -ExecutionPolicy Bypass -File scripts/verified-build.ps1 -ListModes'
Append-Line -Buffer $lines -Text 'powershell.exe -ExecutionPolicy Bypass -File scripts/verified-build.ps1 -ListProfiles'
Append-Line -Buffer $lines -Text 'powershell.exe -ExecutionPolicy Bypass -File scripts/verified-build.ps1 -ShowMode Quick'
Append-Line -Buffer $lines -Text 'powershell.exe -ExecutionPolicy Bypass -File scripts/verified-build.ps1 -ShowProfile Performance'
Append-Line -Buffer $lines -Text 'powershell.exe -ExecutionPolicy Bypass -File scripts/verified-build.ps1 -ListProfiles -RefreshDocs'
Append-Line -Buffer $lines -Text 'powershell.exe -ExecutionPolicy Bypass -File scripts/verified-build.ps1 -RefreshDocsOnly'
Append-Line -Buffer $lines -Text '```'
Append-Line -Buffer $lines
Append-Line -Buffer $lines -Text '## Common Commands'
Append-Line -Buffer $lines
Append-Line -Buffer $lines -Text '```powershell'
Append-Line -Buffer $lines -Text 'powershell.exe -ExecutionPolicy Bypass -File scripts/verified-build.ps1 -Mode Core'
Append-Line -Buffer $lines -Text 'powershell.exe -ExecutionPolicy Bypass -File scripts/verified-build.ps1 -Mode Quick'
Append-Line -Buffer $lines -Text 'powershell.exe -ExecutionPolicy Bypass -File scripts/verified-build.ps1 -Mode Selective -Profile ExternalProcess -UseDedicatedRepo'
Append-Line -Buffer $lines -Text 'powershell.exe -ExecutionPolicy Bypass -File scripts/verified-build.ps1 -Mode Selective -Profile Diagnostics -UseDedicatedRepo'
Append-Line -Buffer $lines -Text 'powershell.exe -ExecutionPolicy Bypass -File scripts/verified-build.ps1 -Mode Selective -Profile SpringBoot -UseDedicatedRepo'
Append-Line -Buffer $lines -Text 'powershell.exe -ExecutionPolicy Bypass -File scripts/verified-build.ps1 -Mode Selective -Profile Performance -UseDedicatedRepo'
Append-Line -Buffer $lines -Text 'powershell.exe -ExecutionPolicy Bypass -File scripts/verified-build.ps1 -Mode Full -UseDedicatedRepo'
Append-Line -Buffer $lines -Text 'powershell.exe -ExecutionPolicy Bypass -File scripts/verified-build.ps1 -Mode Core -RefreshDocs'
Append-Line -Buffer $lines -Text 'powershell.exe -ExecutionPolicy Bypass -File scripts/verified-build.ps1 -RefreshDocsOnly'
Append-Line -Buffer $lines -Text '```'

$content = ($lines -join "`n") + "`n"
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText($OutputPath, $content, $utf8NoBom)

Write-Host ("Generated build preset reference: " + $OutputPath) -ForegroundColor Green
