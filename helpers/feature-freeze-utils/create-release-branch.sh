#!/bin/bash
set -e

RELEASE_BRANCH="$1"
TOKEN="$2"
ORIGINAL_REMOTE="$3"  # Pass %vcsroot.url% from TeamCity

if [ -z "RELEASE_BRANCH" ]; then
    echo "Error: RELEASE_BRANCH argument is required. Please pass the name of branch to be created as a first argument"
    exit 1
fi

if [ -z "TOKEN" ]; then
    echo "Error: TOKEN argument is required. Please use a Space Authorization Token as a second argument"
    exit 1
fi

if [ -z "$ORIGINAL_REMOTE" ]; then
    echo "Error: ORIGINAL_REMOTE argument is required. "
    exit 1
fi

# Convert SSH URL to HTTPS URL
# Handles formats like:
#   ssh://git@git.jetbrains.team/repo.git
#   git@github.com:user/repo.git
convert_to_https() {
    local url="$1"

    if [[ "$url" == ssh://* ]]; then
        # ssh://git@host/path -> https://host/path
        echo "$url" | sed -E 's|ssh://git@([^/]+)/|https://\1/|'
    elif [[ "$url" == git@* ]]; then
        # git@host:path -> https://host/path
        echo "$url" | sed -E 's|git@([^:]+):|https://\1/|'
    elif [[ "$url" == https://* ]]; then
        # Already HTTPS
        echo "$url"
    else
        echo "Error: Unsupported URL format: $url" >&2
        exit 1
    fi
}

HTTPS_REMOTE=$(convert_to_https "$ORIGINAL_REMOTE")

# Check if branch already exists (locally or remotely)
if git show-ref --verify --quiet "refs/heads/$RELEASE_BRANCH" || \
   git ls-remote --exit-code --heads origin "$RELEASE_BRANCH" > /dev/null 2>&1; then
    echo "Branch $RELEASE_BRANCH already exists."
    exit 1
fi


git config user.email "teamcity@jetbrains.com"
git config user.name "TeamCity"

git checkout -b "$RELEASE_BRANCH"

git remote set-url origin "$HTTPS_REMOTE"

temp="${HTTPS_REMOTE#https://}"
HOSTNAME="${temp%%/*}"
git -c "http.https://${HOSTNAME}/.extraheader=Authorization: Bearer $TOKEN" push origin "$RELEASE_BRANCH"

git remote set-url origin "$ORIGINAL_REMOTE"