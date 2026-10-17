# API REST local

Execute `mvn spring-boot:run` em `backend/` com Java 25. O perfil padrão é `local`.
Nesse perfil, o adaptador usa memória: estado e recibos são perdidos ao reiniciar e não são compartilhados entre processos.
O perfil alternativo [local-postgres](postgresql.md) mantém o mesmo contrato com estado e recibos duráveis no PostgreSQL.
Os endpoints existem somente nesses dois perfis de desenvolvimento.

## Identidade no protótipo

Todas as operações exigem `X-Player-Id` com UUID. O chamador pode escolher esse valor: **não é autenticação**.
Este contrato é destinado ao desenvolvimento local. Antes de hospedar, substituir a identidade pelo principal autenticado,
implementar armazenamento durável e configurar os endpoints fora do perfil local.
Não há cadastro de jogadores nesta etapa: os dois IDs distintos são informados na criação.

## Endpoints

| Método | Caminho | Resultado |
|---|---|---|
| POST | `/games` | Cria partida, devolve `201` e `Location`; ator deve ser um dos participantes |
| GET | `/games/{id}` | Devolve o estado atual para um participante |
| POST | `/games/{id}/moves` | Aplica um turno completo e devolve o estado confirmado |

Criação:

```json
{
  "blackPlayerId": "11111111-1111-4111-8111-111111111111",
  "whitePlayerId": "22222222-2222-4222-8222-222222222222"
}
```

Lance das brancas, usando seu ID no cabeçalho:

```json
{
  "from": { "row": 5, "column": 0 },
  "landings": [{ "row": 4, "column": 1 }],
  "expectedVersion": 0,
  "clientMoveId": "33333333-3333-4333-8333-333333333333"
}
```

Coordenadas entre 0 e 7, apenas casas escuras. Capturas múltiplas enviam todos os pousos no mesmo comando.
A resposta contém `id`, jogadores, `turn`, `currentPlayerId`, `version`, `status`, `winner`, `pieces` e `legalMoves`.
Cada peça contém `row`, `column`, `color` e `king`. `legalMoves` contém somente turnos completos permitidos.
Na vitória, `currentPlayerId` é nulo, `legalMoves` é vazio e `winner` indica a cor vencedora.

## Versão e idempotência

Cada lance aceito incrementa a versão uma vez. A atualização de estado e do recibo é atômica por partida.
`clientMoveId` é único por partida. Repetir exatamente jogador, caminho, versão esperada e ID devolve
o snapshot da primeira aceitação, inclusive se a partida já avançou depois. Consulte GET para recuperar
o estado mais recente. O reenvio não aplica o lance novamente nem reduz a versão armazenada.
Reutilizar o ID com outro conteúdo gera conflito. Comandos rejeitados não consomem o ID.

## Erros

Erros retornam `application/problem+json`, com `status`, `title`, `detail` e `code`.

| HTTP | `code` | Situação |
|---|---|---|
| 400 | INVALID_REQUEST | Corpo, UUID, coordenada ou cabeçalho inválido/ausente |
| 403 | FORBIDDEN | Ator não participa da partida |
| 404 | NOT_FOUND | Partida inexistente |
| 409 | VERSION_CONFLICT | Estado usado pelo cliente está desatualizado |
| 409 | COMMAND_CONFLICT | ID do comando reutilizado com conteúdo diferente |
| 409 | WRONG_TURN | Participante tenta jogar fora da vez |
| 409 | FINISHED | Novo comando em partida encerrada |
| 422 | INVALID_MOVE | Caminho bem formado que viola as regras do jogo |

O perfil `local-postgres` grava estado, lances e recibos na mesma transação com controle otimista no banco.
O [WebSocket](realtime.md) entrega snapshots confirmados entre os participantes em uma instância da API.
Ainda não há endpoint de histórico, autenticação real, empates ou relógio.

Exemplos executáveis: [backend/requests.http](../backend/requests.http).
