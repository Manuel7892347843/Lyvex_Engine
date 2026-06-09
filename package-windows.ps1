$ErrorActionPreference = "Stop"

Write-Host "=== LyvexEngine Windows Package Builder ===" -ForegroundColor Cyan
Write-Host ""

$projectDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$outputRoot = Join-Path $projectDir "release"
$libsDir = Join-Path $projectDir "build\libs"
$gradlew = Join-Path $projectDir "gradlew.bat"

Write-Host "Project directory: $projectDir"
Write-Host ""

$version = Read-Host "Inserisci la versione del pacchetto, esempio 1.0.0"
$version = $version.Trim()

if ([string]::IsNullOrWhiteSpace($version)) {
    Write-Host "ERRORE: versione vuota." -ForegroundColor Red
    pause
    exit 1
}

$invalidChars = [System.IO.Path]::GetInvalidFileNameChars()
foreach ($char in $invalidChars) {
    if ($version.Contains($char)) {
        Write-Host "ERRORE: la versione contiene caratteri non validi per un nome file." -ForegroundColor Red
        pause
        exit 1
    }
}

$packageName = "LyvexEngine-$version"
$outputDir = Join-Path $outputRoot $packageName
$zipPath = Join-Path $outputRoot "$packageName.zip"

$defaultJavaHome = ""

if ($env:JAVA21_HOME) {
    $defaultJavaHome = $env:JAVA21_HOME
    Write-Host "JAVA21_HOME rilevato:"
    Write-Host $defaultJavaHome -ForegroundColor Yellow
    Write-Host ""
}

if ($defaultJavaHome -ne "") {
    $inputJavaHome = Read-Host "Inserisci il percorso della JDK 21 oppure premi INVIO per usare quello rilevato"
    if ([string]::IsNullOrWhiteSpace($inputJavaHome)) {
        $jdkPath = $defaultJavaHome
    } else {
        $jdkPath = $inputJavaHome
    }
} else {
    Write-Host "Inserisci il percorso della JDK 21."
    Write-Host "Esempio: la cartella che contiene bin\java.exe"
    Write-Host ""
    $jdkPath = Read-Host "Percorso JDK 21"
}

$jdkPath = $jdkPath.Trim('"').Trim()

if ([string]::IsNullOrWhiteSpace($jdkPath)) {
    Write-Host "ERRORE: percorso JDK 21 vuoto." -ForegroundColor Red
    pause
    exit 1
}

if (-not (Test-Path $jdkPath)) {
    Write-Host "ERRORE: percorso JDK 21 non trovato:" -ForegroundColor Red
    Write-Host $jdkPath
    pause
    exit 1
}

$javaExe = Join-Path $jdkPath "bin\java.exe"

if (-not (Test-Path $javaExe)) {
    Write-Host "ERRORE: java.exe non trovato." -ForegroundColor Red
    Write-Host "Lo script ha cercato qui:"
    Write-Host $javaExe
    Write-Host ""
    Write-Host "Devi indicare la cartella principale della JDK, non la cartella bin."
    pause
    exit 1
}

if (-not (Test-Path $gradlew)) {
    Write-Host "ERRORE: gradlew.bat non trovato:" -ForegroundColor Red
    Write-Host $gradlew
    pause
    exit 1
}

Write-Host ""
Write-Host "Controllo versione Java..." -ForegroundColor Cyan
& $javaExe -version
Write-Host ""

Write-Host "Configuro Java 21 per questa build..." -ForegroundColor Cyan
$env:JAVA_HOME = $jdkPath
$env:Path = (Join-Path $jdkPath "bin") + ";" + $env:Path

Write-Host ""
Write-Host "Genero jar, shadow jar e API jar con Gradle..." -ForegroundColor Cyan

Push-Location $projectDir
try {
    & $gradlew clean jar shadowJar lyvexApiJar
} finally {
    Pop-Location
}

if (-not (Test-Path $libsDir)) {
    Write-Host "ERRORE: cartella build\libs non trovata dopo la build." -ForegroundColor Red
    pause
    exit 1
}

