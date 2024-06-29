"""
This script verifies the icon mappings from an old UI to a new UI, as defined in a JSON file.
It checks if the icons specified in the JSON mappings actually exist on the disk and lists any inconsistencies.

Main steps:
1. Change directory to the working directory containing resources.
2. Read the JSON file containing icon mappings.
3. List all icon files in the specified directory, ignoring certain files.
4. Compare the list of files from the directory with the mappings from the JSON.
5. Report missing mappings (files present in directory but not in JSON) and 
   missing files (mappings present in JSON but files do not exist on disk).

Functions:
- get_all_mappings(_json_data, parent_key=""): Recursively retrieves all icon mappings from the JSON data.
- get_json_data(): Reads JSON data from the specified JSON file.
- list_files(): Lists all files in the icon directory, ignoring specific files.
- Flat_map(f, xs): Utility function to flatten nested lists.

Usage:
Ensure the working directory, JSON file, and icon directory paths are correct.
Run the script to check the mappings and receive a report on any discrepancies.
"""

import json
import os

working_directory = "../intellij-plugin/educational-core/resources/"
os.chdir(working_directory)

# JSON file with icon mappings
json_file = "EduToolsIconMappings.json"
# Directory with icons
icons_directory = "icons/com/jetbrains/edu/"

# Files to ignore during the check, because they are already made for new UI
files_to_ignore = (
    "icon-robots.txt",
    "dot.svg",
    "ignoreSyncFile.svg",
    "guidedProject.svg",
    "guidedProjectSelected.svg",
    "simpleLesson.svg",
    "simpleLessonSelected.svg",
    "syncFilesModInfo.svg",
    "syncFilesModWarning.svg"
)


# Function to recursively get all mappings from the JSON data
def get_all_mappings(_json_data, parent_key=""):
    _mappings = {}
    if isinstance(_json_data, dict):
        for key, value in _json_data.items():
            full_key = os.path.join(parent_key, key)
            if isinstance(value, dict):
                _mappings.update(get_all_mappings(value, full_key))
            elif isinstance(value, list):
                for item in value:
                    _mappings[item.removeprefix(icons_directory)] = full_key
            else:
                _mappings[value.removeprefix(icons_directory)] = full_key
    return _mappings


# Function to read JSON data from the file
def get_json_data():
    with open(json_file, "r") as _json_file:
        data = json.load(_json_file)

    return data["icons"]["com"]["jetbrains"]["edu"]


# Function that lists all files in the icons directory, except ignored ones
def list_files():
    result = []
    root: str
    _icons = os.path.join(working_directory, icons_directory)
    for root, dirs, files in os.walk(icons_directory):
        if len(result) > 300:
            # Early stop if too many files
            break
        for _file in files:
            if _file.endswith("_dark.svg") or _file.endswith("@20x20.svg") or _file in files_to_ignore:
                continue
            result.append(os.path.relpath(os.path.join(root, _file), icons_directory))
    return result


# Utility function to flatten nested lists
def flat_map(f, xs):
    ys = []
    for x in xs:
        ys.extend(f(x))
    return ys


if __name__ == '__main__':
    json_data = get_json_data()  # Read JSON data
    mappings = get_all_mappings(json_data)  # Get all mappings from JSON data
    all_files_in_mapping = flat_map(lambda item: [item[0], item[1]], mappings.items())  # Flatten the mappings list
    files_in_dir = list_files()  # List all files in the icons directory

    missing_mappings = []
    missing_files = []
    for file in files_in_dir:
        if file not in all_files_in_mapping:
            missing_mappings.append(file)  # File not found in JSON mappings
        elif not os.path.exists(os.path.join(icons_directory, file)):
            missing_files.append(file)  # File listed in JSON but does not exist on disk

    if missing_mappings:
        print("The following files are missing in the JSON mappings:")
        for file in missing_mappings:
            print(file)

    if missing_files:
        print("\nThe following files have incorrect mappings in the JSON (files do not exist on disk):")
        for file in missing_files:
            print(file)

    if not missing_mappings and not missing_files:
        print("Everything is fine")  # All files and mappings are correct
