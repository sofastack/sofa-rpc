#!/bin/bash
set -eEuo pipefail

BASEDIR=$(dirname "$0")
cd "${BASEDIR}"

git_status="$(git status --untracked-files=no --porcelain)"

# make sure git has no un commit files
if [ -n "$git_status" ]; then
  echo "Please commit your change before run this shell, un commit files:"
  echo "$git_status"
  exit 1
fi
