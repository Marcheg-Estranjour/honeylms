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

# Target: ng serve (default) or the Docker front, e.g. E2E_BASE_URL=http://127.0.0.1:8000
BASE_URL="${E2E_BASE_URL:-http://localhost:4200}"
if ! curl -sf -o /dev/null "$BASE_URL"; then
  echo "Rien ne répond sur $BASE_URL : lancez d'abord « npm start » (ou « docker compose up -d » pour :8000)." >&2
  exit 1
fi

# With --network host, "localhost" may resolve to ::1 (IPv6) inside the container, and some POST
# requests to the port published by Docker then never get an answer (seen on register and on a
# refused login). Prefer IPv4 when the same URL answers on 127.0.0.1.
IPV4_URL="${BASE_URL/\/\/localhost/\/\/127.0.0.1}"
if [[ "$IPV4_URL" != "$BASE_URL" ]] && curl -sf -o /dev/null "$IPV4_URL"; then
  echo "E2E : $BASE_URL remplacé par $IPV4_URL (IPv4, évite la résolution IPv6 de localhost dans le conteneur)."
  BASE_URL="$IPV4_URL"
fi

# --network host : the container reaches ng serve (and, through its proxy, the backend) on localhost.
# --user + HOME   : result files belong to you, not to root.
docker run --rm --network host --ipc=host \
  --user "$(id -u):$(id -g)" -e HOME=/tmp -e E2E_BASE_URL="$BASE_URL" \
  -v "$PWD":/work -w /work \
  "mcr.microsoft.com/playwright:v${VERSION}-noble" \
  npx playwright test "$@"
