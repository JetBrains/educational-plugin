#!/bin/bash
set -e

RELEASE_BRANCH="$1"

if [ -z "RELEASE_BRANCH" ]; then
    echo "Error: RELEASE_BRANCH argument is required"
    exit 1
fi

# Check if branch already exists (locally or remotely)
if git show-ref --verify --quiet "refs/heads/$RELEASE_BRANCH" || \
   git ls-remote --exit-code --heads origin "$RELEASE_BRANCH" > /dev/null 2>&1; then
    echo "Branch $RELEASE_BRANCH already exists."
    exit 1
fi

git config user.email "teamcity@jetbrains.com"
git config user.name "TeamCity"

git checkout -b "$RELEASE_BRANCH"
git push origin "$RELEASE_BRANCH"
