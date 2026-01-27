import argparse
import os
import re
import sys

from external_services import commit_changes_to_educational_plugin

"""
   Updates the pluginVersion in the gradle.properties file 
   and commits the changes to a specified branch in Space.

   Arguments:
       --new_version (str): The new plugin version (e.g., "2026.1-2025.3-123"). This argument is 
       required.
       --space_token (str): The token for authenticating with Space. This argument is required.
       --branch (str): The name of the branch to which the changes will be committed. This 
       argument is required.
       --commit_message (str): The commit message to use when committing to Space. This argument 
       is optional and defaults to "Updated pluginVersion".
   """

PLUGIN_VERSION_PATTERN = r"^\s*pluginVersion\s*=\s*(.*)"

def parse_args():
    parser = argparse.ArgumentParser(description="Update pluginVersion in gradle.properties and commit to Space")
    parser.add_argument("--new_version", type=str, required=True, help="New plugin version (e.g., 2026.1-2025.3-123)")
    parser.add_argument("--space_token", type=str, required=True, help="Space token")
    parser.add_argument("--branch", type=str, required=True, help="Branch name to commit to")
    parser.add_argument("--commit_message", type=str, default="Updated pluginVersion", help="Commit message")
    return parser.parse_args()

def get_plugin_version(file_path: str) -> str | None:
    if not os.path.exists(file_path):
        print(f"Error: {file_path} not found.")
        return None
    
    with open(file_path, "r") as f:
        for line in f:
            match = re.match(PLUGIN_VERSION_PATTERN, line)
            if match:
                return match.group(1).strip()
    return None

def update_plugin_version(file_path: str, new_version: str) -> bool:
    if not os.path.exists(file_path):
        print(f"Error: {file_path} not found.")
        return False

    with open(file_path, "r") as f:
        lines = f.readlines()

    found = False
    new_lines = []
    for line in lines:
        match = re.match(PLUGIN_VERSION_PATTERN, line)
        if match:
            new_lines.append(re.sub(PLUGIN_VERSION_PATTERN, rf"\g<1>{new_version}", line))
            found = True
        else:
            new_lines.append(line)

    if not found:
        # Ensure it ends with a newline if we add a new one.
        if new_lines and not new_lines[-1].endswith("\n"):
            new_lines[-1] += "\n"
        new_lines.append(f"pluginVersion={new_version}\n")

    with open(file_path, "w") as f:
        f.writelines(new_lines)

    return True

def get_changes(file_path: str) -> list:
    with open(file_path, "r") as f:
        content = f.read()

    return [
        {
            "path": file_path,
            "content": {"className": "GitFileContent.Text", "value": content}
        }
    ]

def main():
    args = parse_args()
    gradle_properties_path = "gradle.properties"

    current_version = get_plugin_version(gradle_properties_path)
    if current_version == args.new_version:
        print(f"pluginVersion is already set to {args.new_version}, nothing to do")
        return

    if not update_plugin_version(gradle_properties_path, args.new_version):
        sys.exit(1)

    print(f"Successfully updated pluginVersion to {args.new_version} in {gradle_properties_path}")

    changes = get_changes(gradle_properties_path)

    commit_changes_to_educational_plugin(
        space_token=args.space_token,
        branch_name=args.branch,
        commit_massage=args.commit_message,
        changes=changes
    )

if __name__ == "__main__":
    main()
