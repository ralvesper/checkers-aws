# PostgreSQL local

O perfil `local-postgres` usa JDBC e Flyway para persistir partidas, lances aceitos e recibos de idempotência.
O perfil padrão `local` continua usando memória, sem exigir banco ou Docker.
Os dois perfis são de desenvolvimento: `X-Player-Id` ainda não autentica o jogador.

## Iniciar no WSL / Linux

Na raiz do projeto, escolha uma senha local (não a envie ao GitHub):

```bash
export CHECKERS_DATABASE_PASSWORD='escolha-uma-senha-local'
docker compose up -d --wait postgres
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local-postgres
```

PowerShell, na raiz:

```powershell
$env:CHECKERS_DATABASE_PASSWORD = 'escolha-uma-senha-local'
docker compose up -d --wait postgres
Set-Location backend
mvn spring-boot:run '-Dspring-boot.run.profiles=local-postgres'
```

Também é possível usar PostgreSQL existente definindo `CHECKERS_DATABASE_URL` (JDBC),
`CHECKERS_DATABASE_USER` e `CHECKERS_DATABASE_PASSWORD`. Padrões de URL e usuário:
`jdbc:postgresql://localhost:5432/checkers` e `checkers`. A senha é obrigatória.

As migrações em `backend/src/main/resources/db/migration/` são executadas antes do adaptador ficar disponível.
O contrato HTTP e os exemplos de [requests.http](../backend/requests.http) são os mesmos do perfil em memória.
Crie uma partida, aplique um lance e reinicie a API: GET continua retornando o estado persistido.

## Modelo e transações

| Tabela | Conteúdo |
|---|---|
| `games` | Identidade, versão atual, snapshot JSONB, datas de criação e atualização |
| `game_moves` | Comando aceito, `clientMoveId`, versão resultante, snapshot da resposta e data |
| `flyway_schema_history` | Migrações aplicadas pelo Flyway |

O formato do snapshot tem `schemaVersion = 1` e é independente do DTO HTTP. Reconstituição valida
identidades, casas, turno, versão e coerência entre estado terminal e movimentos disponíveis.
O histórico contém apenas lances aceitos; não existe endpoint público de histórico nesta etapa.

A leitura de estado e recibos usa uma única consulta, com snapshot consistente em `READ COMMITTED`.
A gravação faz `UPDATE ... WHERE id = ? AND version = ?`, seguida do INSERT do lance na mesma transação.
Falha no histórico desfaz a atualização da partida. Há unicidade de `(game_id, client_move_id)`
e `(game_id, resulting_version)` no banco.

Quando duas requisições leem a mesma versão, somente um UPDATE pode confirmar. A outra transação
é desfeita e relê o estado uma vez: um reenvio idêntico retorna o recibo original; outro comando
recebe conflito de versão. Esse mecanismo funciona entre processos conectados ao mesmo banco.

O adaptador carrega os recibos da partida para manter a porta atual. Para partidas longas ou alto volume,
evoluir a porta para buscar um recibo específico, paginar histórico e adotar pool de conexões.

## Testes com banco real

No CI, PostgreSQL 17 é obrigatório e a suíte executa testes de migração, persistência entre adaptadores,
reenvio após outra jogada, corridas entre duas conexões e rollback por falha injetada no histórico.
Há também um teste HTTP com o perfil `local-postgres`.

Para executar localmente, use **um banco dedicado a testes**: a suíte de rollback cria um trigger temporário.

```bash
export PG_TEST_URL='jdbc:postgresql://localhost:5432/checkers_test'
export PG_TEST_USER='checkers'
export PG_TEST_PASSWORD="$CHECKERS_DATABASE_PASSWORD"
cd backend
mvn test
```

O banco `checkers_test` deve existir e o usuário deve poder criar tabelas, funções e triggers.
Sem `PG_TEST_URL`, somente os testes que exigem PostgreSQL são ignorados; domínio, aplicação,
codec JSON e API em memória continuam executando. O CI sempre informa essa variável.

## Parar

Na raiz, `docker compose down` remove os containers e mantém o volume e os dados.
`docker compose down -v` também apaga o banco local permanentemente.

O uso de RDS, secrets gerenciados, autenticação real, backups, pool de conexões e produção
será tratado antes de hospedar. Nenhum recurso AWS é provisionado por esta etapa.
