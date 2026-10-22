# Ill Eagle — Project Summary & Architecture Overview

## 1. Project Overview & Vision
**Ill Eagle** (formerly Emigrant English) is an offline, rule-based custom Android keyboard derived from HeliBoard 4.2-beta1 / AOSP LatinIME. Its core purpose is to apply deliberately incorrect, humorous, or dialectical English transformations (word replacements, suffix adaptations, multiword phrase substitutions, sentence-dependent context rules, and subject-verb grammar modifications) after standard HeliBoard autocorrect has finished, without compromising standard keyboard features, privacy, or performance.

---

## 2. Key Architectural Pillars

### A. Core Transformation Engine (`EmigrantEngine.kt`)
- **Package:** `helium314.keyboard.latin.emigrant`
- **Design:** Singleton engine (`EmigrantEngine.instance`) executing transformations during word commit (`processWordCommit`) and after word commit (`evaluatePhraseRulesAfterCommit`).
- **Rule Categories & Deterministic Priority Hierarchy:**
  1. **Context Rules (`context_rules`):** Highest priority. Evaluated against bounded sentence history (`contains.size` descending for condition specificity). Can override single-word replacements safely.
  2. **Phrase Rules (`phrase_rules`):** Multiword exact phrase substitutions (e.g., `ill eagle` -> `illegal`).
  3. **Grammar Rules (`grammar_rules`):** Pattern-based subject-verb rules evaluated against the immediately preceding subject word (e.g., `I am` -> `I is`).
  4. **Word Rules (`word_rules`):** Single-word exact substitutions (e.g., `illegal` -> `ill eagle`, `jam` -> `jelly`).
  5. **Suffix Rules (`default_suffixes`):** Automatic global suffix handling (e.g., `-s`, `-ing`, `-ed`, `-ly`, `-ity`, `-ness`).

### B. Runtime Multiple Replacements & Randomness
- Rules support both single `"replacement": "..."` and array `"replacements": ["...", "..."]`.
- When multiple replacements exist for a matching rule, one replacement is selected randomly at runtime with equal probability using an injectable `Random` instance.
- Identical match conditions in JSON are automatically grouped and deduplicated upon import.

### C. Persistent Ruleset Manager & Transactional Import (`EmigrantRulesManager.kt`)
- Loads bundled default asset dictionary (`assets/emigrant_rules.json`).
- Supports importing custom JSON rulesets via Android's System Document Picker (`ACTION_OPEN_DOCUMENT`) without broad storage permissions.
- **Transactional Imports:** Validates imported JSON structure into temporary memory without mutating active engine state. Saves to temporary file `emigrant_rules_custom.tmp` before atomically replacing `emigrant_rules_custom.json`. On any failure, previous stored file and active ruleset remain untouched.
- Saves active ruleset metadata and individual feature category toggles in `SharedPreferences`.

### D. Settings UI Integration (`EmigrantRulesScreen.kt`)
- Accessible via **HeliBoard Settings -> Emigrant English Rules**.
- Features:
  - **Master Switch:** Emigrant Mode (ON / OFF). When OFF, keyboard behaves as standard HeliBoard.
  - **Individual Category Toggles:**
    - Word Replacements (ON / OFF)
    - Automatic Suffixes (ON / OFF)
    - Phrase Rules (ON / OFF)
    - Context Rules (ON / OFF)
    - Grammar Rules (ON / OFF)
  - Active Ruleset Display
  - Import JSON Ruleset Button
  - Restore Bundled Defaults Button

### E. Privacy, Safety & History Tracking
- **RAM-Only History:** `wordHistory` (max 10 entries) and `sentenceHistory` (max 20 entries) are RAM-only and **never persisted to disk**.
- **Loop Prevention:** Per-entry `contextApplied = true` and `isEmigrantGenerated = true` flags prevent recursive or infinite re-transformations.
- **Boundary Isolation:** Sentence history is immediately cleared on punctuation boundaries (`.`, `!`, `?`, `\n`), field switches, editor resets, or when Backspace modifies committed text.
- **Sensitive Fields:** Transformations are completely bypassed in password and sensitive fields.
- **Strict Editor Text Verification:** Retrospective replacements query `InputConnection.getTextBeforeCursor()` to confirm exact editor text before modifying it. If text or cursor position is uncertain, replacement is safely skipped.

