<#
.SYNOPSIS
Runs the verified Windows-friendly module-by-module build flow for LingoNexus.

.DESCRIPTION
Bootstraps the required parent POMs and upstream modules into a local Maven repository,
then verifies downstream modules in a Windows-friendly order that avoids the reactor
classpath issue documented in docs/build-troubleshooting.md.

The script supports reusable mode presets, focused verification profiles, dedicated
repository bootstrapping, and discovery commands for listing or inspecting presets.

.PARAMETER Mode
Selects the base build mode. Available values are loaded from scripts/build-modes.ps1.

.PARAMETER Profile
Selects an optional focused verification profile. Available values are loaded from
scripts/build-profiles.ps1. Use None to disable profile-specific test execution.

.PARAMETER ListModes
Lists all available build modes with descriptions and override summaries, then exits.

.PARAMETER ListProfiles
Lists all available build profiles with descriptions, plan counts, and test counts, then exits.

.PARAMETER ShowMode
Shows one build mode in detail, including its description and flag overrides, then exits.

.PARAMETER ShowProfile
Shows one build profile in detail, including its description, module targets, and test lists, then exits.

.PARAMETER RefreshDocs
Regenerates `scripts/BUILD_PRESETS.md` from the current mode/profile definitions before exiting or running the build flow.

.PARAMETER RefreshDocsOnly
Regenerates `scripts/BUILD_PRESETS.md` and exits without running build steps.

.PARAMETER SkipExamples
Skips the examples module compile step.

.PARAMETER SkipScripts
Skips the script language module install steps.

.PARAMETER SkipUtils
Skips the utils module install step.

.PARAMETER SkipModules
Skips the modules install step.

.PARAMETER SkipTestcases
Skips testcase compilation and profile testcase execution.

.PARAMETER ReinstallUpstream
Forces reinstall of upstream modules even when artifacts already exist in the selected repository.

.PARAMETER RepairInstalledJars
Repacks locally built jars from target/classes when jar contents are incomplete on this machine.

.PARAMETER RetryCount
Controls how many times Maven commands are retried after failure.

.PARAMETER RetryDelaySeconds
Controls the pause between Maven command retries.

.PARAMETER UseDedicatedRepo
Uses a project-local Maven repository. If MavenRepoPath is omitted, defaults to .verified-m2-repo.

.PARAMETER MavenRepoPath
Overrides the Maven local repository path used by this script.

.EXAMPLE
powershell.exe -ExecutionPolicy Bypass -File scripts/verified-build.ps1 -Mode Core

Runs the fastest core-only verification flow.

.EXAMPLE
powershell.exe -ExecutionPolicy Bypass -File scripts/verified-build.ps1 -Mode Selective -Profile ExternalProcess -UseDedicatedRepo

Runs the focused external-process verification profile in an isolated local repository.

.EXAMPLE
powershell.exe -ExecutionPolicy Bypass -File scripts/verified-build.ps1 -ListProfiles

Lists all focused verification profiles and exits without running a build.

.EXAMPLE
powershell.exe -ExecutionPolicy Bypass -File scripts/verified-build.ps1 -Mode Core -RefreshDocs

Refreshes the generated preset reference and then runs the core-only verification flow.

.EXAMPLE
powershell.exe -ExecutionPolicy Bypass -File scripts/verified-build.ps1 -RefreshDocsOnly

Refreshes the generated preset reference and exits immediately.

.EXAMPLE
Get-Help .\scripts\verified-build.ps1 -Detailed

