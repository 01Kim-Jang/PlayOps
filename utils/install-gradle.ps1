param(
    [string]$Version = "8.10.2",
    [string]$InstallRoot = (Join-Path $PSScriptRoot "bin"),
    [int]$RetryCount = 3
)

$ErrorActionPreference = "Stop"

$distributionName = "gradle-$Version"
$downloadUrls = @(
    "https://services.gradle.org/distributions/$distributionName-bin.zip",
    "https://downloads.gradle.org/distributions/$distributionName-bin.zip"
)
$zipPath = Join-Path $InstallRoot "$distributionName-bin.zip"
$installPath = Join-Path $InstallRoot $distributionName
$gradleBat = Join-Path $installPath "bin\gradle.bat"

New-Item -ItemType Directory -Force -Path $InstallRoot | Out-Null

if (!(Test-Path $gradleBat)) {
    $downloaded = $false
    foreach ($downloadUrl in $downloadUrls) {
        for ($attempt = 1; $attempt -le $RetryCount; $attempt++) {
            try {
                if (Test-Path $zipPath) {
                    Remove-Item -Path $zipPath -Force
                }

                Write-Host "Downloading $distributionName from $downloadUrl (attempt $attempt/$RetryCount)"
                Invoke-WebRequest -Uri $downloadUrl -OutFile $zipPath
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
        throw "Failed to download $distributionName from all configured URLs."
    }

    Write-Host "Extracting $zipPath"
    Expand-Archive -Path $zipPath -DestinationPath $InstallRoot -Force
    Remove-Item -Path $zipPath -Force
} else {
    Write-Host "$distributionName is already installed."
}

if (!(Test-Path $gradleBat)) {
    throw "Gradle executable was not found at $gradleBat"
}

$gradleBin = Join-Path $installPath "bin"
$env:PATH = "$gradleBin;$env:PATH"

Write-Host "Gradle installed: $gradleBat"
Write-Host "For this PowerShell session:"
Write-Host "`$env:PATH = `"$gradleBin;`$env:PATH`""
& $gradleBat --version