---

## 3. JSON Dictionary Schema Format (`assets/emigrant_rules.json`)

```json
{
  "version": 1,
  "default_suffixes": {
    "s": "s",
    "es": "es",
    "ed": "ed",
    "ing": "ing",
    "ly": "ly",
    "ity": "ity",
    "ities": "ities",
    "ness": "ness",
    "able": "able",
    "ful": "ful"
  },
  "word_rules": [
    {
      "id": "word_illegal",
      "match": "illegal",
      "replacement": "ill eagle"
    },
    {
      "id": "word_jam",
      "match": "jam",
      "replacements": ["jelly", "marmalade"]
    }
  ],
  "phrase_rules": [
    {
      "id": "phrase_ill_eagle",
      "match": "ill eagle",
      "replacement": "illegal"
    }
  ],
  "context_rules": [
    {
      "id": "context_cash_bank",
      "contains": ["cash", "bank"],
      "target": "bank",
      "replacement": "river bank"
    },
    {
      "id": "context_jam_traffic",
      "contains": ["jam", "traffic"],
      "target": "jam",
      "replacements": ["strawberry jam", "raspberry jam", "fruit jam"]
    }
  ],
  "grammar_rules": [
    {
      "id": "grammar_bad_be",
      "subjects": ["i", "you", "we", "they"],
      "verb_replacements": {
        "am": "is",
        "are": "is"
      }
    }
  ]
}
```

---

## 4. Key Modified & Custom Files Summary

| File Path | Role |
|---|---|
| `app/src/main/assets/emigrant_rules.json` | Bundled default JSON dictionary |
| `app/src/main/java/helium314/keyboard/latin/emigrant/EmigrantEngine.kt` | Core transformation engine & validation |
| `app/src/main/java/helium314/keyboard/latin/emigrant/EmigrantRulesManager.kt` | Ruleset manager, JSON import, preferences |
| `app/src/main/java/helium314/keyboard/settings/screens/EmigrantRulesScreen.kt` | Settings UI (Master switch, category toggles, import/restore) |
| `app/src/test/java/helium314/keyboard/latin/emigrant/EmigrantEngineTest.kt` | Unit test suite (25 passing tests) |
| `app/src/main/java/helium314/keyboard/latin/inputlogic/InputLogic.java` | Word commit integration, phrase/context/grammar evaluation, backspace handling |
| `app/src/main/java/helium314/keyboard/latin/LatinIME.java` | Engine initialization & history lifecycle |
| `app/src/main/java/helium314/keyboard/settings/screens/MainSettingsScreen.kt` | Main settings menu entry |
| `app/src/main/java/helium314/keyboard/settings/SettingsNavHost.kt` | Settings navigation route |
| `app/build.gradle.kts` | App name, build config, and output APK naming (`IllEagle...apk`) |
| `settings.gradle` | Root project name (`Ill-Eagle`) |
| `donottranslate.xml` & `strings.xml` | App branding name (`Ill Eagle`) |

---

## 5. Verification & Test Suite Status
- **Unit Tests:** `25/25 Passed` in `EmigrantEngineTest`.
- **Debug APK Build:** Successfully compiled (`gradle_build("app:assembleDebug")`).
- **Real-Phone Testing:** Passed all real Samsung device tests across word rules, automatic suffixes, phrase rules, forward/reverse context rules, subject-verb grammar rules, category toggles, and JSON imports.

---

## 6. Project Roadmap & Future Milestones
- **Milestones 1–7 (Completed):** Core Engine, Automatic Suffixes, Multiword Phrases, Context Rules, Transactional JSON Import, Settings UI, Category Switches, Deterministic Rule Priority, Multiple Alternatives & Runtime Randomness.
- **Milestone 5A (Completed):** First Subject-Verb Grammar Rule Engine.
- **Future Milestones:** Expanded grammar pattern matchers, optional dictionary validation/real-words-only mode, selective probabilities.