$jar = Get-ChildItem $libsDir -Filter "*-all.jar" | Select-Object -First 1

if ($null -eq $jar) {
    Write-Host "ERRORE: fat jar non trovato in build\libs dopo shadowJar." -ForegroundColor Red
    pause
    exit 1
}

$apiSourceJarPath = Join-Path $libsDir "lyvex-api.jar"

if (-not (Test-Path $apiSourceJarPath)) {
    Write-Host "ERRORE: lyvex-api.jar non trovato dopo lyvexApiJar." -ForegroundColor Red
    pause
    exit 1
}

Write-Host ""
Write-Host "Fat jar trovato:"
Write-Host $jar.FullName -ForegroundColor Green
Write-Host ""

Write-Host "API jar trovato:"
Write-Host $apiSourceJarPath -ForegroundColor Green
Write-Host ""

if (-not (Test-Path $outputRoot)) {
    New-Item -ItemType Directory -Path $outputRoot -Force | Out-Null
}

if (Test-Path $outputDir) {
    Write-Host "Rimuovo vecchia cartella pacchetto..."
    Remove-Item $outputDir -Recurse -Force
}

if (Test-Path $zipPath) {
    Write-Host "Rimuovo vecchio zip..."
    Remove-Item $zipPath -Force
}

Write-Host "Creo cartella di output..."
New-Item -ItemType Directory -Path $outputDir -Force | Out-Null

Write-Host "Copio fat jar..."
Copy-Item $jar.FullName (Join-Path $outputDir $jar.Name)

Write-Host "Copio lyvex-api.jar..."
Copy-Item $apiSourceJarPath (Join-Path $outputDir "lyvex-api.jar") -Force

$imguiFile = Join-Path $projectDir "imgui.ini"
if (Test-Path $imguiFile) {
    Write-Host "Copio imgui.ini..."
    Copy-Item $imguiFile $outputDir
} else {
    Write-Host "imgui.ini non trovato, continuo comunque." -ForegroundColor Yellow
}

$assetsDir = Join-Path $projectDir "Assets"
if (Test-Path $assetsDir) {
    Write-Host "Copio cartella Assets..."
    Copy-Item $assetsDir (Join-Path $outputDir "Assets") -Recurse
} else {
    Write-Host "Cartella Assets non trovata, continuo comunque." -ForegroundColor Yellow
}

Write-Host "Copio runtime Java 21..."
Copy-Item $jdkPath (Join-Path $outputDir "runtime") -Recurse

Write-Host "Creo Avvia.bat..."

$batContent = @"
@echo off
title LyvexEngine
cd /d "%~dp0"

if not exist "%~dp0runtime\bin\java.exe" (
    echo ERRORE: runtime\bin\java.exe non trovato.
    echo Controlla che la cartella runtime sia presente accanto ad Avvia.bat.
    pause
    exit /b 1
)

if not exist "%~dp0$($jar.Name)" (
    echo ERRORE: $($jar.Name) non trovato.
    echo Controlla che il file .jar sia presente accanto ad Avvia.bat.
    pause
    exit /b 1
)

"%~dp0runtime\bin\java.exe" -jar "%~dp0$($jar.Name)"

pause
"@

Set-Content -Path (Join-Path $outputDir "Avvia.bat") -Value $batContent -Encoding ASCII

Write-Host "Creo file zip..."
Compress-Archive -Path $outputDir -DestinationPath $zipPath -Force

Write-Host ""
Write-Host "Pacchetto creato con successo!" -ForegroundColor Green
Write-Host ""
Write-Host "Cartella creata:"
Write-Host $outputDir
Write-Host ""
Write-Host "Zip creato:"
Write-Host $zipPath
Write-Host ""
Write-Host "Sul secondo PC estrai lo zip e avvia Avvia.bat."
Write-Host ""
Write-Host "Nota: lyvex-api.jar e' incluso nella release e puo' essere copiato nei progetti in Libs\lyvex-api.jar."
Write-Host ""

pause