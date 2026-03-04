<#
.SYNOPSIS
    Builds SuperKey (OSS or Pro) and deploys the JAR to your local JMeter installation.

.DESCRIPTION
    Runs 'mvn verify -P <profile>', then copies the resulting JAR to
    $JMeterHome\lib\ext, removing any previous SuperKey JARs first.

.PARAMETER Profile
    Build profile: 'oss' or 'pro'. Defaults to 'oss'.

.PARAMETER JMeterHome
    Path to your JMeter installation. Defaults to the configured default below.

.EXAMPLE
    .\scripts\build-and-deploy.ps1              # builds OSS, deploys
    .\scripts\build-and-deploy.ps1 -Profile pro # builds Pro, deploys
    .\scripts\build-and-deploy.ps1 -Profile pro -JMeterHome "C:\tools\jmeter"
#>
param(
    [Parameter(Mandatory = $false)]
    [ValidateSet("oss", "pro")]
    [string]$Profile = "oss",

    [Parameter(Mandatory = $false)]
    [string]$JMeterHome = "C:\Users\Navee\tools\apache-jmeter-5.6.3"
)

# ── Colours ───────────────────────────────────────────────────────────────────
function Write-Step { param($m) Write-Host "`n▶  $m" -ForegroundColor Cyan }
function Write-Ok   { param($m) Write-Host "   ✅ $m" -ForegroundColor Green }
function Write-Fail { param($m) Write-Host "`n   🚨 $m" -ForegroundColor Red }

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot  = Split-Path -Parent $ScriptDir
Set-Location $RepoRoot

$ExtDir = Join-Path $JMeterHome "lib\ext"

# ── 1. Validate JMeter home ───────────────────────────────────────────────────
Write-Step "Validating JMeter installation"
if (-not (Test-Path $ExtDir)) {
    Write-Fail "JMeter lib\ext not found at: $ExtDir"
    Write-Host "   Pass the correct path: -JMeterHome 'C:\path\to\jmeter'" -ForegroundColor Yellow
    exit 1
}
Write-Ok "JMeter lib\ext: $ExtDir"

# ── 2. Build ──────────────────────────────────────────────────────────────────
Write-Step "Building SuperKey [$Profile profile] — mvn verify -P $Profile"
mvn verify -P $Profile
if ($LASTEXITCODE -ne 0) {
    Write-Fail "Build FAILED. Fix the errors above."
    exit 1
}
Write-Ok "Build succeeded"

# ── 3. Find the built JAR ─────────────────────────────────────────────────────
Write-Step "Locating built JAR"
$TargetJar = Get-ChildItem -Path "target" -Filter "*-${Profile}.jar" | Select-Object -First 1
if (-not $TargetJar) {
    Write-Fail "Could not find *-${Profile}.jar in target/"
    exit 1
}
Write-Ok "JAR: $($TargetJar.FullName)"
Write-Ok "Size: $([math]::Round($TargetJar.Length / 1KB, 1)) KB"

# ── 4. Remove old SuperKey JARs from lib\ext ─────────────────────────────────
Write-Step "Removing old SuperKey JARs from JMeter lib\ext"
$OldJars = Get-ChildItem -Path $ExtDir -Filter "superkey-*.jar"
if ($OldJars) {
    foreach ($old in $OldJars) {
        Remove-Item $old.FullName -Force
        Write-Host "   Removed: $($old.Name)" -ForegroundColor Gray
    }
} else {
    Write-Host "   No existing SuperKey JARs found — clean install." -ForegroundColor Gray
}

# ── 5. Copy new JAR ───────────────────────────────────────────────────────────
Write-Step "Deploying JAR to JMeter"
Copy-Item -Path $TargetJar.FullName -Destination $ExtDir -Force
Write-Ok "Deployed: $($TargetJar.Name) → $ExtDir"

# ── Done ──────────────────────────────────────────────────────────────────────
$emoji = if ($Profile -eq "pro") { "🔒" } else { "🌐" }
Write-Host ""
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Green
Write-Host "  ✅  SuperKey [$Profile] deployed successfully! $emoji" -ForegroundColor Green
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Green
Write-Host ""
Write-Host "  JAR     : $($TargetJar.Name)" -ForegroundColor Gray
Write-Host "  JMeter  : $ExtDir" -ForegroundColor Gray
Write-Host ""
Write-Host "  ➡  Restart JMeter to load the new plugin." -ForegroundColor White
Write-Host ""
