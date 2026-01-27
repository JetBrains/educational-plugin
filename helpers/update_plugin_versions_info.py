import argparse
import os
import sys
import re

from external_services import commit_changes_to_educational_plugin

"""
    Adds new version to PluginVersionsInfo.md, commits and pushes changes to Space.

    Arguments:
        --release_version: str
            Specifies the release version in a format such as '2026.1'. This argument is 
            required.
        --space_token: str
            Represents the token used for Space authentication. This argument is required.
        --branch: str
            Indicates the target branch name to which changes should be committed. This 
            argument is required.
        --commit_message: Optional[str], default is 'Updated PluginVersionsInfo.md'
            Provides a custom commit message to be used when committing changes.
    """

def parse_args():
    parser = argparse.ArgumentParser(description="Update and commit changes in PluginVersionsInfo.md to Space")
    parser.add_argument("--release_version", type=str, required=True, help="Release version (e.g., 2026.1)")
    parser.add_argument("--space_token", type=str, required=True, help="Space token")
    parser.add_argument("--branch", type=str, required=True, help="Branch name to commit to")
    parser.add_argument("--commit_message", type=str, default="Updated PluginVersionsInfo.md", help="Commit message")
    return parser.parse_args()

def extract_version(file_path: str, constant_name: str) -> str | None:
    if not os.path.exists(file_path):
        print(f"Error: File not found at {file_path}")
        return None

    with open(file_path, "r") as f:
        content = f.read()

    # Regex to match Kotlin constant definitions like:
    # const val JSON_FORMAT_VERSION: Int = 22
    # val CURRENT_YAML_VERSION = 5
    regex = rf"(?:const\s+val|val)\s+{constant_name}\s*(?::\s*Int)?\s*=\s*(\d+)"
    match = re.search(regex, content)

    if match:
        return match.group(1)
    return None

def update_plugin_versions_info(
    markdown_path: str,
    edu_versions_path: str,
    yaml_mapper_path: str,
    plugin_version: str
) -> bool:
    # Extract JSON_FORMAT_VERSION from EduVersions.kt
    json_version = extract_version(edu_versions_path, "JSON_FORMAT_VERSION")
    if json_version is None:
        print("Error: Could not extract JSON_FORMAT_VERSION from EduVersions.kt")
        return False

    # Extract CURRENT_YAML_VERSION from YamlMapper.kt
    yaml_version = extract_version(yaml_mapper_path, "CURRENT_YAML_VERSION")
    if yaml_version is None:
        print("Error: Could not extract CURRENT_YAML_VERSION from YamlMapper.kt")
        return False

    if not os.path.exists(markdown_path):
        print(f"Error: {markdown_path} not found.")
        return False

    with open(markdown_path, "r") as f:
        content = f.read()

    table_header = "**Versions without EDU IDEs**"
    next_table_header = "**Versions with EDU IDEs**"

    table_start = content.find(table_header)
    if table_start == -1:
        print(f"Error: Could not find '{table_header}' in file")
        return False

    table_end = content.find(next_table_header, table_start)
    if table_end == -1:
        print(f"Error: Could not find '{next_table_header}' in file")
        return False

    before_table = content[:table_start]
    table_section = content[table_start:table_end]
    after_table = content[table_end:]
    lines = table_section.splitlines()

    # Check if plugin_version already exists in the table
    if any(f"{plugin_version}" in line for line in lines):
        print(f"Version {plugin_version} already exists in the table. Skipping update.")
        return True

    # Find the last table row
    insert_index = -1
    for i in range(len(lines) - 1, -1, -1):
        if lines[i].strip().startswith("|") and "|" in lines[i]:
            insert_index = i
            break

    if insert_index == -1:
        print("Error: Could not find table rows")
        return False

    # Expects row in format: | 2025.11   | 22   | 5    |
    columns = [col for col in lines[insert_index].split("|") if col][1:] # drop first empty string from split
    
    release_pad_end = 10
    json_pad_end = 5
    yaml_pad_end = 5
    
    if len(columns) >= 3:
        release_pad_end = len(columns[0]) - 1
        json_pad_end = len(columns[1]) - 1
        yaml_pad_end = len(columns[2]) - 1

    new_row = f"| {plugin_version.ljust(release_pad_end)}| {json_version.ljust(json_pad_end)}| {yaml_version.ljust(yaml_pad_end)}|"

    lines.insert(insert_index + 1, new_row)

    new_content = before_table + "\n".join(lines) + "\n" + after_table

    with open(markdown_path, "w") as f:
        f.write(new_content)

    print(f"Successfully added version {plugin_version} (Json={json_version}, Yaml={yaml_version}) to the table")
    return True

def get_changes(file_path: str) -> list:
    if not os.path.exists(file_path):
        print(f"Error: {file_path} not found.")
        sys.exit(1)

    with open(file_path, "r") as f:
        content = f.read()

    changes = [
        {
            "path": file_path,
            "content": {"className": "GitFileContent.Text", "value": content}
        }
    ]
    return changes

def main():
    args = parse_args()

    markdown_path = "documentation/PluginVersionsInfo.md"
    edu_versions_path = "edu-format/src/com/jetbrains/edu/learning/courseFormat/EduVersions.kt"
    yaml_mapper_path = "edu-format/src/com/jetbrains/edu/learning/yaml/YamlMapper.kt"

    success = update_plugin_versions_info(
        markdown_path=markdown_path,
        edu_versions_path=edu_versions_path,
        yaml_mapper_path=yaml_mapper_path,
        plugin_version=args.release_version
    )

    if not success:
        sys.exit(1)

    changes = get_changes(markdown_path)

    commit_changes_to_educational_plugin(
        space_token=args.space_token,
        branch_name=args.branch,
        commit_massage=args.commit_message,
        changes=changes
    )

if __name__ == "__main__":
    main()
