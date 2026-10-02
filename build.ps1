$ErrorActionPreference = 'Stop'

Write-Host "WanderBots build helper" -ForegroundColor Cyan

$java = & java -version 2>&1 | Out-String
if ($LASTEXITCODE -ne 0) {
    throw "Java is not installed. Install a 64-bit Java 21 JDK first."
}
if ($java -notmatch 'version "21') {
    Write-Warning "The current java command does not report Java 21. Paper 1.21.11 should be run/built with Java 21."
}

if (Get-Command gradle -ErrorAction SilentlyContinue) {
    gradle build
    exit $LASTEXITCODE
}

throw "Gradle was not found. Install Gradle 9+ and add it to PATH, then run this script again."
