# Build Troubleshooting

> Updated: 2026-03-09  
> Doc navigation: `docs/INDEX.md`  
> Architecture baseline: `docs/architecture.md`  
> Quick integration guide: `docs/quick-start.md`  
> Runtime diagnostics: `docs/diagnostics.md`


## Current Environment-Specific Issue

In the current Windows environment, multi-module reactor compilation may fail when downstream modules compile directly against upstream reactor output directories.

Observed symptom:

- `lingonexus-api` compiles successfully
- `lingonexus-core` fails during reactor builds with many `package ... does not exist` errors
- compiling `lingonexus-core` alone after installing `lingonexus-api` to the local Maven repository succeeds

This suggests an environment-specific `javac` / reactor classpath behavior rather than a simple source-level dependency declaration problem.

## Verified Workaround

Install upstream modules into the local Maven repository first, then compile downstream modules separately.

## Scripted Workflow

You can run the verified module-by-module build flow with:

```powershell
powershell.exe -ExecutionPolicy Bypass -File scripts/verified-build.ps1
```

If Chinese text looks garbled in Windows PowerShell, first try `chcp 65001`, or read the file explicitly as UTF-8 with `Get-Content -Encoding utf8 README.zh-CN.md`.

```powershell
chcp 65001
Get-Content -Encoding utf8 README.zh-CN.md
```

For day-to-day use, the script now supports four modes:

- `-Mode Core`: installs only the root POM plus `lingonexus-api` and `lingonexus-core`; best for core/runtime iteration.
- `-Mode Selective`: default behavior; respects the explicit `-Skip*` flags you pass.
- `-Mode Quick`: skips `examples` and `testcases` for a faster local verification loop.
- `-Mode Full`: runs the full module-by-module verification flow.

For `Selective` mode, the script also supports optional focused profiles:

- `-Profile ExternalProcess`: runs the external-process compatibility / worker-pool / negotiation verification subset.
- `-Profile Diagnostics`: runs diagnostics / metadata / statistics verification subset.
- `-Profile SpringBoot`: runs the Spring Boot starter / configuration / modules / async / batch verification subset.
- `-Profile Performance`: runs the No-Spring baseline/performance subset plus Spring Boot performance and stress tests.

When a profile is selected, the script automatically enables the required script/modules/testcase steps and skips `examples` by default so the run stays focused.

The mode presets live in `scripts/build-modes.ps1`, and the focused profile definitions live in `scripts/build-profiles.ps1`, so adding or adjusting either one no longer requires editing the main build flow.

For a generated static reference page, see `scripts/BUILD_PRESETS.md`. Regenerate it with `powershell.exe -ExecutionPolicy Bypass -File scripts/generate-build-presets-doc.ps1`.

Recommended commands:

```powershell
# Fastest loop when only `lingonexus-api` / `lingonexus-core` changed
powershell.exe -ExecutionPolicy Bypass -File scripts/verified-build.ps1 -Mode Core

# Fast local verification while iterating on core/runtime work
powershell.exe -ExecutionPolicy Bypass -File scripts/verified-build.ps1 -Mode Quick

# Focused validation for external-process changes
powershell.exe -ExecutionPolicy Bypass -File scripts/verified-build.ps1 -Mode Selective -Profile ExternalProcess -UseDedicatedRepo

# Focused validation for diagnostics / metadata changes
powershell.exe -ExecutionPolicy Bypass -File scripts/verified-build.ps1 -Mode Selective -Profile Diagnostics -UseDedicatedRepo

# Focused validation for Spring Boot starter changes
powershell.exe -ExecutionPolicy Bypass -File scripts/verified-build.ps1 -Mode Selective -Profile SpringBoot -UseDedicatedRepo

# Focused validation for baseline / performance / stress changes
powershell.exe -ExecutionPolicy Bypass -File scripts/verified-build.ps1 -Mode Selective -Profile Performance -UseDedicatedRepo

# Full validation in an isolated local repository
powershell.exe -ExecutionPolicy Bypass -File scripts/verified-build.ps1 -Mode Full -UseDedicatedRepo
```

Discovery commands:

```powershell
# Show the built-in PowerShell help page
Get-Help .\scripts\verified-build.ps1 -Detailed

# Refresh the generated preset reference without changing the build steps you selected
powershell.exe -ExecutionPolicy Bypass -File scripts/verified-build.ps1 -ListProfiles -RefreshDocs

# Refresh the generated preset reference and exit immediately
powershell.exe -ExecutionPolicy Bypass -File scripts/verified-build.ps1 -RefreshDocsOnly

# List all available modes
powershell.exe -ExecutionPolicy Bypass -File scripts/verified-build.ps1 -ListModes

# List all available focused profiles
powershell.exe -ExecutionPolicy Bypass -File scripts/verified-build.ps1 -ListProfiles

# Show one mode in detail
powershell.exe -ExecutionPolicy Bypass -File scripts/verified-build.ps1 -ShowMode Quick

# Show one profile in detail
powershell.exe -ExecutionPolicy Bypass -File scripts/verified-build.ps1 -ShowProfile Performance
```

