import argparse
import base64
from typing import Optional

from external_services import commit_changes_to_educational_plugin, create_review_in_educational_plugin, get_reviewer, has_branch, \
    get_youtrack_issue, YoutrackIssue
from platform_support_common import collect_changes, FileModification, Changes


def commit_changes(space_token: str, platform_version: int, issue: Optional[YoutrackIssue], changes: Changes):
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

    if issue:
        commit_message = f"EDU-{issue.issue_number}: Drop {platform_version} support"
    else:
        commit_message = f"Drop {platform_version} support"

    commit_changes_to_educational_plugin(
        space_token=space_token,
        branch_name=f"refs/heads/{branch(platform_version)}",
        commit_massage=commit_message,
        changes=files
    )


def branch(platform_version: int) -> str:
    return f"drop-{platform_version}"


def create_review(space_token: str, platform_version: int, issue: Optional[YoutrackIssue]):
    reviewer = get_reviewer(issue)
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

    issue = get_youtrack_issue(args.youtrack_token, "edu: drop platform", platform_version)
    changes = collect_changes(platform_version)
    commit_changes(args.space_token, platform_version, issue, changes)
    create_review(args.space_token, platform_version, issue)


if __name__ == '__main__':
    main()
