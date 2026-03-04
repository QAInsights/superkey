<#
.SYNOPSIS
    Pushes Pro-only source files to the private superkey-pro repository.

.DESCRIPTION
    Handles the full Pro feature push workflow:
      1. Verifies you are on appropriate branch
      2. Shows which pro/ files will be committed
      3. Builds + tests the Pro JAR to confirm everything compiles
      4. Force-adds the pro/ source files (they are git-ignored in the OSS repo)
      5. Commits with your message
      6. Pushes to the 'pro' remote ONLY
      7. Resets the commit locally (keeps files on disk, removes from git history)

    After this script, your local branch is clean and the pro files are
    safely in the private repo. The OSS public repo is never touched.

.PARAMETER Message
    The commit message (required). Example: "feat(pro): add team sync feature"

.PARAMETER Branch
    The branch to push to on the pro remote. Defaults to current branch.

.EXAMPLE
    .\scripts\push-pro-feature.ps1 -Message "feat(pro): add team sync"
    .\scripts\push-pro-feature.ps1 -Message "feat(pro): license server" -Branch "pro-feat/license-server"
#>
param(
    [Parameter(Mandatory = $true)]
    [string]$Message,

    [Parameter(Mandatory = $false)]
    [string]$Branch = ""
)

# ── Colours ───────────────────────────────────────────────────────────────────
function Write-Step   { param($m) Write-Host "`n▶  $m" -ForegroundColor Cyan }
function Write-Ok     { param($m) Write-Host "   ✅ $m" -ForegroundColor Green }
function Write-Warn   { param($m) Write-Host "   ⚠️  $m" -ForegroundColor Yellow }
function Write-Fail   { param($m) Write-Host "`n   🚨 $m" -ForegroundColor Red }

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot  = Split-Path -Parent $ScriptDir
Set-Location $RepoRoot

# ── 1. Determine branch ───────────────────────────────────────────────────────
Write-Step "Checking git state"

$CurrentBranch = git branch --show-current
if (-not $Branch) { $Branch = $CurrentBranch }

Write-Ok "Repo root : $RepoRoot"
Write-Ok "Branch    : $Branch"

# Warn if pushing from main directly
if ($Branch -eq "main") {
    Write-Warn "You are pushing a Pro feature directly from 'main'."
    Write-Warn "Consider using a 'pro-feat/*' branch for cleaner history."
    $confirm = Read-Host "   Continue anyway? (y/N)"
    if ($confirm -ne "y") { exit 0 }
}

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

# ── 4. Build Pro JAR to verify everything compiles ────────────────────────────
Write-Step "Building Pro JAR (mvn verify -P pro)"
mvn verify -P pro -q
if ($LASTEXITCODE -ne 0) {
    Write-Fail "Pro build FAILED. Fix the errors above before pushing."
    exit 1
}
Write-Ok "Pro build succeeded — all tests passed"

# ── 5. Force-add pro files ────────────────────────────────────────────────────
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

# ── 6. Commit ─────────────────────────────────────────────────────────────────
Write-Step "Committing: $Message"
git commit -m $Message
if ($LASTEXITCODE -ne 0) {
    Write-Fail "git commit failed."
    exit 1
}
Write-Ok "Committed"

# ── 7. Push to pro remote ONLY ────────────────────────────────────────────────
Write-Step "Pushing to 'pro' remote — $Branch"
git push pro $Branch
if ($LASTEXITCODE -ne 0) {
    Write-Fail "git push to 'pro' remote failed. Rolling back local commit..."
    git reset HEAD~1
    exit 1
}
Write-Ok "Pushed to private pro repo: $(git remote get-url pro)"

# ── 8. Reset locally (remove pro commit from local history) ───────────────────
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
Write-Host "  Branch : $Branch" -ForegroundColor Gray
Write-Host ""
Write-Host "  Next steps:" -ForegroundColor White
Write-Host "  - Open a PR on the private GitHub repo to merge into main" -ForegroundColor Gray
Write-Host "  - After merging, run: git push pro main (to sync)" -ForegroundColor Gray
Write-Host ""
