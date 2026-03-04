#!/usr/bin/env bash
# =============================================================================
# push-oss.sh
# Commits and pushes OSS changes to BOTH the public (origin) and private (pro) repos.
#
# Usage:
#   ./scripts/push-oss.sh -m "feat(oss): add shortcut aliases"
#   ./scripts/push-oss.sh -m "fix(oss): resolve NPE" -a     # stage all files
#
# Options:
#   -m  Commit message (required)
#   -a  Stage all changes (git add .). Default: only tracked files (git add -u)
# =============================================================================
set -euo pipefail

CYAN='\033[0;36m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'
step() { echo -e "\n${CYAN}▶  $1${NC}"; }
ok()   { echo -e "   ${GREEN}✅ $1${NC}"; }
fail() { echo -e "\n   ${RED}🚨 $1${NC}"; exit 1; }

MESSAGE=""
STAGE_ALL=false

while getopts "m:a" opt; do
    case $opt in
        m) MESSAGE="$OPTARG" ;;
        a) STAGE_ALL=true ;;
        *) echo "Usage: $0 -m <message> [-a]"; exit 1 ;;
    esac
done

[[ -z "$MESSAGE" ]] && fail "Commit message required. Use: $0 -m \"feat(oss): your message\""

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

# ── 1. Verify remotes ─────────────────────────────────────────────────────────
step "Verifying remotes"
git remote | grep -q "^pro$" || fail "'pro' remote not found.\n   git remote add pro https://github.com/QAInsights/superkey-pro.git"
ok "origin → $(git remote get-url origin)"
ok "pro    → $(git remote get-url pro)"

# ── 2. Build OSS (safety check) ───────────────────────────────────────────────
step "Verifying OSS build (mvn verify -P oss)"
mvn verify -P oss -q || fail "OSS build FAILED. Fix errors before pushing."
ok "OSS build passed"

# ── 3. Stage and commit ───────────────────────────────────────────────────────
step "Staging changes"
if $STAGE_ALL; then
    git add .
    ok "Staged all changes (git add .)"
else
    git add -u
    ok "Staged tracked changes (git add -u)"
fi

if [[ -z "$(git status --porcelain)" ]]; then
    echo -e "   ${YELLOW}Nothing to commit — working tree clean.${NC}"
    exit 0
fi

step "Committing: $MESSAGE"
git commit -m "$MESSAGE"
ok "Committed"

# ── 4. Push to origin (public OSS) ───────────────────────────────────────────
step "Pushing to origin (public OSS repo)"
git push origin main
ok "Pushed → origin/main"

# ── 5. Push to pro (private, keep in sync) ───────────────────────────────────
step "Syncing to pro (private repo)"
git push pro main
ok "Pushed → pro/main"

# ── Done ─────────────────────────────────────────────────────────────────────
echo ""
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}  ✅  OSS changes pushed to both repos!${NC}"
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo "  origin/main → $(git remote get-url origin)"
echo "  pro/main    → $(git remote get-url pro)"
echo ""
