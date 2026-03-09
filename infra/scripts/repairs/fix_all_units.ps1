#!/usr/bin/env pwsh
# fix_all_units.ps1
# Applies systematic fixes to all CloudInfraMap microservice units
# Fixes:
# 1. Test .csproj: Use Microsoft.NET.Sdk.Web, update package versions, add coverlet.collector
# 2. Src .csproj: Remove duplicate common proto compilation (already in Base)
# 3. Service .cs files: Replace 'using CloudInfraMap.Base.Models' with 'using CloudInfraMap.Base.ErrorHandling'
# 4. Fix IStateStore.SetAsync mock signatures (remove extra TimeSpan? parameter)

param(
    [string]$UnitsDir = "$PSScriptRoot\units",
    [switch]$DryRun = $false,
    [string]$Filter = "*"
)

$ErrorActionPreference = "Continue"
$fixedCount = 0
$skippedCount = 0
$errorCount = 0

function Write-Log {
    param([string]$Message, [string]$Level = "INFO")
    $color = switch ($Level) {
        "INFO" { "Cyan" }
        "SUCCESS" { "Green" }
        "WARN" { "Yellow" }
        "ERROR" { "Red" }
        "SKIP" { "Gray" }
        default { "White" }
    }
    Write-Host "[$Level] $Message" -ForegroundColor $color
}

