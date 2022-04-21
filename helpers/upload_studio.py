#!/usr/bin/env python3

"""
Required libraries:
    * argparse https://pypi.org/project/argparse/
    * requests https://pypi.org/project/requests/
    * tqdm https://pypi.org/project/tqdm/
"""

import argparse
import os
import requests
from tqdm import tqdm


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--apiKey", type=str, required=True, help="API key to authenticate at https://repo.labs.intellij.net. Can be "
                                                                  "created and found https://repo.labs.intellij.net/webapp/#/profile")
    parser.add_argument("--version", type=str, required=True, help="Android Studio version (e.g. 2020.3.1.4)")
    args = parser.parse_args()

    for o in ["mac", "linux", "windows"]:
        if o == "linux":
            archive_type = "tar.gz"
        else:
            archive_type = "zip"

        filename = f"android-studio-{args.version}-{o}.{archive_type}"
        print(f"Download {filename}")
        with requests.get(f"https://dl.google.com/dl/android/studio/ide-zips/{args.version}/{filename}", stream=True) as r:
            if r.status_code == 200:
                download_file(r, filename)
            else:
                print(f"Can't download {filename}. {r}")
                continue

        print(f"Upload {filename} to https://repo.labs.intellij.net/edu-tools")
        upload_file(filename, args.apiKey)

        os.remove(filename)


def download_file(request, filename):
    import shutil

    file_size = int(request.headers["Content-length"])
    with open(filename, 'wb') as f:
        with tqdm.wrapattr(request.raw, "read", total=file_size) as wrapped_data:
            shutil.copyfileobj(wrapped_data, f)
    print()


def upload_file(filename, api_key):
    file_size = os.stat(filename).st_size
    with open(filename, "rb") as f:
        with tqdm.wrapattr(f, "read", total=file_size) as wrapped_file:
            requests.put(f"https://repo.labs.intellij.net/edu-tools/{filename}", data=wrapped_file, headers={"X-JFrog-Art-Api": api_key})
    print()


if __name__ == '__main__':
    main()
