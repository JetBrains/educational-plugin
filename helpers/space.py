import json
import logging
from typing import Optional, List, Dict
from urllib.request import Request, urlopen
from urllib.parse import quote


def commit_changes_to_educational_plugin(token: str, branch_name: str, commit_massage: str, changes: List[Dict]):
    data = {
        "baseCommit": "master",
        "targetBranch": branch_name,
        "commitMessage": commit_massage,
        "files": changes
    }
    response = make_request("https://jetbrains.team/api/http/projects/edu/repositories/educational-plugin/commit",
                            token, method="POST", data=data)
    if not response["success"]:
        raise Exception(response["message"])


def create_review_in_educational_plugin(token: str, source_branch: str, title: str, review_username: str):
    data = {
        "repository": "educational-plugin",
        "sourceBranch": source_branch,
        "targetBranch": "master",
        "title": title
    }

    make_review_response = make_request("https://jetbrains.team/api/http/projects/key:EDU/code-reviews/merge-requests",
                                        token, method="POST", data=data)
    review_number = make_review_response["number"]
    make_request(f"https://jetbrains.team/api/http/projects/key:EDU/code-reviews/number:{review_number}/participants/username:{review_username}",
                 token, method="POST", data={"role": "Reviewer"})


def has_branch(token: str, branch_name: str) -> bool:
    encoded_branch = quote(f"refs/heads/{branch_name}")
    url = f"https://jetbrains.team/api/http/projects/key:EDU/repositories/educational-plugin/heads?pattern={encoded_branch}"
    response = make_request(url, token, method="GET", data=None)
    return len(response["data"]) != 0


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