function Fix-TestCsproj {
    param([string]$CsprojPath, [string]$UnitName)
    
    if (-not (Test-Path $CsprojPath)) {
        Write-Log "Test csproj not found: $CsprojPath" "WARN"
        return $false
    }
    
    $content = Get-Content $CsprojPath -Raw
    $changed = $false
    
    # 1. Change SDK from Microsoft.NET.Sdk to Microsoft.NET.Sdk.Web
    if ($content -match 'Sdk="Microsoft\.NET\.Sdk"' -and $content -notmatch 'Sdk="Microsoft\.NET\.Sdk\.Web"') {
        $content = $content -replace 'Sdk="Microsoft\.NET\.Sdk"', 'Sdk="Microsoft.NET.Sdk.Web"'
        $changed = $true
        Write-Log "  Fixed SDK: Microsoft.NET.Sdk -> Microsoft.NET.Sdk.Web" "INFO"
    }
    
    # 2. Remove conflicting explicit package versions that cause NU1605
    $packagesToRemove = @(
        'Microsoft\.Extensions\.Logging',
        'Microsoft\.Extensions\.Logging\.Abstractions',
        'Microsoft\.Extensions\.Configuration',
        'Microsoft\.Extensions\.Configuration\.Abstractions'
    )
    foreach ($pkg in $packagesToRemove) {
        if ($content -match "<PackageReference Include=""$pkg"" Version=""[^""]*"" />") {
            $content = $content -replace "\s*<PackageReference Include=""$pkg"" Version=""[^""]*"" />\r?\n", ""
            $changed = $true
            Write-Log "  Removed conflicting package: $pkg" "INFO"
        }
    }
    
    # 3. Update xunit version (any version < 2.9.2)
    if ($content -match '<PackageReference Include="xunit" Version="2\.[0-8]\.[0-9]"') {
        $content = $content -replace '<PackageReference Include="xunit" Version="[^"]*"', '<PackageReference Include="xunit" Version="2.9.2"'
        $changed = $true
        Write-Log "  Updated xunit to 2.9.2" "INFO"
    }
    
    # 3b. Fix EF Core InMemory version in test projects
    if ($content -match '<PackageReference Include="Microsoft\.EntityFrameworkCore\.InMemory" Version="9\.0\.0"') {
        $content = $content -replace '<PackageReference Include="Microsoft\.EntityFrameworkCore\.InMemory" Version="9\.0\.0"', '<PackageReference Include="Microsoft.EntityFrameworkCore.InMemory" Version="9.0.2"'
        $changed = $true
        Write-Log "  Updated Microsoft.EntityFrameworkCore.InMemory to 9.0.2 in test project" "INFO"
    }
    
    # 4. Update xunit.runner.visualstudio version
    if ($content -match '<PackageReference Include="xunit\.runner\.visualstudio" Version="2\.[0-7]\.[0-9]"') {
        $content = $content -replace '<PackageReference Include="xunit\.runner\.visualstudio" Version="[^"]*"', '<PackageReference Include="xunit.runner.visualstudio" Version="2.8.2"'
        $changed = $true
        Write-Log "  Updated xunit.runner.visualstudio to 2.8.2" "INFO"
    }
    
    # 5. Update Microsoft.NET.Test.Sdk version
    if ($content -match '<PackageReference Include="Microsoft\.NET\.Test\.Sdk" Version="17\.[0-9]\.[0-9]"') {
        $content = $content -replace '<PackageReference Include="Microsoft\.NET\.Test\.Sdk" Version="[^"]*"', '<PackageReference Include="Microsoft.NET.Test.Sdk" Version="17.11.1"'
        $changed = $true
        Write-Log "  Updated Microsoft.NET.Test.Sdk to 17.11.1" "INFO"
    }
    
    # 6. Update Moq version
    if ($content -match '<PackageReference Include="Moq" Version="4\.20\.[0-9]+"') {
        $content = $content -replace '<PackageReference Include="Moq" Version="[^"]*"', '<PackageReference Include="Moq" Version="4.20.72"'
        $changed = $true
        Write-Log "  Updated Moq to 4.20.72" "INFO"
    }
    
    # 6b. Add FluentAssertions if test file uses it but csproj doesn't have it
    $testDir = Split-Path $CsprojPath -Parent
    $hasFluentInCode = Get-ChildItem -Path $testDir -Filter "*.cs" -ErrorAction SilentlyContinue | 
    Where-Object { (Get-Content $_.FullName -Raw -ErrorAction SilentlyContinue) -match 'FluentAssertions' } |
    Select-Object -First 1
    if ($hasFluentInCode -and $content -notmatch 'FluentAssertions') {
        # Add FluentAssertions after Moq
        if ($content -match '<PackageReference Include="Moq"') {
            $content = $content -replace '(<PackageReference Include="Moq"[^/]*/>\s*)', "`$1    <PackageReference Include=""FluentAssertions"" Version=""6.12.0"" />`n    "
        }
        else {
            # Add before closing ItemGroup
            $content = $content -replace '(</ItemGroup>)', "    <PackageReference Include=""FluentAssertions"" Version=""6.12.0"" />`n  `$1"
        }
        $changed = $true
        Write-Log "  Added FluentAssertions 6.12.0" "INFO"
    }
    
    # 7. Add coverlet.collector if missing
    if ($content -notmatch 'coverlet\.collector') {
        # Add before closing ItemGroup that has xunit
        $coverletEntry = @"
    <PackageReference Include="coverlet.collector" Version="6.0.2">
      <PrivateAssets>all</PrivateAssets>
      <IncludeAssets>runtime; build; native; contentfiles; analyzers; buildtransitive</IncludeAssets>
    </PackageReference>
"@
        # Insert after xunit.runner.visualstudio closing tag
        if ($content -match '(?s)(.*xunit\.runner\.visualstudio.*?</PackageReference>)(\s*)(.*?)') {
            $content = $content -replace '((?s).*xunit\.runner\.visualstudio.*?</PackageReference>)', "`$1`n$coverletEntry"
            $changed = $true
            Write-Log "  Added coverlet.collector 6.0.2" "INFO"
        }
    }
    
    # 8. Remove duplicate proto compilation from test projects (if any)
    if ($content -match '<Protobuf Include=".*common.*\.proto"') {
        $content = $content -replace '(?s)\s*<!-- gRPC Test Support -->\s*<ItemGroup>\s*(<Protobuf Include="[^"]*common[^"]*"[^/]*/>\s*)*</ItemGroup>', ''
        $content = $content -replace '(?s)\s*<ItemGroup>\s*(<Protobuf Include="[^"]*common[^"]*"[^/]*/>\s*)+</ItemGroup>', ''
        $changed = $true
        Write-Log "  Removed duplicate common proto compilation from test project" "INFO"
    }
    
    if ($changed -and -not $DryRun) {
        Set-Content -Path $CsprojPath -Value $content -NoNewline
        Write-Log "  Saved: $CsprojPath" "SUCCESS"
    }
    
    return $changed
}

