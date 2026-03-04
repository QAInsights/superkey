#!/usr/bin/env bash
# =============================================================================
# build-and-deploy.sh
# Builds SuperKey (OSS or Pro) and deploys the JAR to your local JMeter.
#
# Usage:
#   ./scripts/build-and-deploy.sh              # builds OSS (default)
#   ./scripts/build-and-deploy.sh -p pro       # builds Pro
#   ./scripts/build-and-deploy.sh -p pro -j /path/to/jmeter
#
# Options:
#   -p  Profile: oss or pro (default: oss)
#   -j  JMeter home directory (default: auto-detected or $JMETER_HOME env var)
# =============================================================================
set -euo pipefail

# ── Colours ───────────────────────────────────────────────────────────────────
CYAN='\033[0;36m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'
step() { echo -e "\n${CYAN}▶  $1${NC}"; }
ok()   { echo -e "   ${GREEN}✅ $1${NC}"; }
fail() { echo -e "\n   ${RED}🚨 $1${NC}"; exit 1; }

# ── Defaults ──────────────────────────────────────────────────────────────────
PROFILE="oss"

# Auto-detect JMeter home: env var → common Mac location → common Windows location
detect_jmeter_home() {
    if [[ -n "${JMETER_HOME:-}" ]] && [[ -d "$JMETER_HOME" ]]; then
        echo "$JMETER_HOME"
        return
    fi
    # macOS: Homebrew or ~/tools
    for candidate in \
        "/opt/homebrew/opt/jmeter" \
        "$HOME/tools/apache-jmeter-5.6.3" \
        "/usr/local/opt/jmeter" \
        "$HOME/apache-jmeter-5.6.3"; do
        [[ -d "$candidate/lib/ext" ]] && echo "$candidate" && return
    done
    echo ""
}

JMETER_HOME_ARG=""

# ── Parse args ────────────────────────────────────────────────────────────────
while getopts "p:j:" opt; do
    case $opt in
        p) PROFILE="$OPTARG" ;;
        j) JMETER_HOME_ARG="$OPTARG" ;;
        *) echo "Usage: $0 [-p oss|pro] [-j /path/to/jmeter]"; exit 1 ;;
    esac
done

[[ "$PROFILE" != "oss" && "$PROFILE" != "pro" ]] && fail "Profile must be 'oss' or 'pro'"

# ── Navigate to repo root ─────────────────────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

# ── Resolve JMeter home ───────────────────────────────────────────────────────
if [[ -n "$JMETER_HOME_ARG" ]]; then
    JMETER_HOME="$JMETER_HOME_ARG"
else
    JMETER_HOME="$(detect_jmeter_home)"
fi

# ── 1. Validate JMeter home ───────────────────────────────────────────────────
step "Validating JMeter installation"
EXT_DIR="$JMETER_HOME/lib/ext"

if [[ -z "$JMETER_HOME" ]] || [[ ! -d "$EXT_DIR" ]]; then
    fail "JMeter lib/ext not found.\n   Set JMETER_HOME env var or pass: -j /path/to/jmeter"
fi
ok "JMeter lib/ext: $EXT_DIR"

# ── 2. Build ──────────────────────────────────────────────────────────────────
step "Building SuperKey [$PROFILE profile] — mvn verify -P $PROFILE"
if ! mvn verify -P "$PROFILE"; then
    fail "Build FAILED. Fix the errors above."
fi
ok "Build succeeded"

# ── 3. Find the built JAR ─────────────────────────────────────────────────────
step "Locating built JAR"
TARGET_JAR=$(find target -maxdepth 1 -name "*-${PROFILE}.jar" | head -1)
[[ -z "$TARGET_JAR" ]] && fail "Could not find *-${PROFILE}.jar in target/. Run the build first."

JAR_SIZE=$(du -sh "$TARGET_JAR" | cut -f1)
ok "JAR  : $TARGET_JAR"
ok "Size : $JAR_SIZE"

# ── 4. Remove old SuperKey JARs ───────────────────────────────────────────────
step "Removing old SuperKey JARs from JMeter lib/ext"
OLD_JARS=$(find "$EXT_DIR" -name "superkey-*.jar" 2>/dev/null || true)
if [[ -n "$OLD_JARS" ]]; then
    echo "$OLD_JARS" | while read -r old; do
        rm -f "$old"
        echo -e "   Removed: $(basename "$old")"
    done
else
    echo -e "   No existing SuperKey JARs found — clean install."
fi

# ── 5. Copy new JAR ───────────────────────────────────────────────────────────
step "Deploying JAR to JMeter"
cp "$TARGET_JAR" "$EXT_DIR/"
ok "Deployed: $(basename "$TARGET_JAR") → $EXT_DIR"

# ── Done ──────────────────────────────────────────────────────────────────────
[[ "$PROFILE" == "pro" ]] && EMOJI="🔒" || EMOJI="🌐"
echo ""
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}  ✅  SuperKey [$PROFILE] deployed successfully! $EMOJI${NC}"
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo "  JAR    : $(basename "$TARGET_JAR")"
echo "  JMeter : $EXT_DIR"
echo ""
echo -e "  ➡  Restart JMeter to load the new plugin."
echo ""
