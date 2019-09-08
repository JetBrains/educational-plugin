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
    parser.add_argument("--version", type=str, required=True, help="Android Studio version (e.g. 3.4.0.18)")
    parser.add_argument("--buildVersion", type=str, required=True, help="Android Studio build version (e.g. 183.5452501)")
    args = parser.parse_args()

    for o in ["mac", "linux", "windows"]:
        if o == "linux":
            archive_type = "tar.gz"
        else:
            archive_type = "zip"

        filename = f"android-studio-ide-{args.buildVersion}-{o}.{archive_type}"
        print(f"Download {filename}")
        with requests.get(f"https://dl.google.com/dl/android/studio/ide-zips/{args.version}/{filename}", stream=True) as r:
            if r.status_code == 200:
                download_file(r, filename)
            else:
                print(f"Can't download {filename}. {r}")
                continue

        print(f"Upload {filename} to https://repo.labs.intellij.net/edu-tools")
        with open(filename, 'rb') as data:
            requests.put(f"https://repo.labs.intellij.net/edu-tools/{filename}", data=data, headers={"X-JFrog-Art-Api": args.apiKey})

        os.remove(filename)


def download_file(request, filename):
    with open(filename, 'wb') as f:
        with tqdm(total=int(request.headers["Content-length"]), unit="B", unit_scale=True) as progress:
            for chunk in request.iter_content(chunk_size=8 * 1024):
                if chunk:  # filter out keep-alive new chunks
                    f.write(chunk)
                    f.flush()
                    progress.update(len(chunk))


if __name__ == '__main__':
    main()
