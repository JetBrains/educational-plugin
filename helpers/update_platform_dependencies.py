import argparse
import json
import logging
import os
import subprocess
import sys
from typing import Optional
from urllib.parse import quote
from urllib.request import Request, urlopen


def gradle_property_path(platform_version: int) -> str:
    return f"gradle-{platform_version}.properties"


def read_gradle_property_text(platform_version: int) -> str:
    with open(gradle_property_path(platform_version)) as f:
        return f.read()


def make_request(url: str, token: str, method: str, data: Optional[dict]) -> dict:
    headers = {
        "Authorization": f"Bearer {token}",
        "Accept": "application/json",
        "Content-Type": "application/json"
    }
    if data:
        raw_data = json.dumps(data).encode()
    else:
        raw_data = None
    logging.debug(f" >> {method} {url}")
    response = urlopen(Request(url, raw_data, headers, method=method))
    response_str = response.read().decode()
    if response_str:
        return json.loads(response_str)
    else:
        return {}


def has_branch(token: str, branch_name: str) -> bool:
    encoded_branch = quote(f"refs/heads/{branch_name}")
    url = f"https://jetbrains.team/api/http/projects/key:EDU/repositories/educational-plugin/heads?pattern={encoded_branch}"
    response = make_request(url, token, method="GET", data=None)
    return len(response["data"]) != 0


REVIEWER = "Arseniy.Pendryak"


def create_review(token: str, platform_version: int):
    data = {
        "repository": "educational-plugin",
        "sourceBranch": f"update-{platform_version}",
        "targetBranch": "master",
        "title": f"Update {platform_version} IDE and plugin dependencies"
    }

    make_review_response = make_request("https://jetbrains.team/api/http/projects/key:EDU/code-reviews/merge-requests",
                                        token, method="POST", data=data)
    review_number = make_review_response["number"]
    make_request(f"https://jetbrains.team/api/http/projects/key:EDU/code-reviews/number:{review_number}/participants/username:{REVIEWER}",
                 token, method="POST", data={"role": "Reviewer"})


def commit_changes(token: str, platform_version: int):
    branch_name = f"refs/heads/update-{platform_version}"
    data = {
        "baseCommit": "master",
        "targetBranch": branch_name,
        "commitMessage": f"Update {platform_version} IDE and plugin dependencies",
        "files": [
            {
                "path": gradle_property_path(platform_version),
                "content": {"className": "GitFileContent.Text", "value": read_gradle_property_text(platform_version)}
            }
        ]
    }
    response = make_request("https://jetbrains.team/api/http/projects/edu/repositories/educational-plugin/commit",
                            token, method="POST", data=data)
    if not response["success"]:
        raise Exception(response["message"])


def update_versions(updater_path: str, platform_version: int):
    java_home = os.getenv("JAVA_HOME")
    if java_home:
        java = f"{java_home}/bin/java"
    else:
        # Use java executable from path
        java = "java"
    if sys.platform == "win32":
        java += ".exe"

    args = [java, "-jar", updater_path, "--majorVersion", str(platform_version)]
    logging.debug(" " + " ".join(args))
    try:
        subprocess.run(args,
                       check=True,
                       stdout=subprocess.PIPE,
                       stderr=subprocess.PIPE)
    except subprocess.CalledProcessError as e:
        out = e.stderr.decode("utf-8")
        if out:
            print(out, file=sys.stderr)
        raise e


def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument("--token", type=str, required=True, help="Space token")
    parser.add_argument("--platform_version", type=int, required=True, help="Major version of IntelliJ platform")
    parser.add_argument("--updater_path", type=str, required=True, help="Path of version updater jar file")
    parser.add_argument("--log", type=str, required=False, help="Log level")
    return parser.parse_args()


def main():
    args = parse_args()
    log_level = args.log
    if log_level:
        logging.basicConfig(level=log_level.upper())

    token = args.token
    platform_version = args.platform_version

    branch_name = f"update-{platform_version}"
    if has_branch(token, branch_name):
        print(f"{branch_name} already exists")
        return

    old_text = read_gradle_property_text(platform_version)
    update_versions(args.updater_path, platform_version)
    new_text = read_gradle_property_text(platform_version)

    if old_text == new_text:
        print("Everything is up-to-date")
        return

    commit_changes(token, platform_version)
    create_review(token, platform_version)


if __name__ == '__main__':
    main()
