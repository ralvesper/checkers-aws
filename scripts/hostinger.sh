#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

REMOTE_HOST="${CHECKERS_REMOTE_HOST:-hostinger}"
REMOTE_DIR="${CHECKERS_REMOTE_DIR:-/root/checkers-aws}"
# 8081 já é usado por outro app nesse VPS (passa-pra-frente); 55432 evita o Postgres nativo na 5432.
FRONTEND_PORT="${CHECKERS_FRONTEND_PORT:-8092}"
POSTGRES_PORT="${CHECKERS_POSTGRES_PORT:-55432}"
COMPOSE_FILES="-f compose.yaml -f compose.hostinger.yaml"

sync() {
  rsync -az --delete \
    --exclude .git --exclude node_modules --exclude target --exclude dist --exclude .angular --exclude .env --exclude backups --exclude .playwright-mcp \
    ./ "$REMOTE_HOST:$REMOTE_DIR/"
  ssh "$REMOTE_HOST" "test -f $REMOTE_DIR/.env || cp $REMOTE_DIR/.env.example $REMOTE_DIR/.env"
  # Gera a chave de sessão uma única vez; nunca a sobrescreve (ver docs/hostinger.md).
  ssh "$REMOTE_HOST" "grep -qE '^CHECKERS_SESSION_SECRET=.+' $REMOTE_DIR/.env || (sed -i '/^CHECKERS_SESSION_SECRET=/d' $REMOTE_DIR/.env; echo \"CHECKERS_SESSION_SECRET=\$(openssl rand -hex 32)\" >> $REMOTE_DIR/.env); chmod 600 $REMOTE_DIR/.env"
}

remote_compose() {
  ssh "$REMOTE_HOST" "cd $REMOTE_DIR && CHECKERS_FRONTEND_PORT=$FRONTEND_PORT CHECKERS_POSTGRES_PORT=$POSTGRES_PORT docker compose $COMPOSE_FILES $*"
}

action="${1:-up}"
case "$action" in
  up)
    sync
    remote_compose up --build -d --wait
    printf '\nRodando em %s:%s (senha do banco e CHECKERS_SESSION_SECRET ficam em %s/.env no servidor)\n' "$REMOTE_HOST" "$FRONTEND_PORT" "$REMOTE_DIR"
    ;;
  down) remote_compose down ;;
  logs) remote_compose logs -f ;;
  status) remote_compose ps ;;
  reset)
    if [[ "${2:-}" != "--delete-data" ]]; then printf 'Para apagar o banco remoto, use: bash scripts/hostinger.sh reset --delete-data\n' >&2; exit 2; fi
    remote_compose down -v ;;
  *) printf 'Uso: bash scripts/hostinger.sh up|down|logs|status|reset --delete-data\n' >&2; exit 2 ;;
esac
