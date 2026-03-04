<#
.SYNOPSIS
    Commits and pushes OSS changes to both the public (origin) and private (pro) repos.

.PARAMETER Message
    The commit message (required).

.PARAMETER All
    If set, stages ALL modified/untracked files before committing (git add .).
    Default: only stages already-tracked modified files (git add -u).

.EXAMPLE
    .\scripts\push-oss.ps1 -Message "feat(oss): add shortcut aliases"
    .\scripts\push-oss.ps1 -Message "fix(oss): resolve NPE" -All
#>
param(
    [Parameter(Mandatory = $true)]
    [string]$Message,

    [Parameter(Mandatory = $false)]
    [switch]$All
)

function Write-Step { param($m) Write-Host "`n▶  $m" -ForegroundColor Cyan }
function Write-Ok   { param($m) Write-Host "   ✅ $m" -ForegroundColor Green }
function Write-Fail { param($m) Write-Host "`n   🚨 $m" -ForegroundColor Red }

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot  = Split-Path -Parent $ScriptDir
Set-Location $RepoRoot

# ── 1. Verify pro remote ──────────────────────────────────────────────────────
Write-Step "Verifying remotes"
$remotes = git remote
if ($remotes -notcontains "pro") {
    Write-Fail "'pro' remote not found. Add it: git remote add pro https://github.com/QAInsights/superkey-pro.git"
    exit 1
}
Write-Ok "origin → $(git remote get-url origin)"
Write-Ok "pro    → $(git remote get-url pro)"

# ── 2. Build OSS (safety check) ───────────────────────────────────────────────
Write-Step "Verifying OSS build (mvn verify -P oss)"
mvn verify -P oss -q
if ($LASTEXITCODE -ne 0) {
    Write-Fail "OSS build FAILED. Fix errors before pushing."
    exit 1
}
Write-Ok "OSS build passed"

# ── 3. Stage and commit ───────────────────────────────────────────────────────
Write-Step "Staging changes"
if ($All) {
    git add .
    Write-Ok "Staged all changes (git add .)"
} else {
    git add -u
    Write-Ok "Staged tracked changes (git add -u)"
}

$status = git status --porcelain
if (-not $status) {
    Write-Host "   Nothing to commit — working tree clean." -ForegroundColor Yellow
    exit 0
}

Write-Step "Committing: $Message"
git commit -m $Message
if ($LASTEXITCODE -ne 0) {
    Write-Fail "git commit failed."
    exit 1
}
Write-Ok "Committed"

# ── 4. Push to origin (public OSS) ───────────────────────────────────────────
Write-Step "Pushing to origin (public OSS repo)"
git push origin main
if ($LASTEXITCODE -ne 0) {
    Write-Fail "Push to origin failed."
    exit 1
}
Write-Ok "Pushed → origin/main"

# ── 5. Push to pro (private, keep in sync) ───────────────────────────────────
Write-Step "Syncing to pro (private repo)"
git push pro main
if ($LASTEXITCODE -ne 0) {
    Write-Fail "Push to pro failed. Run manually: git push pro main"
    exit 1
}
Write-Ok "Pushed → pro/main"

# ── Done ─────────────────────────────────────────────────────────────────────
Write-Host ""
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Green
Write-Host "  ✅  OSS changes pushed to both repos!" -ForegroundColor Green
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Green
Write-Host ""
Write-Host "  origin/main → $(git remote get-url origin)" -ForegroundColor Gray
Write-Host "  pro/main    → $(git remote get-url pro)"    -ForegroundColor Gray
Write-Host ""