function Fix-SrcCsproj {
    param([string]$CsprojPath, [string]$UnitName)
    
    if (-not (Test-Path $CsprojPath)) {
        Write-Log "Src csproj not found: $CsprojPath" "WARN"
        return $false
    }
    
    $content = Get-Content $CsprojPath -Raw
    $changed = $false
    
    # Remove duplicate common proto compilation (already in Base)
    # Pattern: <Protobuf Include="$(ProtosRoot)\common\*.proto" ... />
    if ($content -match '<Protobuf Include="[^"]*ProtosRoot[^"]*common[^"]*"') {
        # Remove the common proto line
        $content = $content -replace '\s*<Protobuf Include="[^"]*ProtosRoot[^"]*common[^"]*"[^/]*/>\r?\n', ''
        $changed = $true
        Write-Log "  Removed common proto compilation (already in Base)" "INFO"
    }
    
    # Remove config_distributor proto if it's a Client reference (already in Base as Both)
    if ($content -match '<Protobuf Include="[^"]*config_distributor\.proto" GrpcServices="Client"') {
        $content = $content -replace '\s*<Protobuf Include="[^"]*config_distributor\.proto" GrpcServices="Client"[^/]*/>\r?\n', ''
        $changed = $true
        Write-Log "  Removed config_distributor Client proto (already in Base)" "INFO"
    }
    
    # Add AdditionalImportDirs to remaining proto references if not present
    if ($content -match '<Protobuf Include="[^"]*ProtosRoot[^"]*" GrpcServices="Server"' -and
        $content -notmatch 'AdditionalImportDirs') {
        $content = $content -replace '(<Protobuf Include="[^"]*ProtosRoot[^"]*" GrpcServices="Server" ProtoRoot="[^"]*")', '$1 AdditionalImportDirs="$(ProtosRoot)"'
        $changed = $true
        Write-Log "  Added AdditionalImportDirs to Server proto" "INFO"
    }
    
    # Remove ProtobufIncludeDir property (replaced by AdditionalImportDirs)
    if ($content -match '<ProtobufIncludeDir>') {
        $content = $content -replace '\s*<ProtobufIncludeDir>[^<]*</ProtobufIncludeDir>\r?\n', ''
        $changed = $true
        Write-Log "  Removed ProtobufIncludeDir (using AdditionalImportDirs)" "INFO"
    }
    
    # Fix EF Core version downgrade (Base requires 9.0.2)
    if ($content -match '<PackageReference Include="Microsoft\.EntityFrameworkCore" Version="9\.0\.0"') {
        $content = $content -replace '<PackageReference Include="Microsoft\.EntityFrameworkCore" Version="9\.0\.0"', '<PackageReference Include="Microsoft.EntityFrameworkCore" Version="9.0.2"'
        $changed = $true
        Write-Log "  Updated Microsoft.EntityFrameworkCore to 9.0.2" "INFO"
    }
    if ($content -match '<PackageReference Include="Microsoft\.EntityFrameworkCore\.InMemory" Version="9\.0\.0"') {
        $content = $content -replace '<PackageReference Include="Microsoft\.EntityFrameworkCore\.InMemory" Version="9\.0\.0"', '<PackageReference Include="Microsoft.EntityFrameworkCore.InMemory" Version="9.0.2"'
        $changed = $true
        Write-Log "  Updated Microsoft.EntityFrameworkCore.InMemory to 9.0.2" "INFO"
    }
    if ($content -match '<PackageReference Include="Npgsql\.EntityFrameworkCore\.PostgreSQL" Version="9\.0\.0"') {
        $content = $content -replace '<PackageReference Include="Npgsql\.EntityFrameworkCore\.PostgreSQL" Version="9\.0\.0"', '<PackageReference Include="Npgsql.EntityFrameworkCore.PostgreSQL" Version="9.0.4"'
        $changed = $true
        Write-Log "  Updated Npgsql.EntityFrameworkCore.PostgreSQL to 9.0.4" "INFO"
    }
    
    # Clean up empty PropertyGroup blocks
    $content = $content -replace '(?s)\s*<PropertyGroup>\s*<ErrorOnDuplicatePublishOutputFiles>false</ErrorOnDuplicatePublishOutputFiles>\s*</PropertyGroup>\s*\r?\n', ''
    
    if ($changed -and -not $DryRun) {
        Set-Content -Path $CsprojPath -Value $content -NoNewline
        Write-Log "  Saved: $CsprojPath" "SUCCESS"
    }
    
    return $changed
}

