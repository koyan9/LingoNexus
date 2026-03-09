function Get-BuildModes {
    return @{
        Selective = @{
            Description = 'Respects explicit -Skip* flags and acts as the base mode for focused profiles.'
            Overrides = @{}
        }
        Core = @{
            Description = 'Installs only the root POM plus lingonexus-api and lingonexus-core for the fastest runtime iteration loop.'
            Overrides = @{
                SkipScripts = $true
                SkipUtils = $true
                SkipModules = $true
                SkipExamples = $true
                SkipTestcases = $true
            }
        }
        Quick = @{
            Description = 'Builds core plus downstream dependencies but skips examples and testcase compilation for a faster sanity pass.'
            Overrides = @{
                SkipExamples = $true
                SkipTestcases = $true
            }
        }
        Full = @{
            Description = 'Runs the full module-by-module build flow, including testcase compilation.'
            Overrides = @{}
        }
    }
}
