import argparse

from external_services import create_branch, has_branch

"""
    Creates a new branch from the latest master in Space.

    Arguments:
        --branch_name (str): Specifies the name of the branch to be created. This argument is required.
        --space_token (str): The token for authenticating with Space. This argument is required.
"""

def parse_args():
    parser = argparse.ArgumentParser(description="Creates new branch from latest master")
    parser.add_argument("--branch_name", type=str, required=True, help="Name of the branch to be created")
    parser.add_argument("--space_token", type=str, required=True, help="Space token")
    return parser.parse_args()

def main():
    args = parse_args()

    branch_name = args.branch_name
    space_token = args.space_token

    if has_branch(space_token=space_token, branch_name=branch_name):
        print(f"Branch '{branch_name}' already exists. Nothing to do")
        return

    create_branch(space_token=space_token, branch_name=branch_name)

    print(f"Successfully created new branch '{branch_name}' from master")

if __name__ == "__main__":
    main()
