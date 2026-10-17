# Hostinger — implantação e operação

Endereço informado: https://damas.highonline.com.br/. Na revisão de 17/10/2026, HTTPS respondeu 200
 e `/api/actuator/health/readiness` retornou `UP`. Isso verifica a saúde naquele momento, não autenticação,
backup, configuração do VPS ou execução do código novo. Nenhuma alteração remota foi realizada por esta revisão.

Este guia oferece uma **configuração de referência com Docker Compose**. A instalação atual no Hostinger
não foi inspecionada por SSH. Antes de aplicar, registre o caminho real do checkout, commit em execução,
Compose ou Kubernetes utilizado, localização do volume, proxy TLS e processo de renovação do certificado.
Se a instalação usa MicroK8s, traduza estas configurações para Secret, Deployment, Service e Ingress;
não execute uma segunda stack Compose sobre as mesmas portas ou dados.

## Mudança de identidade — ler antes de atualizar

A API antiga aceitava UUIDs escolhidos pelo cliente. A nova versão emite sessão de visitante assinada e
vincula cada assento a essa sessão. O criador ocupa as brancas; o convite das pretas pode ser aceito
uma vez, por outra sessão. Reenvio pelo mesmo convidado é idempotente. O convite expira em 7 dias.

**Links antigos com `player=` deixam de autorizar acesso.** A migração V2 preserva partidas e histórico,
mas não inventa permissões para esses registros: os UUIDs antigos não eram credenciais confiáveis.
Atualize backend e frontend juntos e crie novas partidas após a atualização. Sessões novas duram 30 dias,
ficam em sessionStorage da aba e sobrevivem a reload; fechar a aba/perder esse armazenamento pode perder
seu acesso. Não há conta, recuperação de senha, revogação individual ou transferência de sessão entre dispositivos.
Convites são capacidades: antes do primeiro aceite, quem possuir o convite pode ocupar a vaga.

No PostgreSQL, o hash do convite e os vínculos de assento são persistidos. O token de sessão é verificado
por HMAC-SHA256 com chave estável. Rotacionar a chave invalida as sessões anteriores; reinicie a API ao rotacionar.
Mantenha-a fora do Git, dos logs, de capturas de tela e do frontend.

## Configuração da referência Compose

Requer Docker/Compose v2, checkout do repositório e proxy TLS no VPS. Preserve o `.env` existente.
Adicione uma chave nova, gerada **uma vez** com `openssl rand -hex 32`, e mantenha-a nos próximos deploys.
Não use a senha do banco como chave de sessão.

```properties
CHECKERS_DATABASE_PASSWORD=SENHA_FORTE_DO_BANCO_EXISTENTE
CHECKERS_POSTGRES_PORT=5432
CHECKERS_SESSION_SECRET=CHAVE_ALEATORIA_GERADA_COM_OPENSSL
CHECKERS_WEBSOCKET_ALLOWED_ORIGINS=https://damas.highonline.com.br
```

Os valores acima são placeholders, não credenciais utilizáveis. Restrinja a leitura do arquivo (`chmod 600 .env`).
A variável de origem agora é parametrizada no Compose; alterar `.env` exige recriar o backend para carregar o valor.
A senha de um banco já inicializado não muda apenas por editar `.env`.

```bash
# Execute no checkout correto do projeto, após backup e revisão da versão.
docker compose -f compose.yaml -f compose.hostinger.yaml config --quiet
docker compose -f compose.yaml -f compose.hostinger.yaml up --build -d --wait
```

O override adiciona restart policy. Nginx e PostgreSQL continuam vinculados ao loopback do host;
a API fica na rede Docker. Publique apenas HTTP/HTTPS pelo proxy do VPS. Não abra PostgreSQL ou backend diretamente na internet.
A referência de proxy está em [nginx.conf.example](../deploy/hostinger/nginx.conf.example).
Confira os caminhos de certificado; execute `nginx -t` antes de recarregar Nginx. Verifique a renovação automática
com o mecanismo realmente instalado. Não sobrescreva virtual hosts de outras aplicações.

