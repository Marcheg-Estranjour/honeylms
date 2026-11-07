#!/usr/bin/env bash
# Test de sauvegarde / restauration de la base HoneyLMS (dossier CDA, § 9.5).
# Ne touche pas à la base de travail : la sauvegarde est restaurée dans une base NEUVE
# (honeylms_restore), puis les deux bases sont comparées table par table.
#
# Prérequis : la stack tourne (docker compose up -d) et a été préparée (demo-prepare.sh).
# Usage (racine du dépôt) : bash test-restauration.sh | tee restauration.log
set -euo pipefail

C=honeylms-postgres          # conteneur PostgreSQL
U=honeylms                   # utilisateur
SRC=honeylms                 # base de travail
DST=honeylms_restore         # base de test de restauration
DUMP=honeylms_$(date +%Y%m%d_%H%M).dump

COUNT_SQL="
SELECT 'role' AS t, count(*) FROM role UNION ALL
SELECT 'user_account', count(*) FROM user_account UNION ALL
SELECT 'course', count(*) FROM course UNION ALL
SELECT 'course_module', count(*) FROM course_module UNION ALL
SELECT 'lesson', count(*) FROM lesson UNION ALL
SELECT 'enrollment', count(*) FROM enrollment UNION ALL
SELECT 'assignment', count(*) FROM assignment UNION ALL
SELECT 'submission', count(*) FROM submission UNION ALL
SELECT 'stored_file', count(*) FROM stored_file UNION ALL
SELECT 'lesson_completion', count(*) FROM lesson_completion UNION ALL
SELECT 'training_class', count(*) FROM training_class UNION ALL
SELECT 'flyway_schema_history', count(*) FROM flyway_schema_history
ORDER BY 1;"

echo "== Test de restauration HoneyLMS — $(date '+%d/%m/%Y %H:%M:%S')"
docker exec "$C" psql -U "$U" -d "$SRC" -tAc "SELECT version();"

echo; echo "== 1. Sauvegarde (pg_dump, format custom)"
time docker exec "$C" pg_dump -U "$U" -Fc "$SRC" > "$DUMP"
ls -lh "$DUMP"

echo; echo "== 2. Restauration dans une base neuve ($DST)"
docker exec "$C" dropdb -U "$U" --if-exists "$DST"
docker exec "$C" createdb -U "$U" "$DST"
time docker exec -i "$C" pg_restore -U "$U" -d "$DST" --no-owner < "$DUMP"

echo; echo "== 3. Comparaison du nombre de lignes par table"
docker exec "$C" psql -U "$U" -d "$SRC" -tA -F ' ' -c "$COUNT_SQL" > /tmp/hl_src.txt
docker exec "$C" psql -U "$U" -d "$DST" -tA -F ' ' -c "$COUNT_SQL" > /tmp/hl_dst.txt
paste /tmp/hl_src.txt /tmp/hl_dst.txt | awk '{printf "%-22s source=%-6s restauree=%-6s %s\n", $1, $2, $4, ($2==$4?"OK":"ECART")}'

echo; echo "== 4. Dernière migration Flyway dans la base restaurée"
docker exec "$C" psql -U "$U" -d "$DST" -tAc \
  "SELECT version, description, success FROM flyway_schema_history WHERE version IS NOT NULL ORDER BY installed_rank DESC LIMIT 1;"

if diff -q /tmp/hl_src.txt /tmp/hl_dst.txt >/dev/null; then
  echo; echo "RESULTAT : restauration conforme, toutes les tables ont le même nombre de lignes."
else
  echo; echo "RESULTAT : ECART détecté, voir le tableau ci-dessus."
fi
echo "(La base $DST peut être supprimée avec : docker exec $C dropdb -U $U $DST)"
