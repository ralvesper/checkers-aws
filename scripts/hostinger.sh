#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

REMOTE_HOST="${CHECKERS_REMOTE_HOST:-hostinger}"
REMOTE_DIR="${CHECKERS_REMOTE_DIR:-/root/checkers-aws}"
COMPOSE_FILES="-f compose.yaml -f compose.hostinger.yaml"

sync() {
  rsync -az --delete \
    --exclude .git --exclude node_modules --exclude target --exclude dist --exclude .angular --exclude .env --exclude backups --exclude .playwright-mcp \
    ./ "$REMOTE_HOST:$REMOTE_DIR/"
  ssh "$REMOTE_HOST" "test -f $REMOTE_DIR/.env || cp $REMOTE_DIR/.env.example $REMOTE_DIR/.env"
  # Chave de sessão e portas são geradas/ajustadas uma única vez no .env remoto, nunca sobrescritas
  # depois (ver docs/hostinger.md). 8081/5432 colidem com outros apps/serviços nativos desse VPS.
  ssh "$REMOTE_HOST" "cd $REMOTE_DIR && \
    grep -qE '^CHECKERS_SESSION_SECRET=.+' .env || (sed -i '/^CHECKERS_SESSION_SECRET=/d' .env; echo \"CHECKERS_SESSION_SECRET=\$(openssl rand -hex 32)\" >> .env); \
    grep -q '^CHECKERS_FRONTEND_PORT=' .env || echo 'CHECKERS_FRONTEND_PORT=8092' >> .env; \
    grep -q '^CHECKERS_POSTGRES_PORT=' .env || echo 'CHECKERS_POSTGRES_PORT=55432' >> .env; \
    chmod 600 .env"
}

remote_compose() {
  ssh "$REMOTE_HOST" "cd $REMOTE_DIR && docker compose $COMPOSE_FILES $*"
}

action="${1:-up}"
case "$action" in
  up)
    sync
    remote_compose up --build -d --wait
    printf '\nAtualizado em %s (porta, senha e CHECKERS_SESSION_SECRET ficam em %s/.env no servidor)\n' "$REMOTE_HOST" "$REMOTE_DIR"
    ;;
  down) remote_compose down ;;
  logs) remote_compose logs -f ;;
  status) remote_compose ps ;;
  reset)
    if [[ "${2:-}" != "--delete-data" ]]; then printf 'Para apagar o banco remoto, use: bash scripts/hostinger.sh reset --delete-data\n' >&2; exit 2; fi
    remote_compose down -v ;;
  *) printf 'Uso: bash scripts/hostinger.sh up|down|logs|status|reset --delete-data\n' >&2; exit 2 ;;
esac
