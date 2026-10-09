#!/usr/bin/env bash
# Prepares the demo data that the SQL seed cannot create (files must go through the API
# to land in the uploads volume): a lesson resource and a student submission waiting for
# a correction. Idempotent: running it twice changes nothing.
#
# Prerequisites: the stack is running (docker compose up -d), curl, jq, python3.
# Usage (repo root):  ./scripts/demo-prepare.sh          [API=http://localhost:8080 by default]
set -euo pipefail

API="${API:-http://localhost:8080}"
PASSWORD='Honey2026!'
COURSE='Anglais professionnel — B1'
RESOURCE_TITLE='Vocabulaire — se présenter'
ASSIGNMENT='Rédiger un email de relance client'
HERE="$(cd "$(dirname "$0")" && pwd)"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

for tool in curl jq python3; do
  command -v "$tool" >/dev/null || { echo "« $tool » est requis." >&2; exit 1; }
done

say() { printf '\033[1;34m▸\033[0m %s\n' "$*"; }

# login EMAIL → prints the JWT
login() {
  curl -sf "$API/api/auth/login" -H 'Content-Type: application/json' \
    -d "$(jq -n --arg e "$1" --arg p "$PASSWORD" '{email: $e, password: $p}')" | jq -r .accessToken
}

# get TOKEN PATH → JSON (fails on HTTP error)
get() { curl -sf "$API$2" -H "Authorization: Bearer $1"; }

# id_by FIELD VALUE  (reads a JSON array on stdin)
id_by() { jq -r --arg f "$1" --arg v "$2" '.[] | select(.[$f] == $v) | .id' | head -n 1; }
id_by_order() { jq -r --argjson o "$1" '.[] | select(.displayOrder == $o) | .id' | head -n 1; }

curl -sf -o /dev/null "$API/api/courses" || { echo "API injoignable sur $API (docker compose up -d ?)" >&2; exit 1; }

TRAINER=$(login formateur@honeylms.test)
STUDENT=$(login etudiant@honeylms.test)

COURSE_ID=$(curl -sf "$API/api/courses" | id_by title "$COURSE")
[ -n "$COURSE_ID" ] || { echo "Cours « $COURSE » introuvable : le jeu de démo est-il chargé (profil dev) ?" >&2; exit 1; }
MODULE1=$(get "$TRAINER" "/api/courses/$COURSE_ID/modules" | id_by_order 1)
MODULE2=$(get "$TRAINER" "/api/courses/$COURSE_ID/modules" | id_by_order 2)
LESSON_1_1=$(get "$TRAINER" "/api/modules/$MODULE1/lessons" | id_by_order 1)
LESSON_2_3=$(get "$TRAINER" "/api/modules/$MODULE2/lessons" | id_by_order 3)

# 1. Resource on « Se présenter » (uploaded by the trainer)
if [ -n "$(get "$TRAINER" "/api/lessons/$LESSON_1_1/resources" | id_by title "$RESOURCE_TITLE")" ]; then
  say "Ressource « $RESOURCE_TITLE » : déjà présente"
else
  python3 "$HERE/make-pdf.py" "$TMP/vocabulaire-se-presenter.pdf" "$RESOURCE_TITLE" \
    "Hello, my name is ... and I work as a ... at ..." \
    "I am in charge of ... / I report to ..." \
    "Our company specialises in ... We have ... employees." \
    "Nice to meet you. / Pleased to meet you."
  curl -sf -o /dev/null "$API/api/lessons/$LESSON_1_1/resources" -H "Authorization: Bearer $TRAINER" \
    -F "title=$RESOURCE_TITLE" -F "file=@$TMP/vocabulaire-se-presenter.pdf;type=application/pdf"
  say "Ressource « $RESOURCE_TITLE » : ajoutée"
fi

# 2. Camille's submission on the English assignment (left « à corriger » for the live demo)
ASSIGNMENT_ID=$(get "$TRAINER" "/api/lessons/$LESSON_2_3/assignments" | id_by title "$ASSIGNMENT")
STATUS=$(curl -s -o /dev/null -w '%{http_code}' "$API/api/assignments/$ASSIGNMENT_ID/submissions/me" \
  -H "Authorization: Bearer $STUDENT")
if [ "$STATUS" = "200" ]; then
  say "Dépôt de Camille sur « $ASSIGNMENT » : déjà présent"
else
  python3 "$HERE/make-pdf.py" "$TMP/relance-client-camille.pdf" "Follow-up email — Camille Martin" \
    "Subject: Follow-up on our quote of 2 October" \
    "Dear Mr Smith," \
    "I hope you are well. I am following up on the quote we sent you last week." \
    "Please let me know if you have any questions." \
    "Kind regards, Camille Martin"
  curl -sf -o /dev/null "$API/api/assignments/$ASSIGNMENT_ID/submissions" -H "Authorization: Bearer $STUDENT" \
    -F "file=@$TMP/relance-client-camille.pdf;type=application/pdf"
  say "Dépôt de Camille sur « $ASSIGNMENT » : ajouté (à corriger)"
fi

say "Démo prête. Voir docs/DEMO.md pour le scénario."