The script uses a project-local Maven repository at `.verified-m2-repo` to avoid intermittent access issues with the machine-level local repository.

When a dedicated repository path is used, the script now also installs the root/parent POMs (`lingonexus`, `lingonexus-script`, `lingonexus-testcase`) and the downstream dependency modules that testcase compilation needs, so the isolated repository can be bootstrapped from an empty state.

The script also supports repairing incomplete jars on this machine by repacking them directly from `target/classes` when the jar contains fewer `.class` files than the compiled output directory.

Optional flags:

- `-SkipScripts`
- `-SkipUtils`
- `-SkipModules`
- `-SkipExamples`
- `-SkipTestcases`
- `-ReinstallUpstream`
- `-RepairInstalledJars`

When `lingonexus-api` or `lingonexus-core` has changed locally, prefer:

```powershell
powershell.exe -ExecutionPolicy Bypass -File scripts/verified-build.ps1 -ReinstallUpstream
```

If you changed `scripts/build-modes.ps1` or `scripts/build-profiles.ps1`, refresh the generated reference page with:

```powershell
powershell.exe -ExecutionPolicy Bypass -File scripts/verified-build.ps1 -ListProfiles -RefreshDocs

# Or use the dedicated refresh-only shortcut
powershell.exe -ExecutionPolicy Bypass -File scripts/verified-build.ps1 -RefreshDocsOnly
```

### Step 1: Install `lingonexus-api`

```bash
mvn --% -pl lingonexus-api -Dmaven.test.skip=true install
```

### Step 2: Install `lingonexus-core`

```bash
mvn --% -pl lingonexus-core -Dmaven.test.skip=true install
```

### Step 3: Compile downstream modules

```bash
mvn --% -pl lingonexus-examples -Dmaven.test.skip=true compile
```

### Verified Single-Module Compiles

After installing upstream artifacts, the following module compiles have been verified individually:

```bash
mvn --% -pl lingonexus-script/lingonexus-script-groovy -Dmaven.test.skip=true compile
mvn --% -pl lingonexus-script/lingonexus-script-javascript -Dmaven.test.skip=true compile
mvn --% -pl lingonexus-script/lingonexus-script-javaexpr -Dmaven.test.skip=true compile
mvn --% -pl lingonexus-script/lingonexus-script-java -Dmaven.test.skip=true compile
mvn --% -pl lingonexus-script/lingonexus-script-kotlin -Dmaven.test.skip=true compile
mvn --% -pl lingonexus-utils -Dmaven.test.skip=true compile
mvn --% -pl lingonexus-modules -Dmaven.test.skip=true compile
mvn --% -pl lingonexus-examples -Dmaven.test.skip=true compile
mvn --% -pl lingonexus-testcase/lingonexus-testcase-springboot -Dmaven.test.skip=true compile
mvn --% -pl lingonexus-testcase/lingonexus-testcase-springboot test-compile
mvn --% -pl lingonexus-testcase/lingonexus-testcase-nospring test-compile
```

## What Has Been Verified

- `lingonexus-api` installs successfully
- `lingonexus-core` compiles and installs successfully when built alone
- `lingonexus-examples` compiles successfully when built alone after upstream installation
- script language modules compile successfully when built alone after upstream installation
- `lingonexus-utils` and `lingonexus-modules` compile successfully when built alone
- `lingonexus-spring-boot-starter` compiles successfully when built alone
- `lingonexus-testcase-springboot` and `lingonexus-testcase-nospring` test sources compile successfully when built alone

## Additional Workaround For Incomplete Jars

When this environment produces a jar that is missing classes present in `target/classes`, run:

```powershell
powershell.exe -ExecutionPolicy Bypass -File scripts/verified-build.ps1 -ReinstallUpstream -RepairInstalledJars
```

The script compares the number of compiled classes in `target/classes` with the number of class entries in the generated jar. When the jar is incomplete, it repacks the jar from `target/classes` and syncs the repaired jar back into the active local Maven repositories.

## Recommendation

Until the reactor-classpath issue is fully resolved, prefer:

- installing upstream modules first
- compiling downstream modules independently
- using focused module-by-module verification instead of one-shot full reactor validation
- using `scripts/verified-build.ps1` as the default local verification path on Windows
- isolating build artifacts in `.verified-m2-repo` for more stable local verification
