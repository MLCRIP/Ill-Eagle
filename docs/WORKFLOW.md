# Ill Eagle — Versioning, Build Packaging & Development Workflow

This document records the permanent development workflow for preparing and packaging testing builds of **Ill Eagle**.

---

## 1. Five-Part Versioning Scheme

Ill Eagle uses a five-component version string:

`vMAJOR.FEATURE.IMPROVEMENT.FIX.BUILD`

### Component Increment Rules
- **MAJOR (1st):** Major architectural changes or milestone releases (e.g. `v1.0.0.0.0`).
- **FEATURE (2nd):** Significant new features or functional modules (e.g. `v0.1.0.0.0`).
- **IMPROVEMENT (3rd):** Smaller features or meaningful improvements to existing components (e.g. `v0.0.1.0.0`).
- **FIX (4th):** Bug fixes and minor behavioral corrections (e.g. `v0.0.0.1.0`).
- **BUILD (5th, default):** New testing builds with no higher-level increment (e.g. `v0.0.0.0.1`).

**Reset Rule:** When incrementing a higher component, all components to its right are automatically reset to zero.

---

## 2. Running a New Testing Build

Version increments occur **only** when explicitly executing the packaging workflow script:

```bash
python tools/package_test_build.py [--type {build,fix,improvement,feature,major}] [--version vX.Y.Z.A.B] [--dry-run]
```

### Examples
- **Default testing build increment (BUILD + 1):**
  ```bash
  python tools/package_test_build.py
  ```
- **Bug fix build increment (FIX + 1):**
  ```bash
  python tools/package_test_build.py --type fix
  ```
- **New feature build increment (FEATURE + 1):**
  ```bash
  python tools/package_test_build.py --type feature
  ```
- **Explicit custom version override:**
  ```bash
  python tools/package_test_build.py --version v0.1.0.0.0
  ```
- **Dry-run simulation (no files modified):**
  ```bash
  python tools/package_test_build.py --dry-run
  ```

---

## 3. Versioning & Build Safety Properties

1. **Monotonic Version Code:** Android `versionCode` increases by 1 for every successful test package build.
2. **Standard Builds Do Not Increment:** Routine Gradle builds (`gradle_build("app:assembleDebug")`), unit tests, and IDE indexing read `version.properties` without modifying it.
3. **Transaction Rollback:** If a build or packaging step fails, `version.properties` automatically rolls back to its pre-build state so the version number is not permanently consumed.
4. **Collision Protection:** Before starting a build, the script verifies whether target files already exist in `C:\AndroidProjects\keyboard\`. If a target file exists, the workflow halts with a collision error.

---

## 4. Generated Artifacts & Parent Directory Destination

All 3 testing artifacts are published directly to the parent directory:

`C:\AndroidProjects\keyboard\`

### Artifact Inventory for Version `vX.Y.Z.A.B`:
1. **`IllEagle_vX.Y.Z.A.B-debug.apk`**  
   The compiled, installable Android debug APK (`applicationId: net.mlcrip.illeagle.debug`).
2. **`IllEagle_vX.Y.Z.A.B-source.zip`**  
   Clean source archive containing all code, resources, dictionaries, licenses, and docs required to build the exact APK. **Excludes `.git/`** and build caches.
3. **`IllEagle_vX.Y.Z.A.B-backup.zip`**  
   Full development backup archive **preserving the complete `.git/` repository and commit history**.

---

## 5. How to Restore from a Development Backup

To restore the project from a development backup ZIP:

1. Unzip `IllEagle_vX.Y.Z.A.B-backup.zip` into your desired working directory.
2. Verify that `.git/` is present in the unzipped folder.
3. Run `git status` to verify commit history and working tree status.
4. Run `gradlew app:assembleDebug` or open the project in Android Studio.
