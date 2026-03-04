<#
.SYNOPSIS
    Pushes Pro-only source files to the private superkey-pro repository.

.DESCRIPTION
    Handles the full Pro feature push workflow:
      1. Shows which pro/ files will be committed
      2. Builds + tests the Pro JAR to confirm everything compiles
      3. Force-adds the pro/ source files (they are git-ignored in the OSS repo)
      4. Commits with your message
      5. Pushes directly to 'pro/main' (no PR needed on the private repo)
      6. Resets the commit locally (keeps files on disk, removes from git history)

    After this script, your local branch is clean and the pro files are
    safely in the private repo. The OSS public repo is never touched.

.PARAMETER Message
    The commit message (required). Example: "feat(pro): add team sync feature"

.EXAMPLE
    .\scripts\push-pro-feature.ps1 -Message "feat(pro): add team sync"
    .\scripts\push-pro-feature.ps1 -Message "feat(pro): license server"
#>
param(
    [Parameter(Mandatory = $true)]
    [string]$Message
)

# ── Colours ───────────────────────────────────────────────────────────────────
function Write-Step   { param($m) Write-Host "`n▶  $m" -ForegroundColor Cyan }
function Write-Ok     { param($m) Write-Host "   ✅ $m" -ForegroundColor Green }
function Write-Warn   { param($m) Write-Host "   ⚠️  $m" -ForegroundColor Yellow }
function Write-Fail   { param($m) Write-Host "`n   🚨 $m" -ForegroundColor Red }

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot  = Split-Path -Parent $ScriptDir
Set-Location $RepoRoot

# ── 1. Show state ─────────────────────────────────────────────────────────────
Write-Step "Checking git state"
Write-Ok "Repo root   : $RepoRoot"
Write-Ok "Push target : pro/main (direct — no PR needed)"

# ── 2. Check pro remote exists ────────────────────────────────────────────────
Write-Step "Verifying 'pro' remote exists"
$remotes = git remote
if ($remotes -notcontains "pro") {
    Write-Fail "'pro' remote not found. Add it first:"
    Write-Host "   git remote add pro https://github.com/QAInsights/superkey-pro.git" -ForegroundColor Yellow
    exit 1
}
Write-Ok "Remote 'pro' found: $(git remote get-url pro)"

# ── 3. Detect pro/ files on disk ──────────────────────────────────────────────
Write-Step "Scanning for pro/ source files"
$ProDir = "src\main\java\io\github\naveenkumar\jmeter\superkey\pro"
$ProFiles = Get-ChildItem -Path $ProDir -Recurse -Filter *.java -ErrorAction SilentlyContinue

if (-not $ProFiles -or $ProFiles.Count -eq 0) {
    Write-Fail "No .java files found under $ProDir"
    Write-Host "   Nothing to commit." -ForegroundColor Yellow
    exit 1
}

Write-Ok "Found $($ProFiles.Count) pro source file(s):"
foreach ($f in $ProFiles) {
    Write-Host "      $($f.FullName.Replace($RepoRoot + '\', ''))" -ForegroundColor Gray
}

# ── 3. Build Pro JAR to verify everything compiles ────────────────────────────
Write-Step "Building Pro JAR (mvn verify -P pro)"
mvn verify -P pro -q
if ($LASTEXITCODE -ne 0) {
    Write-Fail "Pro build FAILED. Fix the errors above before pushing."
    exit 1
}
Write-Ok "Pro build succeeded — all tests passed"

# ── 4. Force-add pro files ────────────────────────────────────────────────────
Write-Step "Force-adding pro/ files to git (they are gitignored in OSS repo)"
git add -f $ProDir
if ($LASTEXITCODE -ne 0) {
    Write-Fail "git add failed."
    exit 1
}
Write-Ok "Pro files staged"

# Also stage any other unstaged OSS changes (pom.xml, etc.)
$status = git status --porcelain
if ($status) {
    Write-Warn "Additional unstaged changes detected:"
    git status --short
    $addOss = Read-Host "   Stage ALL other changes too? (y/N)"
    if ($addOss -eq "y") { git add . }
}

# ── 5. Commit ─────────────────────────────────────────────────────────────────
Write-Step "Committing: $Message"
git commit -m $Message
if ($LASTEXITCODE -ne 0) {
    Write-Fail "git commit failed."
    exit 1
}
Write-Ok "Committed"

# ── 6. Push directly to pro/main ─────────────────────────────────────────────
Write-Step "Pushing directly to pro/main"
git push pro HEAD:main
if ($LASTEXITCODE -ne 0) {
    Write-Fail "git push to 'pro/main' failed. Rolling back local commit..."
    git reset HEAD~1
    exit 1
}
Write-Ok "Pushed to private pro repo: $(git remote get-url pro)"

# ── 7. Reset locally (remove pro commit from local history) ───────────────────
Write-Step "Resetting local commit (pro files stay on disk)"
git reset HEAD~1
Write-Ok "Local commit removed — pro files still on disk, git history is clean"

# ── Done ──────────────────────────────────────────────────────────────────────
Write-Host ""
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Green
Write-Host "  ✅  Pro feature pushed successfully!" -ForegroundColor Green
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Green
Write-Host ""
Write-Host "  Remote : $(git remote get-url pro)" -ForegroundColor Gray
Write-Host "  Branch : pro/main (direct push)" -ForegroundColor Gray
Write-Host ""
Write-Host "  Done — no PR needed!" -ForegroundColor White
Write-Host ""