A referência limita taxa e corpo das requisições. Ajuste limites à carga real. As sessões anônimas não
substituem contas autenticadas, quotas ou uma revisão de segurança para produção. Não registre Authorization
nem Sec-WebSocket-Protocol: o segundo transporta a credencial do socket. Tokens não vão na URL do WebSocket.

## Backup e restauração

Volume Docker persistente não é backup. Defina frequência, retenção e uma cópia fora do VPS, protegida.
Faça backup também da chave de sessão em um local separado e seguro; ela é necessária para manter sessões
após restauração. Teste a restauração em ambiente separado antes de depender do backup.

```bash
# Dentro do checkout, com .env configurado. O dump contém dados de acesso: proteja-o.
umask 077
mkdir -p backups
backup_file="backups/checkers-$(date -u +%Y%m%dT%H%M%SZ).dump"
docker compose exec -T postgres pg_dump -U checkers -d checkers -Fc > "$backup_file"
pg_restore --list "$backup_file" > /dev/null
```

A última linha requer cliente PostgreSQL no host; alternativamente valide o arquivo com o pg_restore do container.
Não envie dumps ao GitHub. A inspeção do catálogo não substitui restaurar e verificar os dados.

**Restauração substitui dados.** Pare gravações, faça um backup do estado atual e confirme o arquivo correto:

```bash
docker compose -f compose.yaml -f compose.hostinger.yaml stop backend
docker compose exec -T postgres pg_restore -U checkers -d checkers --clean --if-exists --no-owner --no-privileges < BACKUP_ESCOLHIDO.dump
docker compose -f compose.yaml -f compose.hostinger.yaml up -d --wait backend
```

Mantenha frontend/backend na versão correspondente ao schema restaurado. Não use `down -v` nem os comandos
`reset --delete-data` do desenvolvimento no servidor publicado.

## Atualização e rollback

1. Registre commit/imagens em execução e preserve `.env`, volume e configuração do proxy.
2. Gere e valide um backup. Leia as migrações e notas de compatibilidade; a V2 invalida links antigos.
3. Prepare a versão desejada e construa as imagens; o Compose de referência compila a partir do checkout.
4. Execute o up acima numa janela de manutenção. Flyway migra antes da prontidão da API.
5. Teste saúde, nova partida, convite em outro navegador, lances dos dois lados e indicação AO VIVO.
6. Reinicie apenas a API e confira se partida, sessão e reenvio continuam funcionando.

Se falhar, guarde logs sem credenciais e volte ao commit/imagens registrados. Reverter somente o código
não desfaz migrações. Para rollback de versão com schema incompatível, restaure o backup em ambiente preparado
com a versão compatível e a chave de sessão correspondente; a volta à API antiga reintroduz o problema de identidade.
Não prometa rollback automático nem exclusão de migrações Flyway já aplicadas.

## Operação e diagnóstico

```bash
curl --fail https://damas.highonline.com.br/api/actuator/health/readiness
docker compose -f compose.yaml -f compose.hostinger.yaml ps
docker compose -f compose.yaml -f compose.hostinger.yaml logs --tail=200 backend frontend postgres
df -h
docker system df
```

- 401: sessão inválida/expirada ou chave trocada. Não contorne aceitando UUID no cabeçalho antigo.
- 403: sessão sem assento; confira se foi aberto o convite e se o armazenamento da aba foi preservado.
- 409 INVITATION_UNAVAILABLE: convite inválido, expirado ou já aceito por outra sessão.
- WebSocket reconectando: conferir origem HTTPS exata, handshake 101 e encaminhamento Upgrade/Connection/Subprotocol.
- 502/503: verificar saúde do backend, migração, senha e disponibilidade do PostgreSQL.
- Após reboot: confirmar Docker ativo, restart policy, volume montado, proxy e certificado válidos.

Há uma instância de API. Não aumente réplicas antes de distribuir os eventos do hub; estado durável no banco
não distribui mensagens WebSocket sozinho. Pool JDBC, quotas, contas, monitoramento e regras de empate seguem no roadmap.

## AWS

Terraform é estudo opcional separado. Não executar apply: nenhum recurso AWS foi criado nesta alteração,
e não há razão para provisionar AWS para operar a hospedagem Hostinger atual.
