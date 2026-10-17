# API REST — sessões de visitante

O backend expõe o contrato nos perfis local (memória) e local-postgres (durável). O frontend/Nginx usa
`/api` como prefixo externo; diretamente no backend os caminhos abaixo não têm esse prefixo.

## Sessão e assentos

1. `POST /sessions` com `{}` retorna 201 e `{ "token": "...", "expiresAt": 1234567890 }`.
2. Envie `Authorization: Bearer TOKEN` nos pedidos seguintes. O token é emitido e assinado pelo servidor,
   expira em 30 dias e não pode ser substituído por um UUID. `X-Player-Id` não autoriza nenhuma operação.
3. `POST /games` com `{}` cria os dois IDs no servidor, vincula as brancas à sessão e retorna 201,
   Location e `{ "game": { ... }, "invitation": "..." }`. O convite é devolvido somente na criação.
4. Outro visitante cria sua própria sessão e envia `POST /games/{id}/join` com `{ "invitation": "..." }`.
   O primeiro aceite válido ocupa as pretas e consome o convite. Expira em 7 dias. Reenvio pela sessão
   que já ocupou as pretas devolve o estado; outra sessão recebe 409. O dono das brancas não pode ocupar as pretas com a mesma sessão.

Player IDs são identificadores públicos de assento, não credenciais. A ligação entre sessão e assento fica privada.
Os vínculos e hashes dos convites são persistidos na tabela game_access no perfil PostgreSQL. Tokens não são gravados no banco.
A aplicação usa capacidades anônimas; não oferece conta, recuperação de acesso nem prova da identidade da pessoa.

| Método | Caminho | Resultado |
|---|---|---|
| POST | /sessions | Emite sessão de visitante; não exige credencial anterior |
| POST | /games | Cria partida, assento das brancas e convite das pretas |
| POST | /games/{id}/join | Aceita convite de uso único |
| GET | /games/{id} | Estado da partida; X-Seat-Id indica o assento autorizado da sessão |
| POST | /games/{id}/moves | Valida e aplica um lance completo do assento autorizado |

Respostas de sessão e partida usam Cache-Control: no-store. Não armazenar tokens em logs ou URLs públicas.

## Lance

```json
{
  "from": { "row": 5, "column": 0 },
  "landings": [{ "row": 4, "column": 1 }],
  "expectedVersion": 0,
  "clientMoveId": "33333333-3333-4333-8333-333333333333"
}
```

Coordenadas de 0 a 7 em casas escuras; todos os pousos da captura vão juntos. A resposta contém id,
blackPlayerId, whitePlayerId, turn, currentPlayerId, version, status, winner, pieces e legalMoves.
Em vitória, currentPlayerId é nulo e legalMoves é vazio. O servidor resolve o autor pela sessão,
independentemente de IDs informados pelo cliente.

Cada aceite incrementa a versão uma vez e grava estado e recibo na mesma transação. Repetir jogador autorizado,
caminho, versão e clientMoveId devolve a resposta original sem reaplicar. Se a partida avançou, esse recibo pode
ter versão antiga; consulte GET para obter o estado mais recente. ID reaproveitado com outro conteúdo gera conflito.
Rejeições não consomem o clientMoveId.

## Erros

Formato application/problem+json com status, title, detail e code.

| HTTP | code | Situação |
|---|---|---|
| 400 | INVALID_REQUEST | Corpo ou coordenadas/caminho inválidos |
| 401 | UNAUTHORIZED | Token ausente, adulterado ou expirado; WWW-Authenticate: Bearer |
| 403 | FORBIDDEN | Sessão sem assento ou dono tentando aceitar o próprio convite |
| 409 | INVITATION_UNAVAILABLE | Convite inválido, expirado ou usado por outra sessão |
| 409 | VERSION_CONFLICT | Estado desatualizado |
| 409 | COMMAND_CONFLICT | clientMoveId reutilizado com conteúdo diferente |
| 409 | WRONG_TURN | Fora da vez |
| 409 | FINISHED | Novo lance em partida encerrada |
| 422 | INVALID_MOVE | Caminho bem formado que viola as regras |

Partidas sem permissão, inclusive registros legados sem vínculo, não concedem acesso só por conhecer um ID.
A camada interna ainda distingue NOT_FOUND, mas a verificação de assento pode retornar 403 antes dessa consulta.

No WebSocket, use os subprotocolos `checkers.v1` e `guest.TOKEN`. O servidor seleciona checkers.v1,
valida a sessão no handshake e resolve o assento da sala; credenciais não vão na query string.
Veja [realtime.md](realtime.md), [Hostinger](hostinger.md) e [requests.http](../backend/requests.http).