Shows the full script help inside PowerShell.
#>
param(
    [string]$Mode = 'Selective',
    [string]$Profile = 'None',
    [switch]$ListModes,
    [switch]$ListProfiles,
    [string]$ShowMode,
    [string]$ShowProfile,
    [switch]$RefreshDocs,
    [switch]$RefreshDocsOnly,
    [switch]$SkipExamples,
    [switch]$SkipScripts,
    [switch]$SkipUtils,
    [switch]$SkipModules,
    [switch]$SkipTestcases,
    [switch]$ReinstallUpstream,
    [switch]$RepairInstalledJars = $true,
    [int]$RetryCount = 3,
    [int]$RetryDelaySeconds = 2,
    [switch]$UseDedicatedRepo,
    [string]$MavenRepoPath
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$modeDefinitionsPath = Join-Path $PSScriptRoot 'build-modes.ps1'
if (-not (Test-Path $modeDefinitionsPath)) {
    throw "Build mode definitions file not found: $modeDefinitionsPath"
}
. $modeDefinitionsPath
$buildModes = Get-BuildModes
$availableModes = if ($null -eq $buildModes) { @() } else { @($buildModes.Keys) }

$profileDefinitionsPath = Join-Path $PSScriptRoot 'build-profiles.ps1'
if (-not (Test-Path $profileDefinitionsPath)) {
    throw "Build profile definitions file not found: $profileDefinitionsPath"
}
. $profileDefinitionsPath
$buildProfiles = Get-BuildProfiles
$availableProfiles = if ($null -eq $buildProfiles) { @() } else { @($buildProfiles.Keys) }

$inspectionRequested = $ListModes -or $ListProfiles -or
        (-not [string]::IsNullOrWhiteSpace($ShowMode)) -or
        (-not [string]::IsNullOrWhiteSpace($ShowProfile))
$exitAfterRefreshOnly = $RefreshDocsOnly

if (-not [string]::IsNullOrWhiteSpace($ShowMode) -and ($null -eq $buildModes -or -not $buildModes.ContainsKey($ShowMode))) {
    throw "Unknown build mode '$ShowMode'. Available modes: $($availableModes -join ', ')"
}

if (-not [string]::IsNullOrWhiteSpace($ShowProfile) -and ($null -eq $buildProfiles -or -not $buildProfiles.ContainsKey($ShowProfile))) {
    throw "Unknown build profile '$ShowProfile'. Available profiles: $($availableProfiles -join ', ')"
}

if (-not ($inspectionRequested -or $exitAfterRefreshOnly) -and ($null -eq $buildModes -or -not $buildModes.ContainsKey($Mode))) {
    throw "Unknown build mode '$Mode'. Available modes: $($availableModes -join ', ')"
}

if (-not ($inspectionRequested -or $exitAfterRefreshOnly) -and $Profile -ne 'None' -and ($null -eq $buildProfiles -or -not $buildProfiles.ContainsKey($Profile))) {
    throw "Unknown build profile '$Profile'. Available profiles: None, $($availableProfiles -join ', ')"
}

function Invoke-Step {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name,
        [Parameter(Mandatory = $true)]
        [scriptblock]$Action
    )

    Write-Host "==> $Name" -ForegroundColor Cyan
    & $Action
}

function Write-ExecutionProfile {
    $skipSummary = @()
    if ($SkipScripts) {
        $skipSummary += 'scripts'
    }
    if ($SkipUtils) {
        $skipSummary += 'utils'
    }
    if ($SkipModules) {
        $skipSummary += 'modules'
    }
    if ($SkipExamples) {
        $skipSummary += 'examples'
    }
    if ($SkipTestcases) {
        $skipSummary += 'testcases'
    }

    $effectiveRepo = if ([string]::IsNullOrWhiteSpace($MavenRepoPath)) {
        'default-local-repo'
    } else {
        $MavenRepoPath
    }

    Write-Host "Mode: $Mode" -ForegroundColor DarkCyan
    Write-Host "Profile: $Profile" -ForegroundColor DarkCyan
    Write-Host "Repository: $effectiveRepo" -ForegroundColor DarkCyan
    Write-Host ('Skipped groups: ' + ($(if ($skipSummary.Count -eq 0) { 'none' } else { $skipSummary -join ', ' }))) -ForegroundColor DarkCyan
}

function Invoke-RefreshDocsIfRequested {
    if (-not ($RefreshDocs -or $RefreshDocsOnly)) {
        return
    }

    $generatorPath = Join-Path $PSScriptRoot 'generate-build-presets-doc.ps1'
    if (-not (Test-Path $generatorPath)) {
        throw "Build preset generator script not found: $generatorPath"
    }

    Invoke-Step -Name 'Refresh build preset docs' -Action {
        & powershell.exe -ExecutionPolicy Bypass -File $generatorPath
        if ($LASTEXITCODE -ne 0) {
            throw "Build preset doc generation failed with exit code $LASTEXITCODE"
        }
    }
}