function Fix-CSharpFiles {
    param([string]$Directory, [string]$UnitName)
    
    $changed = $false
    $csFiles = Get-ChildItem -Path $Directory -Filter "*.cs" -Recurse
    
    foreach ($file in $csFiles) {
        $content = Get-Content $file.FullName -Raw
        $fileChanged = $false
        
        # Fix 0: Add missing 'using Xunit;' to test files that use [Fact] but don't have the using
        if ($file.FullName -match '\\tests\\' -and 
            $content -match '\[Fact\]' -and 
            $content -notmatch 'using Xunit;') {
            # Add using Xunit; after the last using statement
            $content = $content -replace '(using [^\r\n]+;\r?\n)(?!using )', "`$1using Xunit;`n"
            # Remove duplicate if added multiple times
            $content = $content -replace '(using Xunit;\r?\n)(using Xunit;\r?\n)+', '$1'
            $fileChanged = $true
            Write-Log "  Added 'using Xunit;' to: $($file.Name)" "INFO"
        }
        
        # Fix 1: Replace 'using CloudInfraMap.Base.Models;' with 'using CloudInfraMap.Base.ErrorHandling;'
        # In files that use UnitException factory methods OR new UnitException(ErrorCodes.xxx, ..., StatusCode.xxx)
        # OR access .GrpcStatus property
        if ($content -match 'using CloudInfraMap\.Base\.Models;' -and 
            ($content -match 'UnitException\.(Unauthorized|ValidationFailed|NotFound|Conflict|DependencyFailed|BadRequest)' -or
            $content -match '\.GrpcStatus' -or
            $content -match 'new UnitException\(ErrorCodes\.' -or
            $content -match 'new UnitException\(.*StatusCode\.' -or
            $content -match '\.ErrorCode\s*==' -or
            $content -match 'exception\.ErrorCode')) {
            $content = $content -replace 'using CloudInfraMap\.Base\.Models;', 'using CloudInfraMap.Base.ErrorHandling;'
            $fileChanged = $true
            Write-Log "  Fixed UnitException namespace in: $($file.Name)" "INFO"
        }
        
        # Fix 2: Fix IStateStore.SetAsync mock with extra TimeSpan? parameter (in test files)
        # Handle both single-line and multi-line patterns
        # SAFE approach: only remove the It.IsAny<TimeSpan?>() line from Setup calls
        # Do NOT touch Callback patterns (too complex and risky)
        if ($content -match 'It\.IsAny<TimeSpan\?>\(\)') {
            # Remove the TimeSpan? line from multi-line SetAsync Setup mocks
            # Pattern: "    It.IsAny<TimeSpan?>(),\n"
            $content = $content -replace '[ \t]*It\.IsAny<TimeSpan\?>\(\),[ \t]*\r?\n', ''
            # Also handle single-line: , It.IsAny<TimeSpan?>()
            $content = $content -replace ',\s*It\.IsAny<TimeSpan\?>\(\)', ''
            $fileChanged = $true
            Write-Log "  Fixed IStateStore.SetAsync Setup mock (removed TimeSpan?) in: $($file.Name)" "INFO"
        }
        
        # Fix Callback<..., TimeSpan?, ...> patterns - remove TimeSpan? from generic args
        if ($content -match '\.Callback<[^>]*TimeSpan\?[^>]*>') {
            $content = $content -replace '(\.Callback<[^>]*),\s*TimeSpan\?([^>]*>)', '$1$2'
            $fileChanged = $true
            Write-Log "  Fixed Callback generic args (removed TimeSpan?) in: $($file.Name)" "INFO"
        }
        
        # Fix lambda params in Callback: (key, value, ttl, ct) => -> (key, value, ct) =>
        # Only fix if the lambda has 4 params and one is named ttl/timeSpan/expiry
        if ($content -match '\((\w+),\s*(\w+),\s*(ttl|timeSpan|expiry|ttlParam),\s*(\w+)\)\s*=>') {
            $content = $content -replace '\((\w+),\s*(\w+),\s*(ttl|timeSpan|expiry|ttlParam),\s*(\w+)\)\s*=>', '($1, $2, $4) =>'
            $fileChanged = $true
            Write-Log "  Fixed Callback lambda params (removed TTL param) in: $($file.Name)" "INFO"
        }
        
        # Fix 3: Fix IStateStore.SetAsync production calls with extra TTL parameter
        # Patterns:
        #   _stateStore.SetAsync(key, value, TimeSpan.FromXxx(...), ct)
        #   _stateStore.SetAsync(key, value, null, ct)
        #   _stateStore.SetAsync<T>(key, value, null, ct)
        # Fix: _stateStore.SetAsync(key, value, ct)
        $setAsyncFixed = $false
        # Pattern with TimeSpan
        if ($content -match 'SetAsync[^(]*\([^)]+TimeSpan\.[^)]+\)') {
            $content = $content -replace '(SetAsync[^(]*\([^,]+,\s*[^,]+),\s*TimeSpan\.\w+\([^)]*\),\s*(ct\))', '$1, $2'
            $content = $content -replace '(SetAsync[^(]*\([^,]+,\s*[^,]+),\s*TimeSpan\.\w+\([^)]*\)\)', '$1)'
            $setAsyncFixed = $true
        }
        # Pattern with null as 3rd arg: SetAsync(key, value, null, ct) or SetAsync<T>(key, value, null, ct)
        # Use a line-by-line approach for this one
        $lines = $content -split "`n"
        $newLines = @()
        $lineFixed = $false
        foreach ($line in $lines) {
            if ($line -match 'SetAsync[^(]*\([^,]+,\s*[^,]+,\s*null,\s*ct\)') {
                $line = $line -replace '(SetAsync[^(]*\([^,]+,\s*[^,]+),\s*null,\s*(ct\))', '$1, $2'
                $lineFixed = $true
            }
            $newLines += $line
        }
        if ($lineFixed) {
            $content = $newLines -join "`n"
            $setAsyncFixed = $true
        }
        if ($setAsyncFixed) {
            $fileChanged = $true
            Write-Log "  Fixed SetAsync production call (removed TTL) in: $($file.Name)" "INFO"
        }
        
        # Fix 3a: Fix CloudInfraMap.Base.Services.ISecretStore -> CloudInfraMap.Base.Interfaces.ISecretStore
        if ($content -match 'CloudInfraMap\.Base\.Services\.ISecretStore') {
            $content = $content -replace 'CloudInfraMap\.Base\.Services\.ISecretStore', 'CloudInfraMap.Base.Interfaces.ISecretStore'
            $fileChanged = $true
            Write-Log "  Fixed ISecretStore namespace in: $($file.Name)" "INFO"
        }
        
        # Fix 3b: Fix CloudInfraMap.Units.Base namespace (wrong - should be CloudInfraMap.Base)
        if ($content -match 'CloudInfraMap\.Units\.Base\.') {
            $content = $content -replace 'CloudInfraMap\.Units\.Base\.', 'CloudInfraMap.Base.Services.'
            $fileChanged = $true
            Write-Log "  Fixed CloudInfraMap.Units.Base -> CloudInfraMap.Base.Services in: $($file.Name)" "INFO"
        }
        
        # Fix 4: Fix Program.cs missing ErrorHandlingInterceptor using
        if ($file.Name -eq "Program.cs" -and 
            $content -match 'ErrorHandlingInterceptor' -and 
            $content -notmatch 'using CloudInfraMap\.Base\.ErrorHandling') {
            # Add the using after the last 'using CloudInfraMap.Base' line
            if ($content -match 'using CloudInfraMap\.Base\.Services;') {
                $content = $content -replace '(using CloudInfraMap\.Base\.Services;)', "`$1`nusing CloudInfraMap.Base.ErrorHandling;"
            }
            elseif ($content -match 'using CloudInfraMap\.Base\.') {
                $content = $content -replace '(using CloudInfraMap\.Base\.[^\r\n]+)', "`$1`nusing CloudInfraMap.Base.ErrorHandling;"
            }
            # Remove duplicate if added multiple times
            $content = $content -replace '(using CloudInfraMap\.Base\.ErrorHandling;\r?\n)(using CloudInfraMap\.Base\.ErrorHandling;\r?\n)+', '$1'
            $fileChanged = $true
            Write-Log "  Added ErrorHandling using to Program.cs" "INFO"
        }
        
        if ($fileChanged -and -not $DryRun) {
            Set-Content -Path $file.FullName -Value $content -NoNewline
        }
        
        if ($fileChanged) { $changed = $true }
    }
    
    return $changed
}

