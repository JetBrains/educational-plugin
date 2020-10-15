#!/usr/bin/env python3

import argparse
import re
from typing import Pattern

import requests

AS_VERSION_PATTERN: Pattern[str] = re.compile(r"^\d{3}(\.\d+)?$")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--apiKey", type=str, required=True, help="API key to authenticate at https://repo.labs.intellij.net. Can be "
                                                                  "created and found https://repo.labs.intellij.net/webapp/#/profile")
    parser.add_argument("--version", type=str, required=True, help="Version of Android Studio that should be deleted."
                                                                   "It may be a major version like 193 or concrete version like 193.6085562")
    args = parser.parse_args()

    if not re.match(AS_VERSION_PATTERN, args.version):
        raise Exception(f"{args.version} is invalid version. Version should match `{AS_VERSION_PATTERN.pattern}` pattern")

    response = requests.get("https://repo.labs.intellij.net/api/storage/edu-tools", headers={"X-JFrog-Art-Api": args.apiKey}).json()
    children = response["children"]
    for child in children:
        folder: bool = child["folder"]
        uri: str = child["uri"]
        if not folder and uri.startswith(f"/android-studio-ide-{args.version}"):
            print(f"Delete {uri}")
            delete_response = requests.delete(f"https://repo.labs.intellij.net/edu-tools{uri}", headers={"X-JFrog-Art-Api": args.apiKey})
            if delete_response.status_code != 204:
                print(f"Failed to delete {uri}")


if __name__ == '__main__':
    main()
