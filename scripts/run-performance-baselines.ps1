param(
    [string]$ReportPath,
    [string]$Tests = 'ExternalProcessPerformanceBaselineTest,IsolationModeComparisonBaselineTest,LargeContextPerformanceBaselineTest,LargeContextIsolationModeComparisonBaselineTest,JavaJaninoCacheIdentityPerformanceBaselineTest',
    [switch]$UseDedicatedRepo,
    [string]$MavenRepoPath,
    [switch]$SkipVerifiedBuild
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $false

$root = Get-Location
$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$reportDir = Join-Path $root 'docs\performance-reports'
New-Item -ItemType Directory -Force -Path $reportDir | Out-Null
if (-not $ReportPath) {
    $ReportPath = Join-Path $reportDir ("performance-report-$timestamp.md")
}
$logPath = [System.IO.Path]::ChangeExtension($ReportPath, '.log')

if ($UseDedicatedRepo -and [string]::IsNullOrWhiteSpace($MavenRepoPath)) {
    $MavenRepoPath = Join-Path $root '.verified-m2-repo'
}

if (-not $SkipVerifiedBuild) {
    $buildArgs = @('-ExecutionPolicy', 'Bypass', '-File', 'scripts\verified-build.ps1', '-SkipExamples')
    if ($UseDedicatedRepo) {
        $buildArgs += '-UseDedicatedRepo'
        if ($MavenRepoPath) {
            $buildArgs += '-MavenRepoPath'
            $buildArgs += $MavenRepoPath
        }
    }
    & powershell.exe @buildArgs
    if ($LASTEXITCODE -ne 0) {
        throw "verified-build.ps1 failed with exit code $LASTEXITCODE"
    }
}

$command = @('mvn')
if ($MavenRepoPath) {
    $command += "-Dmaven.repo.local=$MavenRepoPath"
}
$command += @(
    '-q',
    '-pl', 'lingonexus-testcase/lingonexus-testcase-nospring',
    '-am',
    '-Dsurefire.failIfNoSpecifiedTests=false',
    "-Dtest=$Tests",
    'test'
)
$cmdLine = ($command -join ' ')

$output = & cmd.exe /c "$cmdLine 2>&1"
$output | Tee-Object -FilePath $logPath | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw "Benchmark command failed with exit code $LASTEXITCODE"
}

$benchmarkLines = @($output | Where-Object { $_ -like 'BENCHMARK|*' })
$benchmarks = @()
foreach ($line in $benchmarkLines) {
    $parts = $line -split '\|'
    if ($parts.Length -lt 2) {
        continue
    }
    $item = [ordered]@{ name = $parts[1] }
    for ($i = 2; $i -lt $parts.Length; $i++) {
        $segment = $parts[$i]
        $eq = $segment.IndexOf('=')
        if ($eq -gt 0) {
            $key = $segment.Substring(0, $eq)
            $value = $segment.Substring($eq + 1)
            $item[$key] = $value
        }
    }
    $benchmarks += [pscustomobject]$item
}

$javaVersion = (& cmd.exe /c "java -version 2>&1" | Select-Object -First 1)
$mavenVersion = (& cmd.exe /c "mvn -q -v 2>&1" | Select-Object -First 1)
$cpu = (Get-CimInstance Win32_Processor | Select-Object -First 1 -ExpandProperty Name)
$memory = [Math]::Round((Get-CimInstance Win32_ComputerSystem).TotalPhysicalMemory / 1GB, 2)
$verificationMode = 'direct test run'
if (-not $SkipVerifiedBuild) {
    $verificationMode = 'scripts/verified-build.ps1 + test run'
}
$repoMode = 'default'
if ($MavenRepoPath) {
    $repoMode = $MavenRepoPath
}

$lines = @()
$lines += '# Performance Report'
$lines += ''
$lines += "> Generated: $((Get-Date).ToString('yyyy-MM-dd HH:mm:ss'))"
$lines += ''
$lines += '## Environment'
$lines += ''
$lines += "- Date: $((Get-Date).ToString('yyyy-MM-dd'))"
$lines += "- OS: $([System.Environment]::OSVersion.VersionString)"
$lines += "- Java version: $javaVersion"
$lines += "- Maven version: $mavenVersion"
$lines += "- CPU: $cpu"
$lines += "- Memory (GB): $memory"
$lines += ''
$lines += '## Build Mode'
$lines += ''
$lines += "- Verification path used: $verificationMode"
$lines += "- Maven repo path: $repoMode"
$lines += ''
$lines += '## Benchmark Scope'
$lines += ''
$lines += "- Tests: $Tests"
$lines += ''
$lines += '## Structured Results'
$lines += ''
if ($benchmarks.Count -eq 0) {
    $lines += '- No BENCHMARK lines were captured.'
} else {
    foreach ($benchmark in $benchmarks) {
        $lines += ("### " + $benchmark.name)
        $lines += ''
        foreach ($prop in $benchmark.PSObject.Properties) {
            if ($prop.Name -eq 'name') {
                continue
            }
            $lines += ("- " + $prop.Name + ': ' + $prop.Value)
        }
        $lines += ''
    }
}
$lines += '## Command'
$lines += ''
$lines += ("- Command: " + $cmdLine)
$lines += ''
$lines += '## Raw Log'
$lines += ''
$lines += ("- " + [System.IO.Path]::GetFileName($logPath))

[System.IO.File]::WriteAllText($ReportPath, ($lines -join "`n") + "`n", [System.Text.UTF8Encoding]::new($false))
$latestReportPath = Join-Path $reportDir "latest-performance-report.md"
$latestLogPath = Join-Path $reportDir "latest-performance-report.log"
Copy-Item -Force $ReportPath $latestReportPath
Copy-Item -Force $logPath $latestLogPath
Write-Host "Generated report: $ReportPath" -ForegroundColor Green
Write-Host "Saved raw log: $logPath" -ForegroundColor Green
Write-Host "Updated latest report: $latestReportPath" -ForegroundColor Green
Write-Host "Updated latest log: $latestLogPath" -ForegroundColor Green
