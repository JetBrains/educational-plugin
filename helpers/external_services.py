import json
import logging
from typing import Optional, List, Dict, Union
from urllib.request import Request, urlopen
from urllib.parse import quote, urlencode


def commit_changes_to_educational_plugin(space_token: str, branch_name: str, commit_massage: str, changes: List[Dict]):
    branch = branch_name if branch_name.startswith("refs/heads/") else f"refs/heads/{branch_name}"

    data = {
        "baseCommit": "master",
        "targetBranch": branch,
        "commitMessage": commit_massage,
        "files": changes
    }
    response = make_request("https://jetbrains.team/api/http/projects/edu/repositories/educational-plugin/commit",
                            space_token, method="POST", data=data)
    if not response["success"]:
        raise Exception(response["message"])


def create_review_in_educational_plugin(space_token: str, source_branch: str, title: str, review_username: str):
    data = {
        "repository": "educational-plugin",
        "sourceBranch": source_branch,
        "targetBranch": "master",
        "title": title
    }

    make_review_response = make_request("https://jetbrains.team/api/http/projects/key:EDU/code-reviews/merge-requests",
                                        space_token, method="POST", data=data)
    review_number = make_review_response["number"]
    make_request(f"https://jetbrains.team/api/http/projects/key:EDU/code-reviews/number:{review_number}/participants/username:{review_username}",
                 space_token, method="POST", data={"role": "Reviewer"})


def has_branch(space_token: str, branch_name: str) -> bool:
    encoded_branch = quote(f"refs/heads/{branch_name}")
    url = f"https://jetbrains.team/api/http/projects/key:EDU/repositories/educational-plugin/heads?pattern={encoded_branch}"
    response = make_request(url, space_token, method="GET", data=None)
    return len(response["data"]) != 0


def get_youtrack_issues(youtrack_token: str, tag: str, text_query: str) -> List[Dict]:
    query_params = urlencode({
        "fields": "summary,numberInProject,customFields(name,value(name))",
        "customFields": "Assignee",
        "query": f"tag: {{{tag}}} project: EDU {text_query}"
    }, quote_via=quote)
    url = f"https://youtrack.jetbrains.com/api/issues/?{query_params}"
    return make_request(url, youtrack_token, "GET")


def full_platform_version(platform_version: int) -> str:
    """
    Converts short platform version into full one.
    I.e. 223 -> 2022.3
    """
    return f"20{platform_version // 10}.{platform_version % 10}"


class YoutrackIssue:
    def __init__(self, issue_number: int, assignee_name: str):
        self.issue_number = issue_number
        self.assignee_name = assignee_name


DEFAULT_REVIEWER = "Arseniy.Pendryak"


def get_reviewer(issue: Optional[YoutrackIssue]) -> str:
    if not issue:
        return DEFAULT_REVIEWER
    # More heuristic solution than 100% reliable way
    # but it seems it works for all possible cases for now.
    return issue.assignee_name.replace(" ", ".")

def get_youtrack_issue(youtrack_token: str, tag: str, platform_version: int) -> Optional[YoutrackIssue]:
    issues = get_youtrack_issues(youtrack_token, tag, str(platform_version))
    if not issues:
        issues = get_youtrack_issues(youtrack_token, tag, full_platform_version(platform_version))

    if not issues:
        return None

    # Expected json format:
    # ```
    # [
    #   {
    #     "summary" : "%summary_text%",
    #     "numberInProject" : "%issue_number%",
    #     "customFields" : [
    #       {
    #         "value" : {
    #           "name" : "%user_name%",
    #           "$type" : "User"
    #         },
    #         "name" : "Assignee",
    #         "$type" : "SingleUserIssueCustomField"
    #       }
    #     ],
    #     "$type" : "Issue"
    #   }
    # ]
    # ```
    issue_number: int = issues[0]["numberInProject"]
    assignee_name: str = issues[0]["customFields"][0]["value"]["name"]
    return YoutrackIssue(issue_number, assignee_name)


def make_request(url: str, token: str, method: str, data: Optional[Dict] = None) -> Union[Dict, List]:
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