function Format-OverridesSummary {
    param(
        [Parameter(Mandatory = $true)]
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

function Write-AvailableModes {
    Write-Host 'Available build modes:' -ForegroundColor Cyan
    foreach ($modeName in ($availableModes | Sort-Object)) {
        $definition = $buildModes[$modeName]
        $description = if ($definition -and $definition.Description) { $definition.Description } else { '' }
        $overrides = if ($definition -and $definition.Overrides) { Format-OverridesSummary -Overrides $definition.Overrides } else { 'none' }
        Write-Host ('- ' + $modeName + ': ' + $description)
        Write-Host ('  Overrides: ' + $overrides) -ForegroundColor DarkGray
    }
}

function Write-AvailableProfiles {
    Write-Host 'Available build profiles:' -ForegroundColor Cyan
    foreach ($profileName in ($availableProfiles | Sort-Object)) {
        $definition = $buildProfiles[$profileName]
        $description = if ($definition -and $definition.Description) { $definition.Description } else { '' }
        $planCount = if ($definition -and $definition.Plans) { @($definition.Plans).Count } else { 0 }
        $testCount = 0
        if ($definition -and $definition.Plans) {
            foreach ($plan in @($definition.Plans)) {
                $testCount += @($plan.Tests).Count
            }
        }
        Write-Host ('- ' + $profileName + ': ' + $description)
        Write-Host ('  Plans: ' + $planCount + ', Tests: ' + $testCount) -ForegroundColor DarkGray
    }
}

function Write-ModeDetails {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name
    )

    $definition = $buildModes[$Name]
    Write-Host ('Mode: ' + $Name) -ForegroundColor Cyan
    Write-Host ('Description: ' + $definition.Description)
    Write-Host ('Overrides: ' + (Format-OverridesSummary -Overrides $definition.Overrides)) -ForegroundColor DarkGray
}

function Write-ProfileDetails {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name
    )

    $definition = $buildProfiles[$Name]
    Write-Host ('Profile: ' + $Name) -ForegroundColor Cyan
    Write-Host ('Description: ' + $definition.Description)
    foreach ($plan in @($definition.Plans)) {
        Write-Host ('- Plan: ' + $plan.Name)
        Write-Host ('  Module: ' + $plan.Module) -ForegroundColor DarkGray
        Write-Host ('  Tests: ' + (@($plan.Tests) -join ', ')) -ForegroundColor DarkGray
    }
}

function Get-ProfileVerificationPlans {
    if ($Profile -eq 'None') {
        return @()
    }

    if ($null -eq $buildProfiles -or -not $buildProfiles.ContainsKey($Profile)) {
        return @()
    }

    return @($buildProfiles[$Profile].Plans)
}

function Get-ProfileVerificationModules {
    $modules = @()
    foreach ($plan in @(Get-ProfileVerificationPlans)) {
        if ($plan -and $plan.Module) {
            $modules += $plan.Module
        }
    }
    return @($modules | Select-Object -Unique)
}

if ($ListModes) {
    Write-AvailableModes
}

if ($ListProfiles) {
    Write-AvailableProfiles
}

if (-not [string]::IsNullOrWhiteSpace($ShowMode)) {
    Write-ModeDetails -Name $ShowMode
}

if (-not [string]::IsNullOrWhiteSpace($ShowProfile)) {
    Write-ProfileDetails -Name $ShowProfile
}

Invoke-RefreshDocsIfRequested

if ($inspectionRequested -or $exitAfterRefreshOnly) {
    return
}

if ($UseDedicatedRepo -and [string]::IsNullOrWhiteSpace($MavenRepoPath)) {
    $MavenRepoPath = Join-Path (Get-Location) '.verified-m2-repo'
}

if (-not [string]::IsNullOrWhiteSpace($MavenRepoPath)) {
    New-Item -ItemType Directory -Force -Path $MavenRepoPath | Out-Null
}

$modeOverrides = if ($buildModes[$Mode] -and $buildModes[$Mode].Overrides) { $buildModes[$Mode].Overrides } else { @{} }
foreach ($presetEntry in $modeOverrides.GetEnumerator()) {
    Set-Variable -Name $presetEntry.Key -Value ([bool]$presetEntry.Value) -Scope Script
}

if ($Profile -ne 'None') {
    $SkipScripts = $false
    $SkipUtils = $false
    $SkipModules = $false
    $SkipExamples = $true
    $SkipTestcases = $false
}

