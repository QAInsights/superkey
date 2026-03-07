# SuperKey Pro — Feature Roadmap

Track Pro-only features to implement. Each item should be developed on a local branch,
then pushed to the private repo using `scripts/push-pro-feature.ps1/.sh`.

---

## Backlog

### 1. Multiple Element Selection
- [ ] Allow user to select and add **multiple elements** at once from the SuperKey dialog
- [ ] Multi-select via checkboxes or Ctrl+Click in the results list
- [ ] Add all selected elements into the JMeter test plan in one action
- [ ] Show count badge: "3 elements selected"
- [ ] Gate with `LicenseBridge.isPro()` — show `ProUpgradeBanner` for OSS users

---

### 2. Templates
- [ ] Add a "Templates" tab or section in the SuperKey dialog
- [ ] Pre-built templates that insert a full group of elements at once, e.g.:
  - **REST API** → Thread Group + HTTP Request + JSON Extractor + Response Assertion
  - **Load Test** → Thread Group (ramp-up configured) + HTTP Request + Listeners
  - **Auth Flow** → HTTP Request (login) + Cookie Manager + HTTP Request (secured endpoint)
- [ ] Templates should be inserted at the currently selected node in the JMeter tree
- [ ] Gate with `LicenseBridge.isPro()` — show `ProUpgradeBanner` for OSS users

---

### 3. AI-Generated Test Plan
- [ ] Add an "AI" tab in the SuperKey dialog with a prompt text field
- [ ] User enters a description (e.g. "test my REST API at api.example.com with 50 users")
- [ ] User provides their own API key (OpenAI / Claude / Gemini) — stored in JMeter user.properties
- [ ] AI generates a structured test plan → plugin parses and inserts elements into JMeter tree
- [ ] Support at least one AI provider at launch (OpenAI recommended)
- [ ] Gate with `LicenseBridge.isPro()` — show `ProUpgradeBanner` for OSS users

---

### 4. Dialog Styles (UI Themes)
- [x] Three visual themes for the SuperKey dialog, activated via `user.properties`:
  - **Sharp Rectangle** — hard 90° corners, flat dark fill (IntelliJ / classic style)
  - **Pill / Fully Rounded** — macOS Spotlight oval bar
  - **Floating with Shadow** — rounded rect with elevation shadow (Linear / Notion style)
- [x] Gate with `LicenseBridge.getDialogStyle()` — OSS users see zero change
- [x] Swap is done via reflection in `SuperKeyDialog.applyProStyleIfEnabled()`
- [x] New Pro classes: `DialogStyle`, `StyleManager`, `StyledDialogPanel` (all in `pro/` package)

**User configuration** (add to `$JMETER_HOME/bin/user.properties`):
```properties
# Values: sharp | pill | floating_shadow
superkey.dialog.style=pill
```

---

## Implementation Order (suggested)

| Priority | Feature | Complexity | Status |
|---|---|---|---|
| 1 | Multiple Element Selection | Low | Backlog |
| 2 | Templates | Medium | Backlog |
| 3 | AI-Generated Test Plan | High | Backlog |
| 4 | **Dialog Styles (UI Themes)** | Low | ✅ Done |

---

## Branch Convention

```bash
# Start a new pro feature
git checkout -b pro-feat/multiple-elements

# When done, push using the script
.\scripts\push-pro-feature.ps1 -Message "feat(pro): multiple element selection"
# or
./scripts/push-pro-feature.sh -m "feat(pro): multiple element selection"
```
