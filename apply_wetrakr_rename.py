#!/usr/bin/env python3
"""One-time repository-wide WeTrakr rename helper. Run from repository root.

It updates text files under the project, then moves the Kotlin package folder from
com/nuvio/simkl/android to com/nuvio/wetrakr/android. Make a branch/backup first.
"""
from pathlib import Path
import shutil

ROOT = Path.cwd()
SKIP_PARTS = {".git", ".gradle", "build", ".idea"}
REPLACEMENTS = [
    ("com.nuvio.simkl.android", "com.nuvio.wetrakr.android"),
    ("Simkl", "WeTrakr"),
    ("simkl", "wetrakr"),
    ("SIMKL", "WETRAKR"),
]
TEXT_SUFFIXES = {".kt", ".kts", ".xml", ".yml", ".yaml", ".md", ".txt", ".json", ".properties", ".gradle", ".pro", ".cpp", ".h", ".html", ".js"}
for path in ROOT.rglob("*"):
    if not path.is_file() or any(part in SKIP_PARTS for part in path.parts):
        continue
    if path.suffix.lower() not in TEXT_SUFFIXES and path.name not in {"Dockerfile", "Makefile"}:
        continue
    try:
        original = path.read_text(encoding="utf-8")
    except (UnicodeDecodeError, OSError):
        continue
    updated = original
    for old, new in REPLACEMENTS:
        updated = updated.replace(old, new)
    if updated != original:
        path.write_text(updated, encoding="utf-8")
old_dir = ROOT / "app/src/main/java/com/nuvio/simkl/android"
new_dir = ROOT / "app/src/main/java/com/nuvio/wetrakr/android"
if old_dir.exists():
    new_dir.mkdir(parents=True, exist_ok=True)
    for source in old_dir.iterdir():
        target = new_dir / source.name
        if source.is_file():
            if not target.exists():
                shutil.move(str(source), str(target))
            else:
                source.unlink()
        elif source.is_dir():
            if not target.exists():
                shutil.move(str(source), str(target))
            else:
                raise SystemExit(f"Resolve duplicate directory manually: {source} -> {target}")
    shutil.rmtree(old_dir, ignore_errors=True)
    for parent in [old_dir.parent, old_dir.parent.parent, old_dir.parent.parent.parent]:
        try: parent.rmdir()
        except OSError: pass
print("WeTrakr naming migration complete. Review changes and run the Android build workflow.")
Path(__file__).unlink(missing_ok=True)  # Do not leave old-brand migration strings in the finished repository.