# Get all unit directories
$unitDirs = Get-ChildItem -Path $UnitsDir -Directory | Where-Object { $_.Name -like $Filter -and $_.Name -like "unit-*" }

Write-Log "Found $($unitDirs.Count) units to process" "INFO"
Write-Log "DryRun: $DryRun" "INFO"
Write-Log "" "INFO"

foreach ($unitDir in $unitDirs) {
    $unitName = $unitDir.Name
    $srcDir = Join-Path $unitDir.FullName "src"
    $testsDir = Join-Path $unitDir.FullName "tests"
    
    # Find test csproj
    $testCsproj = Get-ChildItem -Path $testsDir -Filter "*.csproj" -ErrorAction SilentlyContinue | Select-Object -First 1
    # Find src csproj
    $srcCsproj = Get-ChildItem -Path $srcDir -Filter "*.csproj" -ErrorAction SilentlyContinue | Select-Object -First 1
    
    if (-not $testCsproj -and -not $srcCsproj) {
        Write-Log "Skipping $unitName (no csproj found)" "SKIP"
        $skippedCount++
        continue
    }
    
    Write-Log "Processing: $unitName" "INFO"
    
    try {
        $anyChange = $false
        
        if ($testCsproj) {
            $changed = Fix-TestCsproj -CsprojPath $testCsproj.FullName -UnitName $unitName
            if ($changed) { $anyChange = $true }
        }
        
        if ($srcCsproj) {
            $changed = Fix-SrcCsproj -CsprojPath $srcCsproj.FullName -UnitName $unitName
            if ($changed) { $anyChange = $true }
        }
        
        # Fix C# files in src and tests
        if (Test-Path $srcDir) {
            $changed = Fix-CSharpFiles -Directory $srcDir -UnitName $unitName
            if ($changed) { $anyChange = $true }
        }
        if (Test-Path $testsDir) {
            $changed = Fix-CSharpFiles -Directory $testsDir -UnitName $unitName
            if ($changed) { $anyChange = $true }
        }
        
        if ($anyChange) {
            $fixedCount++
            Write-Log "  ✅ Fixed: $unitName" "SUCCESS"
        }
        else {
            $skippedCount++
            Write-Log "  ⏭️  No changes needed: $unitName" "SKIP"
        }
    }
    catch {
        $errorCount++
        Write-Log "  ❌ Error processing $unitName`: $_" "ERROR"
    }
    
    Write-Log "" "INFO"
}

Write-Log "=== Summary ===" "INFO"
Write-Log "Fixed: $fixedCount" "SUCCESS"
Write-Log "Skipped (no changes): $skippedCount" "SKIP"
Write-Log "Errors: $errorCount" "ERROR"
