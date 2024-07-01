import argparse
import base64
import glob
import os
import re
from enum import Enum
from functools import reduce
from pathlib import Path
from typing import Dict, TypeVar, Tuple, Optional

from external_services import commit_changes_to_educational_plugin, create_review_in_educational_plugin, get_reviewer, has_branch

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
        **process_branch_directories(platform_version),
        **process_run_configurations(platform_version)
    }
    current_dir = Path(".").absolute()
    return {path.absolute().relative_to(current_dir): value for (path, value) in changes.items()}


PLATFORM_DIR_REGEX = re.compile(r"\d{3}")


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


def process_run_configurations(platform_version: int) -> Changes:
    changes = {}
    run_configurations_dir = Path(".idea/runConfigurations")
    for file in glob.glob("*.xml", root_dir=run_configurations_dir):
        path = run_configurations_dir / file
        with open(path, "r") as f:
            text = f.read()
        # Removes `<log_file />` entity from run configurations related to given `platform_version`.
        # `MULTILINE` mode is used to match only one line.
        #
        # Note, removing xml item is intentionally done via regexp instead of xml parsing to preserve formatting
        new_text = re.sub(f"^\s*<log_file.*path=\".*?sandbox-{platform_version}.*?\".*/>\s*$\n", "", text,
                          flags=re.MULTILINE)
        if text != new_text:
            changes[path] = (FileModification.Change, new_text.encode())

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


def commit_changes(space_token: str, platform_version: int, changes: Changes):
    files = []
    for path, (modification, content) in changes.items():
        if modification == FileModification.Delete:
            files.append({
                "path": str(path),
                "content": {"className": "GitFileContent.Deleted"}
            })
        elif modification == FileModification.Change:
            base64_content_value = base64.b64encode(content).decode()
            files.append({
                "path": str(path),
                "content": {"className": "GitFileContent.Base64", "value": base64_content_value}
            })

    commit_changes_to_educational_plugin(
        space_token=space_token,
        branch_name=f"refs/heads/{branch(platform_version)}",
        commit_massage=f"Drop {platform_version} support",
        changes=files
    )


def branch(platform_version: int) -> str:
    return f"drop-{platform_version}"


def create_review(space_token: str, youtrack_token: str, platform_version: int):
    reviewer = get_reviewer(youtrack_token, "edu: drop platform", platform_version)
    create_review_in_educational_plugin(space_token, branch(platform_version), f"Drop support for {platform_version} platform", reviewer)


def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument("--space_token", type=str, required=True, help="Space token")
    parser.add_argument("--youtrack_token", type=str, required=True, help="YouTrack token")
    parser.add_argument("--platform_version", type=int, required=True, help="Major version of IntelliJ platform")
    return parser.parse_args()


def main():
    args = parse_args()

    platform_version = args.platform_version

    branch_name = branch(platform_version)
    if has_branch(args.space_token, branch_name):
        print(f"{branch_name} already exists")
        return

    changes = collect_changes(platform_version)
    commit_changes(args.space_token, platform_version, changes)
    create_review(args.space_token, args.youtrack_token, platform_version)


if __name__ == '__main__':
    main()