function Invoke-ProfileVerificationIfNeeded {
    foreach ($plan in @(Get-ProfileVerificationPlans)) {
        $profileModule = $plan.Module
        $profileTests = @($plan.Tests)
        if ([string]::IsNullOrWhiteSpace($profileModule) -or $profileTests.Count -eq 0) {
            continue
        }

        Invoke-Step -Name "Run $($plan.Name) profile tests" -Action {
            Invoke-Maven -Arguments @(
                '-pl',
                $profileModule,
                ('-Dtest=' + ($profileTests -join ',')),
                'test'
            )
        }
    }
}

function Get-ProjectRevision {
    $pom = [xml](Get-Content -Path 'pom.xml' -Raw)
    return $pom.project.properties.revision
}

function Get-RepositoryCandidates {
    $candidates = @()
    if (-not [string]::IsNullOrWhiteSpace($MavenRepoPath)) {
        $candidates += $MavenRepoPath
    }
    if ($env:MAVEN_REPO_LOCAL) {
        $candidates += $env:MAVEN_REPO_LOCAL
    }
    if ($env:USERPROFILE) {
        $candidates += (Join-Path $env:USERPROFILE '.m2\repository')
    }
    $mvnCommand = Get-Command mvn -ErrorAction SilentlyContinue
    if ($mvnCommand) {
        $candidates += (Join-Path (Split-Path $mvnCommand.Source -Parent) '..\repository')
    }
    return $candidates | Where-Object { $_ } | Select-Object -Unique
}

function Test-ArtifactInstalled {
    param(
        [Parameter(Mandatory = $true)]
        [string]$ArtifactId
    )

    $version = Get-ProjectRevision
    foreach ($repo in Get-RepositoryCandidates) {
        if (-not (Test-Path $repo)) {
            continue
        }
        $base = Join-Path $repo "io\github\koyan9\$ArtifactId\$version"
        $jar = Join-Path $base "$ArtifactId-$version.jar"
        $pom = Join-Path $base "$ArtifactId-$version.pom"
        if ((Test-Path $jar) -and (Test-Path $pom)) {
            return $true
        }
    }
    return $false
}

function Test-PomInstalled {
    param(
        [Parameter(Mandatory = $true)]
        [string]$ArtifactId
    )

    $version = Get-ProjectRevision
    foreach ($repo in Get-RepositoryCandidates) {
        if (-not (Test-Path $repo)) {
            continue
        }
        $base = Join-Path $repo "io\github\koyan9\$ArtifactId\$version"
        $pom = Join-Path $base "$ArtifactId-$version.pom"
        if (Test-Path $pom) {
            return $true
        }
    }
    return $false
}

function Install-PomProjectIfNeeded {
    param(
        [Parameter(Mandatory = $true)]
        [string]$DisplayName,
        [Parameter(Mandatory = $true)]
        [string]$ArtifactId,
        [Parameter(Mandatory = $true)]
        [string]$PomPath
    )

    if (-not $ReinstallUpstream -and (Test-PomInstalled -ArtifactId $ArtifactId)) {
        Write-Host "==> Install $DisplayName (already installed, skipping)" -ForegroundColor DarkYellow
        return
    }

    Invoke-Step -Name "Install $DisplayName" -Action {
        Invoke-Maven -Arguments @('-N', '-f', $PomPath, 'install')
    }
}

function Invoke-Maven {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments
    )

    $command = @('mvn', '--%')
    if (-not [string]::IsNullOrWhiteSpace($MavenRepoPath)) {
        $command += "-Dmaven.repo.local=$MavenRepoPath"
    }
    $command += $Arguments
    Write-Host ('    ' + ($command -join ' ')) -ForegroundColor DarkGray
    for ($attempt = 1; $attempt -le [Math]::Max(1, $RetryCount); $attempt++) {
        & powershell.exe -Command ($command -join ' ')
        if ($LASTEXITCODE -eq 0) {
            return
        }

        if ($attempt -lt [Math]::Max(1, $RetryCount)) {
            Write-Host "    Command failed, retrying in $RetryDelaySeconds second(s)..." -ForegroundColor Yellow
            Start-Sleep -Seconds $RetryDelaySeconds
        }
    }

    throw "Maven command failed with exit code $LASTEXITCODE"
}

