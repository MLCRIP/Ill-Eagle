#!/usr/bin/env python3
"""
Ill Eagle Testing Build & Packaging Workflow
Project: C:\\AndroidProjects\\keyboard\\Ill-Eagle
Artifact Destination: C:\\AndroidProjects\\keyboard\\

Usage:
  python tools/package_test_build.py [--type {build,fix,improvement,feature,major}] [--version vX.Y.Z.A.B] [--dry-run]
"""

import sys
import os
import re
import shutil
import subprocess
import zipfile
import tempfile
import argparse
from pathlib import Path

# Paths
PROJECT_DIR = Path(__file__).resolve().parent.parent
PARENT_DIR = PROJECT_DIR.parent
VERSION_FILE = PROJECT_DIR / "version.properties"
OUTPUT_APK_DIR = PROJECT_DIR / "app" / "build" / "outputs" / "apk" / "debug"

EXCLUDE_DIRS = {
    ".gradle", ".idea", ".kotlin", "build", "app/build",
    "app/.cxx", ".cxx", "captures", "dicts_readme_tmp.md"
}

EXCLUDE_EXTENSIONS = {
    ".apk", ".aab", ".keystore", ".jks", ".p12"
}

EXCLUDE_FILES = {
    "local.properties", "secrets.properties"
}

