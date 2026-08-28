param(
    [string]$InstallRoot = (Join-Path $PSScriptRoot "bin"),
    [int]$RetryCount = 3
)

$ErrorActionPreference = "Stop"

$downloadUrls = @(
    "https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse?project=jdk",
    "https://aka.ms/download-jdk/microsoft-jdk-21-windows-x64.zip"
)
$zipPath = Join-Path $InstallRoot "temurin-jdk-21.zip"
$extractPath = Join-Path $InstallRoot "_java21-extract"
$installPath = Join-Path $InstallRoot "jdk-21"
$javaExe = Join-Path $installPath "bin\java.exe"

function Assert-UnderInstallRoot {
    param([string]$Path)

    $root = [System.IO.Path]::GetFullPath($InstallRoot)
    $target = [System.IO.Path]::GetFullPath($Path)
    if (!$target.StartsWith($root, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to modify path outside install root: $target"
    }
}

New-Item -ItemType Directory -Force -Path $InstallRoot | Out-Null

if (!(Test-Path $javaExe)) {
    $downloaded = $false
    foreach ($downloadUrl in $downloadUrls) {
        for ($attempt = 1; $attempt -le $RetryCount; $attempt++) {
            try {
                if (Test-Path $zipPath) {
                    Remove-Item -Path $zipPath -Force
                }

                Write-Host "Downloading JDK 21 from $downloadUrl (attempt $attempt/$RetryCount)"
                if (Get-Command curl.exe -ErrorAction SilentlyContinue) {
                    & curl.exe -L --fail --retry 2 --retry-delay 3 -o $zipPath $downloadUrl
                    if ($LASTEXITCODE -ne 0) {
                        throw "curl.exe exited with code $LASTEXITCODE"
                    }
                } else {
                    Invoke-WebRequest -Uri $downloadUrl -OutFile $zipPath
                }
                $downloaded = $true
                break
            } catch {
                if ($attempt -eq $RetryCount) {
                    Write-Warning "Download failed from ${downloadUrl}: $($_.Exception.Message)"
                } else {
                    Write-Warning "Download failed: $($_.Exception.Message)"
                    Start-Sleep -Seconds (3 * $attempt)
                }
            }
        }

        if ($downloaded) {
            break
        }
    }

    if (!$downloaded) {
        throw "Failed to download JDK 21 from all configured URLs."
    }

    Assert-UnderInstallRoot $extractPath
    Assert-UnderInstallRoot $installPath
    if (Test-Path $extractPath) {
        Remove-Item -Path $extractPath -Recurse -Force
    }

    Write-Host "Extracting $zipPath"
    Expand-Archive -Path $zipPath -DestinationPath $extractPath -Force
    Remove-Item -Path $zipPath -Force

    $jdkDirectory = Get-ChildItem -Path $extractPath -Directory | Select-Object -First 1
    if ($null -eq $jdkDirectory) {
        throw "Could not find extracted JDK directory."
    }

    if (Test-Path $installPath) {
        Remove-Item -Path $installPath -Recurse -Force
    }
    Move-Item -Path $jdkDirectory.FullName -Destination $installPath
    Remove-Item -Path $extractPath -Recurse -Force
} else {
    Write-Host "Temurin JDK 21 is already installed."
}

if (!(Test-Path $javaExe)) {
    throw "Java executable was not found at $javaExe"
}

$env:JAVA_HOME = $installPath
$env:PATH = "$installPath\bin;$env:PATH"

Write-Host "JDK installed: $installPath"
Write-Host "For this PowerShell session:"
Write-Host "`$env:JAVA_HOME = `"$installPath`""
Write-Host "`$env:PATH = `"$installPath\bin;`$env:PATH`""
& $javaExe -version
