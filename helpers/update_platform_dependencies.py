import argparse
import logging
import os
import subprocess
import sys

from external_services import commit_changes_to_educational_plugin, create_review_in_educational_plugin, has_branch, get_reviewer, \
    get_youtrack_issue


def gradle_property_path(platform_version: int) -> str:
    return f"gradle-{platform_version}.properties"


def read_gradle_property_text(platform_version: int) -> str:
    with open(gradle_property_path(platform_version)) as f:
        return f.read()


def create_review(space_token: str, youtrack_token: str, platform_version: int):
    issue = get_youtrack_issue(youtrack_token, "edu: support platform", platform_version)
    reviewer = get_reviewer(issue)
    create_review_in_educational_plugin(
        space_token=space_token,
        source_branch=f"update-{platform_version}",
        title=f"Update {platform_version} IDE and plugin dependencies",
        review_username=reviewer
    )


def commit_changes(space_token: str, platform_version: int):
    commit_changes_to_educational_plugin(
        space_token=space_token,
        branch_name=f"refs/heads/update-{platform_version}",
        commit_massage=f"Update {platform_version} IDE and plugin dependencies",
        changes=[
            {
                "path": gradle_property_path(platform_version),
                "content": {"className": "GitFileContent.Text", "value": read_gradle_property_text(platform_version)}
            }
        ]
    )


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
    parser.add_argument("--space_token", type=str, required=True, help="Space token")
    parser.add_argument("--youtrack_token", type=str, required=True, help="YouTrack token")
    parser.add_argument("--platform_version", type=int, required=True, help="Major version of IntelliJ platform")
    parser.add_argument("--updater_path", type=str, required=True, help="Path of version updater jar file")
    parser.add_argument("--log", type=str, required=False, help="Log level")
    return parser.parse_args()


def main():
    args = parse_args()
    log_level = args.log
    if log_level:
        logging.basicConfig(level=log_level.upper())

    platform_version = args.platform_version

    branch_name = f"update-{platform_version}"
    if has_branch(args.space_token, branch_name):
        print(f"{branch_name} already exists")
        return

    old_text = read_gradle_property_text(platform_version)
    update_versions(args.updater_path, platform_version)
    new_text = read_gradle_property_text(platform_version)

    if old_text == new_text:
        print("Everything is up-to-date")
        return

    commit_changes(args.space_token, platform_version)
    create_review(args.space_token, args.youtrack_token, platform_version)


if __name__ == '__main__':
    main()
