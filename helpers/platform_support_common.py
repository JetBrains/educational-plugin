"""Common utilities for dropping platform support."""
import os
import re
from enum import Enum
from functools import reduce
from pathlib import Path
from typing import Dict, TypeVar, Tuple, Optional

K = TypeVar('K')
V = TypeVar('V')


def dict_intersection(dict1: Dict[K, V], dict2: Dict[K, V]) -> Dict[K, V]:
    return {k: v for k, v in dict1.items() if dict2.get(k) == v}


class FileModification(Enum):
    Delete = 1
    Change = 2


Changes = Dict[Path, Tuple[FileModification, Optional[bytes]]]


def collect_changes(platform_version: int) -> Changes:
    changes: Changes = {
        **process_gradle_properties(platform_version),
        **process_branch_directories(platform_version)
    }
    current_dir = Path(".").absolute()
    return {path.absolute().relative_to(current_dir): value for (path, value) in changes.items()}


PLATFORM_DIR_REGEX = re.compile(r"\d{3}")

# Platform-specific files of these modules should never be moved into the common source set.
# For example, integration tests pin the IDE version to download for each platform
ALWAYS_PLATFORM_SPECIFIC_MODULES = {Path("intellij-plugin/integration-tests").absolute()}


def process_branch_directories(platform_version: int) -> Changes:
    branch_dirs = []
    for root, dirs, files in os.walk("."):
        if "branches" in dirs:
            branch_dirs.append(Path(root).absolute() / "branches")

    changes = {}
    for branch_dir in branch_dirs:
        # Delete all files related to dropping platform
        old_platform_dir = branch_dir / str(platform_version)
        changes.update({path: (FileModification.Delete, None) for path in old_platform_dir.rglob('*') if path.is_file()})

        if branch_dir.parent in ALWAYS_PLATFORM_SPECIFIC_MODULES:
            continue

        # Try to find files with the same content and move them to common module directory
        sub_dirs = [child for child in branch_dir.glob("*") if
                    child.is_dir() and PLATFORM_DIR_REGEX.match(child.name) and not child.name == str(platform_version)]
        file_dicts = []
        for sub_dir in sub_dirs:
            file_dicts.append({f.relative_to(sub_dir): f.read_bytes() for f in sub_dir.rglob('*') if f.is_file()})
        # It's possible collect nothing. For example, because `branch_dir` was a part of `.git` directory
        # in this case, just continue. Otherwise, `reduce` will fail
        if not file_dicts:
            continue
        same_files = reduce(dict_intersection, file_dicts)

        main_dir = branch_dir.parent
        for (relative_path, content) in same_files.items():
            path_in_main_module = main_dir / relative_path
            # It's possible to have a file with the same name in the main module,
            # and we don't want to override it because it most likely will break compilation
            if not path_in_main_module.exists():
                # Add file to main directory
                changes[path_in_main_module] = (FileModification.Change, content)
                # Delete all files from remaining platform-specific directories
                for sub_dir in sub_dirs:
                    changes[sub_dir / relative_path] = (FileModification.Delete, None)

    return changes


def process_gradle_properties(platform_version: int) -> Changes:
    with open("gradle.properties", "r") as f:
        text = f.read()
    # For example, if `platform_to_drop` is `231`
    # supported values: 231, 232, 233 -> supported values: 232, 233
    new_gradle_properties_text = re.sub(f"supported values: {platform_version},", "supported values:", text)

    return {
        Path(f"gradle-{platform_version}.properties"): (FileModification.Delete, None),
        Path("gradle.properties"): (FileModification.Change, new_gradle_properties_text.encode())
    }
