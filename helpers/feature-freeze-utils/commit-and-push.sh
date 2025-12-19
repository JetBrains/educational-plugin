#!/bin/bash
set -e

# Usage: commit-and-push.sh <branch> <commit_message>

BRANCH="$1"
COMMIT_MESSAGE="$2"

if [ -z "$BRANCH" ] || [ -z "$COMMIT_MESSAGE" ]; then
  echo "Usage: $0 <branch> <commit_message>"
  exit 1
fi

# If nothing is staged, do nothing
if git diff --cached --quiet; then
  echo "No staged changes to commit"
  exit 0
fi

git commit -m "$COMMIT_MESSAGE"
git push origin "$BRANCH"
