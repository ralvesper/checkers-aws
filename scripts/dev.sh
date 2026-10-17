#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
action="${1:-up}"
case "$action" in
  up) docker compose up --build -d --wait; printf '\nAbra http://localhost:8081\n' ;;
  down) CHECKERS_SESSION_SECRET="${CHECKERS_SESSION_SECRET:-unused-for-stop}" CHECKERS_DATABASE_PASSWORD="${CHECKERS_DATABASE_PASSWORD:-unused-for-stop}" docker compose down ;;
  logs) CHECKERS_SESSION_SECRET="${CHECKERS_SESSION_SECRET:-unused-for-stop}" CHECKERS_DATABASE_PASSWORD="${CHECKERS_DATABASE_PASSWORD:-unused-for-logs}" docker compose logs -f ;;
  status) CHECKERS_SESSION_SECRET="${CHECKERS_SESSION_SECRET:-unused-for-stop}" CHECKERS_DATABASE_PASSWORD="${CHECKERS_DATABASE_PASSWORD:-unused-for-status}" docker compose ps ;;
  reset)
    if [[ "${2:-}" != "--delete-data" ]]; then printf 'Para apagar o banco local, use: bash scripts/dev.sh reset --delete-data\n' >&2; exit 2; fi
    CHECKERS_SESSION_SECRET="${CHECKERS_SESSION_SECRET:-unused-for-stop}" CHECKERS_DATABASE_PASSWORD="${CHECKERS_DATABASE_PASSWORD:-unused-for-stop}" docker compose down -v ;;
  *) printf 'Uso: bash scripts/dev.sh up|down|logs|status|reset --delete-data\n' >&2; exit 2 ;;
esac
