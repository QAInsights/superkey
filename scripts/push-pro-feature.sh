#!/usr/bin/env bash
# =============================================================================
# push-pro-feature.sh
# Pushes Pro-only source files to the private superkey-pro repository.
#
# Usage:
#   ./scripts/push-pro-feature.sh -m "feat(pro): add team sync"
#
# Options:
#   -m  Commit message (required)
# =============================================================================
set -euo pipefail

# ── Colours ───────────────────────────────────────────────────────────────────
CYAN='\033[0;36m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'
step()  { echo -e "\n${CYAN}▶  $1${NC}"; }
ok()    { echo -e "   ${GREEN}✅ $1${NC}"; }
warn()  { echo -e "   ${YELLOW}⚠️  $1${NC}"; }
fail()  { echo -e "\n   ${RED}🚨 $1${NC}"; exit 1; }

# ── Parse args ────────────────────────────────────────────────────────────────
MESSAGE=""

while getopts "m:" opt; do
    case $opt in
        m) MESSAGE="$OPTARG" ;;
        *) echo "Usage: $0 -m <commit message>"; exit 1 ;;
    esac
done

[[ -z "$MESSAGE" ]] && fail "Commit message is required. Use: $0 -m \"feat(pro): your message\""

# ── Navigate to repo root ─────────────────────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

# ── 1. Determine branch ───────────────────────────────────────────────────────
step "Checking git state"
ok "Repo root   : $REPO_ROOT"
ok "Push target : pro/main (direct — no PR needed)"

# ── 2. Check pro remote exists ────────────────────────────────────────────────
step "Verifying 'pro' remote exists"
if ! git remote | grep -q "^pro$"; then
    fail "'pro' remote not found. Add it first:\n   git remote add pro https://github.com/QAInsights/superkey-pro.git"
fi
ok "Remote 'pro' found: $(git remote get-url pro)"

# ── 3. Detect pro/ files on disk ──────────────────────────────────────────────
step "Scanning for pro/ source files"
PRO_DIR="src/main/java/io/github/naveenkumar/jmeter/superkey/pro"

if [[ ! -d "$PRO_DIR" ]]; then
    fail "Pro source directory not found: $PRO_DIR"
fi

PRO_FILES=$(find "$PRO_DIR" -name "*.java" 2>/dev/null)
FILE_COUNT=$(echo "$PRO_FILES" | grep -c ".java" || true)

[[ $FILE_COUNT -eq 0 ]] && fail "No .java files found under $PRO_DIR"

ok "Found $FILE_COUNT pro source file(s):"
echo "$PRO_FILES" | while read -r f; do
    echo -e "      ${f#$REPO_ROOT/}"
done

# ── 4. Build Pro JAR to verify everything compiles ────────────────────────────
step "Building Pro JAR (mvn verify -P pro)"
if ! mvn verify -P pro -q; then
    fail "Pro build FAILED. Fix the errors above before pushing."
fi
ok "Pro build succeeded — all tests passed"

# ── 5. Force-add pro files ────────────────────────────────────────────────────
step "Force-adding pro/ files to git (they are gitignored in OSS repo)"
git add -f "$PRO_DIR"
ok "Pro files staged"

# ── 6. Offer to stage other changes ───────────────────────────────────────────
if [[ -n "$(git status --porcelain)" ]]; then
    warn "Additional unstaged changes detected:"
    git status --short
    read -rp "   Stage ALL other changes too? (y/N): " addOss
    [[ "$addOss" == "y" ]] && git add .
fi

# ── 7. Commit ─────────────────────────────────────────────────────────────────
step "Committing: $MESSAGE"
git commit -m "$MESSAGE"
ok "Committed"

# ── 8. Push directly to pro/main ─────────────────────────────────────────────
step "Pushing directly to pro/main"
if ! git push pro HEAD:main; then
    fail "git push to 'pro/main' failed. Rolling back local commit..."
    git reset HEAD~1
    exit 1
fi
ok "Pushed to private pro repo: $(git remote get-url pro)"

# ── 9. Reset locally ─────────────────────────────────────────────────────────
step "Resetting local commit (pro files stay on disk)"
git reset HEAD~1
ok "Local commit removed — pro files still on disk, git history is clean"

# ── Done ──────────────────────────────────────────────────────────────────────
echo ""
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}  ✅  Pro feature pushed successfully!${NC}"
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo "  Remote : $(git remote get-url pro)"
echo "  Branch : pro/main (direct push)"
echo ""
echo "  Done — no PR needed!"
echo ""