function Get-ClassEntriesFromClassesDirectory {
    param(
        [Parameter(Mandatory = $true)]
        [string]$ClassesDirectory
    )

    if (-not (Test-Path $ClassesDirectory)) {
        return @()
    }

    $resolved = (Resolve-Path $ClassesDirectory).Path
    return Get-ChildItem -Recurse -File -Filter *.class -Path $ClassesDirectory |
        ForEach-Object { $_.FullName.Substring($resolved.Length + 1).Replace('\', '/') } |
        Sort-Object
}

function Get-ClassEntriesFromJar {
    param(
        [Parameter(Mandatory = $true)]
        [string]$JarPath
    )

    if (-not (Test-Path $JarPath)) {
        return @()
    }

    return @(jar tf $JarPath | Where-Object { $_ -like '*.class' } | Sort-Object)
}

function Sync-ArtifactToRepositories {
    param(
        [Parameter(Mandatory = $true)]
        [string]$ArtifactId,
        [Parameter(Mandatory = $true)]
        [string]$JarPath
    )

    $version = Get-ProjectRevision
    foreach ($repo in Get-RepositoryCandidates) {
        if (-not (Test-Path $repo)) {
            continue
        }
        $targetDir = Join-Path $repo "io\github\koyan9\$ArtifactId\$version"
        if (-not (Test-Path $targetDir)) {
            continue
        }
        $targetJar = Join-Path $targetDir "$ArtifactId-$version.jar"
        if (Test-Path $targetJar) {
            Copy-Item -Force $JarPath $targetJar
        }
    }
}

function Repair-ModuleJarIfNeeded {
    param(
        [Parameter(Mandatory = $true)]
        [string]$ModulePath,
        [Parameter(Mandatory = $true)]
        [string]$ArtifactId
    )

    if (-not $RepairInstalledJars) {
        return
    }

    $version = Get-ProjectRevision
    $classesDirectory = Join-Path $ModulePath 'target\classes'
    $jarPath = Join-Path $ModulePath "target\$ArtifactId-$version.jar"

    $classEntries = @(Get-ClassEntriesFromClassesDirectory -ClassesDirectory $classesDirectory)
    $jarEntries = @(Get-ClassEntriesFromJar -JarPath $jarPath)

    if ($classEntries.Count -eq 0 -or $classEntries.Count -eq $jarEntries.Count) {
        return
    }

    Write-Host "    Detected incomplete jar for $ArtifactId ($($jarEntries.Count)/$($classEntries.Count) classes). Repacking..." -ForegroundColor Yellow
    $repackedJarPath = Join-Path $ModulePath "target\$ArtifactId-$version-repacked.jar"
    if (Test-Path $repackedJarPath) {
        Remove-Item -Force $repackedJarPath
    }

    Push-Location $classesDirectory
    try {
        & jar cf $repackedJarPath .
        if ($LASTEXITCODE -ne 0) {
            throw "Failed to repack jar for $ArtifactId"
        }
    } finally {
        Pop-Location
    }

    Copy-Item -Force $repackedJarPath $jarPath
    Sync-ArtifactToRepositories -ArtifactId $ArtifactId -JarPath $jarPath
}

Write-ExecutionProfile

Install-PomProjectIfNeeded -DisplayName 'lingonexus root pom' -ArtifactId 'lingonexus' -PomPath 'pom.xml'

if ((-not $SkipScripts) -or (-not $SkipExamples) -or (-not $SkipTestcases)) {
    Install-PomProjectIfNeeded -DisplayName 'lingonexus-script parent pom' -ArtifactId 'lingonexus-script' -PomPath 'lingonexus-script\pom.xml'
}

if (-not $SkipTestcases) {
    Install-PomProjectIfNeeded -DisplayName 'lingonexus-testcase parent pom' -ArtifactId 'lingonexus-testcase' -PomPath 'lingonexus-testcase\pom.xml'
}

if (-not $ReinstallUpstream -and (Test-ArtifactInstalled -ArtifactId 'lingonexus-api')) {
    Write-Host '==> Install lingonexus-api (already installed, skipping)' -ForegroundColor DarkYellow
} else {
    Invoke-Step -Name 'Install lingonexus-api' -Action {
        Invoke-Maven -Arguments @('-pl', 'lingonexus-api', '-Dmaven.test.skip=true', 'install')
        Repair-ModuleJarIfNeeded -ModulePath 'lingonexus-api' -ArtifactId 'lingonexus-api'
    }
}

if (-not $ReinstallUpstream -and (Test-ArtifactInstalled -ArtifactId 'lingonexus-core')) {
    Write-Host '==> Install lingonexus-core (already installed, skipping)' -ForegroundColor DarkYellow
} else {
    Invoke-Step -Name 'Install lingonexus-core' -Action {
        Invoke-Maven -Arguments @('-pl', 'lingonexus-core', '-Dmaven.test.skip=true', 'install')
        Repair-ModuleJarIfNeeded -ModulePath 'lingonexus-core' -ArtifactId 'lingonexus-core'
    }
}

if (-not $SkipUtils) {
    Invoke-Step -Name 'Install lingonexus-utils' -Action {
        Invoke-Maven -Arguments @('-pl', 'lingonexus-utils', '-Dmaven.test.skip=true', 'install')
        Repair-ModuleJarIfNeeded -ModulePath 'lingonexus-utils' -ArtifactId 'lingonexus-utils'
    }
}

if (-not $SkipModules) {
    Invoke-Step -Name 'Install lingonexus-modules' -Action {
        Invoke-Maven -Arguments @('-pl', 'lingonexus-modules', '-Dmaven.test.skip=true', 'install')
        Repair-ModuleJarIfNeeded -ModulePath 'lingonexus-modules' -ArtifactId 'lingonexus-modules'
    }
}

if (-not $SkipScripts) {
    $scriptModules = @(
        'lingonexus-script/lingonexus-script-groovy',
        'lingonexus-script/lingonexus-script-javascript',
        'lingonexus-script/lingonexus-script-javaexpr',
        'lingonexus-script/lingonexus-script-java',
        'lingonexus-script/lingonexus-script-kotlin'
    )

    foreach ($module in $scriptModules) {
        $artifactId = Split-Path $module -Leaf
        Invoke-Step -Name "Install $module" -Action {
            Invoke-Maven -Arguments @('-pl', $module, '-Dmaven.test.skip=true', 'install')
            Repair-ModuleJarIfNeeded -ModulePath $module -ArtifactId $artifactId
        }
    }
}

if (-not $SkipTestcases) {
    Invoke-Step -Name 'Install lingonexus-spring-boot-starter' -Action {
        Invoke-Maven -Arguments @('-pl', 'lingonexus-spring-boot-starter', '-Dmaven.test.skip=true', 'install')
        Repair-ModuleJarIfNeeded -ModulePath 'lingonexus-spring-boot-starter' -ArtifactId 'lingonexus-spring-boot-starter'
    }
}

if (-not $SkipExamples) {
    Invoke-Step -Name 'Compile lingonexus-examples' -Action {
        Invoke-Maven -Arguments @('-pl', 'lingonexus-examples', '-Dmaven.test.skip=true', 'compile')
    }
}

if (-not $SkipTestcases) {
    $profileModules = @(Get-ProfileVerificationModules)
    $needsSpringBootTestcase = ($Profile -eq 'None') -or ($profileModules -contains 'lingonexus-testcase/lingonexus-testcase-springboot')
    $needsNoSpringTestcase = ($Profile -eq 'None') -or ($profileModules -contains 'lingonexus-testcase/lingonexus-testcase-nospring')

    if ($needsSpringBootTestcase) {
        Invoke-Step -Name 'Compile lingonexus-testcase-springboot' -Action {
            Invoke-Maven -Arguments @('-pl', 'lingonexus-testcase/lingonexus-testcase-springboot', '-Dmaven.test.skip=true', 'compile')
        }

        Invoke-Step -Name 'Test-compile lingonexus-testcase-springboot' -Action {
            Invoke-Maven -Arguments @('-pl', 'lingonexus-testcase/lingonexus-testcase-springboot', 'test-compile')
        }
    }

    if ($needsNoSpringTestcase) {
        Invoke-Step -Name 'Test-compile lingonexus-testcase-nospring' -Action {
            Invoke-Maven -Arguments @('-pl', 'lingonexus-testcase/lingonexus-testcase-nospring', 'test-compile')
        }
    }

    Invoke-ProfileVerificationIfNeeded
}

Write-Host 'Verified module-by-module build completed.' -ForegroundColor Green
