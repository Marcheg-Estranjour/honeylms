#!/usr/bin/env bash
# Runs the Playwright E2E tests inside the official Playwright image (browsers included),
# for machines where Playwright cannot install its browsers (e.g. Ubuntu 20.04).
#
# Prerequisites, on the host:  docker compose up -d   (backend + DB)   and   npm start   (ng serve)
# Usage (from frontend/):      npm run e2e:docker    [-- extra playwright args, e.g. --headed is NOT available]
set -euo pipefail
cd "$(dirname "$0")/.."

# The image tag MUST match the installed @playwright/test version.
VERSION=$(node -p "require('./node_modules/@playwright/test/package.json').version")

if ! curl -sf -o /dev/null http://localhost:4200; then
  echo "ng serve ne répond pas sur http://localhost:4200 : lancez d'abord « npm start » dans un autre terminal." >&2
  exit 1
fi

# --network host : the container reaches ng serve (and, through its proxy, the backend) on localhost.
# --user + HOME   : result files belong to you, not to root.
docker run --rm --network host --ipc=host \
  --user "$(id -u):$(id -g)" -e HOME=/tmp \
  -v "$PWD":/work -w /work \
  "mcr.microsoft.com/playwright:v${VERSION}-noble" \
  npx playwright test "$@"