def parse_version_props(file_path: Path):
    props = {}
    if file_path.exists():
        with open(file_path, "r", encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if line and not line.startswith("#") and "=" in line:
                    k, v = line.split("=", 1)
                    props[k.strip()] = v.strip()
    return props

def write_version_props(file_path: Path, major: int, feature: int, improvement: int, fix: int, build: int, code: int):
    content = f"""# Ill Eagle Version Properties
version.major={major}
version.feature={feature}
version.improvement={improvement}
version.fix={fix}
version.build={build}
version.code={code}
"""
    with open(file_path, "w", encoding="utf-8") as f:
        f.write(content)

def compute_next_version(current_props: dict, inc_type: str, custom_version: str = None):
    major = int(current_props.get("version.major", "0"))
    feature = int(current_props.get("version.feature", "0"))
    improvement = int(current_props.get("version.improvement", "0"))
    fix = int(current_props.get("version.fix", "0"))
    build = int(current_props.get("version.build", "0"))
    code = int(current_props.get("version.code", "4200"))

    if custom_version:
        m = re.match(r"^v?(\d+)\.(\d+)\.(\d+)\.(\d+)\.(\d+)$", custom_version)
        if not m:
            raise ValueError(f"Custom version '{custom_version}' must match format vMAJOR.FEATURE.IMPROVEMENT.FIX.BUILD (e.g. v0.0.0.0.1)")
        major, feature, improvement, fix, build = map(int, m.groups())
    else:
        if inc_type == "major":
            major += 1
            feature = improvement = fix = build = 0
        elif inc_type == "feature":
            feature += 1
            improvement = fix = build = 0
        elif inc_type == "improvement":
            improvement += 1
            fix = build = 0
        elif inc_type == "fix":
            fix += 1
            build = 0
        elif inc_type == "build":
            build += 1
        else:
            raise ValueError(f"Unknown increment type: {inc_type}")

    next_code = code + 1
    next_version_str = f"v{major}.{feature}.{improvement}.{fix}.{build}"
    return major, feature, improvement, fix, build, next_code, next_version_str

def check_collisions(parent_dir: Path, version_str: str):
    apk_name = f"IllEagle_{version_str}-debug.apk"
    source_zip_name = f"IllEagle_{version_str}-source.zip"
    backup_zip_name = f"IllEagle_{version_str}-backup.zip"

    collisions = []
    for f in [apk_name, source_zip_name, backup_zip_name]:
        target = parent_dir / f
        if target.exists():
            collisions.append(target)

    if collisions:
        raise FileExistsError(
            f"VERSION COLLISION DETECTED! Target file(s) already exist in {parent_dir}:\n" +
            "\n".join([f"  - {c.name}" for c in collisions]) +
            "\nPlease increment version component or archive/remove old files before packaging."
        )
    return apk_name, source_zip_name, backup_zip_name

def is_path_excluded(rel_path_str: str, is_dir: bool, include_git: bool):
    rel_path_str = rel_path_str.replace("\\", "/")
    parts = rel_path_str.split("/")

    if not include_git and (parts[0] == ".git" or "/.git/" in f"/{rel_path_str}/"):
        return True

    for p in parts:
        if p in EXCLUDE_DIRS:
            return True

    if not is_dir:
        filename = parts[-1]
        if filename in EXCLUDE_FILES:
            return True
        ext = os.path.splitext(filename)[1].lower()
        if ext in EXCLUDE_EXTENSIONS:
            return True

    return False

def create_source_zip(project_dir: Path, zip_path: Path):
    with zipfile.ZipFile(zip_path, "w", compression=zipfile.ZIP_DEFLATED) as zf:
        for root, dirs, files in os.walk(project_dir):
            rel_root = os.path.relpath(root, project_dir)
            if rel_root == ".":
                rel_root = ""

            dirs[:] = [d for d in dirs if not is_path_excluded(os.path.join(rel_root, d), is_dir=True, include_git=False)]

            for file in files:
                rel_file = os.path.join(rel_root, file) if rel_root else file
                if not is_path_excluded(rel_file, is_dir=False, include_git=False):
                    abs_file = project_dir / rel_file
                    zf.write(abs_file, arcname=rel_file)

def create_backup_zip(project_dir: Path, zip_path: Path):
    with zipfile.ZipFile(zip_path, "w", compression=zipfile.ZIP_DEFLATED) as zf:
        for root, dirs, files in os.walk(project_dir):
            rel_root = os.path.relpath(root, project_dir)
            if rel_root == ".":
                rel_root = ""

            dirs[:] = [d for d in dirs if not is_path_excluded(os.path.join(rel_root, d), is_dir=True, include_git=True)]

            for file in files:
                rel_file = os.path.join(rel_root, file) if rel_root else file
                if not is_path_excluded(rel_file, is_dir=False, include_git=True):
                    abs_file = project_dir / rel_file
                    zf.write(abs_file, arcname=rel_file)

def main():
    parser = argparse.ArgumentParser(description="Package Ill Eagle Testing Build")
    parser.add_argument("--type", choices=["build", "fix", "improvement", "feature", "major"], default="build",
                        help="Component to increment (default: build)")
    parser.add_argument("--version", help="Explicit custom 5-part version string (e.g. v0.0.0.0.1)")
    parser.add_argument("--dry-run", action="store_true", help="Simulate without writing files or building")
    args = parser.parse_args()

    print("==================================================")
    print("  Ill Eagle Testing Build & Packaging Workflow")
    print("==================================================")

    current_props = parse_version_props(VERSION_FILE)
    cur_version_str = f"v{current_props.get('version.major', 0)}.{current_props.get('version.feature', 0)}.{current_props.get('version.improvement', 0)}.{current_props.get('version.fix', 0)}.{current_props.get('version.build', 0)}"
    cur_code = current_props.get("version.code", "4200")
    print(f"Current recorded version: {cur_version_str} (versionCode: {cur_code})")

    major, feature, improvement, fix, build, next_code, next_version_str = compute_next_version(
        current_props, args.type, args.version
    )
    print(f"Target testing version : {next_version_str} (versionCode: {next_code})")

    apk_name, source_zip_name, backup_zip_name = check_collisions(PARENT_DIR, next_version_str)
    print(f"Target artifacts in {PARENT_DIR}:")
    print(f"  1. APK:        {apk_name}")
    print(f"  2. Source ZIP: {source_zip_name}")
    print(f"  3. Backup ZIP: {backup_zip_name}")

    if args.dry_run:
        print("\n[DRY RUN] Collision check passed. No files modified.")
        return

    # Transactional Update of version.properties
    write_version_props(VERSION_FILE, major, feature, improvement, fix, build, next_code)
    print(f"Updated {VERSION_FILE.name} to {next_version_str}.")

    # Temp Staging Directory
    temp_dir = Path(tempfile.mkdtemp(prefix="illeagle_pkg_"))
    try:
        # Step A: Run Gradle Build
        print("\nRunning Gradle build ('gradlew.bat app:assembleDebug')...")
        env = os.environ.copy()
        if not env.get("JAVA_HOME") or not os.path.exists(env.get("JAVA_HOME", "")):
            possible_jbr = Path(r"C:\Program Files\Android\Android Studio\jbr")
            if possible_jbr.exists():
                env["JAVA_HOME"] = str(possible_jbr)

        env["ANDROID_USER_HOME"] = r"C:\Users\mlcri\.android"
        env.pop("ANDROID_PREFS_ROOT", None)
        env.pop("ANDROID_SDK_HOME", None)

        cmd = ["cmd.exe", "/c", "gradlew.bat", "app:assembleDebug"]
        res = subprocess.run(cmd, cwd=PROJECT_DIR, env=env, capture_output=True, text=True)
        if res.returncode != 0:
            print(f"ERROR: Gradle build failed! Exit code: {res.returncode}")
            print(res.stdout[-1000:] if res.stdout else "")
            print(res.stderr[-1000:] if res.stderr else "")
            raise RuntimeError("Gradle build failed.")

        expected_apk = OUTPUT_APK_DIR / f"IllEagle_{next_version_str}-debug.apk"
        if not expected_apk.exists():
            raise FileNotFoundError(f"Expected compiled APK not found at {expected_apk}")

        # Step B: Copy APK to temp
        temp_apk = temp_dir / apk_name
        shutil.copy2(expected_apk, temp_apk)

        # Step C: Create Source ZIP in temp
        print("Packaging clean Source ZIP (excluding .git)...")
        temp_source_zip = temp_dir / source_zip_name
        create_source_zip(PROJECT_DIR, temp_source_zip)

        # Step D: Create Development Backup ZIP in temp
        print("Packaging full Development Backup ZIP (including .git repository & history)...")
        temp_backup_zip = temp_dir / backup_zip_name
        create_backup_zip(PROJECT_DIR, temp_backup_zip)

        # Step E: Validation
        print("\nValidating generated artifacts...")
        assert temp_apk.exists() and temp_apk.stat().st_size > 0, "APK missing or empty"
        assert temp_source_zip.exists() and temp_source_zip.stat().st_size > 0, "Source ZIP missing or empty"
        assert temp_backup_zip.exists() and temp_backup_zip.stat().st_size > 0, "Backup ZIP missing or empty"

        with zipfile.ZipFile(temp_source_zip, "r") as zf:
            source_has_git = any(n.startswith(".git/") or "/.git/" in n for n in zf.namelist())
            assert not source_has_git, "Source ZIP incorrectly contains .git"

        with zipfile.ZipFile(temp_backup_zip, "r") as zf:
            backup_has_git = any(n.startswith(".git/") for n in zf.namelist())
            assert backup_has_git, "Backup ZIP missing .git repository"

        # Step F: Publish to Parent Directory
        print(f"\nPublishing all 3 validated artifacts to {PARENT_DIR}...")
        final_apk = PARENT_DIR / apk_name
        final_source = PARENT_DIR / source_zip_name
        final_backup = PARENT_DIR / backup_zip_name

        shutil.copy2(temp_apk, final_apk)
        shutil.copy2(temp_source_zip, final_source)
        shutil.copy2(temp_backup_zip, final_backup)

        print("\n==================================================")
        print("  SUCCESSFULLY PACKAGED ILL EAGLE TESTING BUILD!")
        print("==================================================")
        print(f"Version:      {next_version_str}")
        print(f"versionCode:  {next_code}")
        print("Artifacts:")
        print(f"  1. APK:        {final_apk} ({final_apk.stat().st_size / (1024*1024):.2f} MB)")
        print(f"  2. Source ZIP: {final_source} ({final_source.stat().st_size / (1024*1024):.2f} MB)")
        print(f"  3. Backup ZIP: {final_backup} ({final_backup.stat().st_size / (1024*1024):.2f} MB)")
        print("\nNote: .git history is strictly preserved in backup ZIP and excluded from source ZIP.")

    except Exception as e:
        print("\n==================================================")
        print(f"  ERROR: PACKAGING FAILED ({e})")
        print("  Rolling back version.properties...")
        print("==================================================")
        write_version_props(
            VERSION_FILE,
            int(current_props.get("version.major", "0")),
            int(current_props.get("version.feature", "0")),
            int(current_props.get("version.improvement", "0")),
            int(current_props.get("version.fix", "0")),
            int(current_props.get("version.build", "0")),
            int(current_props.get("version.code", "4200"))
        )
        sys.exit(1)
    finally:
        shutil.rmtree(temp_dir, ignore_errors=True)

if __name__ == "__main__":
    main()
