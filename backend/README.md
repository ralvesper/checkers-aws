# Backend — modelo de domínio

Pacotes agrupados por funcionalidade, com domínio independente do transporte e do banco.

| Pacote | Responsabilidade |
|---|---|
| game/domain | Agregado Game, tabuleiro, lances, regras e estado terminal |
| game/application | Casos de uso, comando PlayMove e porta transacional GameStore |
| game/infrastructure/http | Contrato REST e erros HTTP |
| game/infrastructure/memory | Adaptador local em memória |
| game/infrastructure/postgres | JDBC, codec de snapshot e transações |
| game/infrastructure/realtime | WebSocket de snapshots confirmados |
| player/domain | Agregado Player e identidade PlayerId |

`Game` referencia jogadores por `PlayerId`, sem carregar o agregado `Player`. Duas pessoas distintas são necessárias para iniciar uma partida; **as brancas começam**, com 12 peças para cada lado. `Board` não expõe coleção mutável.

A variante escolhida é [damas brasileiras de 64 casas](../docs/rules/README.md). O domínio aceita lances completos, captura obrigatória, lei da maioria, capturas em cadeia, movimento de damas e coroação ao final do lance. Detecta vitória por ausência de peças ou movimentos do adversário. Empates e relógio ainda não estão implementados.

## Aplicar um lance

`Game` permanece imutável. O chamador deve guardar o snapshot retornado:

```java
Game game = Game.start(blackPlayerId, whitePlayerId);
Move move = new Move(new Square(5, 0), List.of(new Square(4, 1)));
Game updated = game.play(whitePlayerId, move, game.version());
```

`Move` contém origem e pousos ordenados de um turno completo. `legalMoves()` retorna apenas sequências completas que atendem à lei da maioria. As capturadas bloqueiam diagonais até o fim do lance. `play` valida jogador e versão, incrementa a versão uma vez e retorna status e vencedor. Um erro não altera o snapshot original.

`StaleGameVersionException` distingue versão desatualizada; erros de jogador ou lance usam `IllegalArgumentException`. O perfil PostgreSQL adiciona controle otimista no banco além da verificação no domínio.

## Casos de uso e transporte

`game/application` contém `GameService`, o comando `PlayMove` e a porta `GameStore`, sem dependências Spring. A porta exige atualização atômica de estado e recibos de idempotência. `game/infrastructure/memory` implementa esse contrato com `ConcurrentHashMap.compute`, serializando atualizações da mesma partida. `game/infrastructure/http` adapta JSON e códigos HTTP aos casos de uso.

O perfil padrão `local` disponibiliza a API com armazenamento volátil e sessões de visitante assinadas. O perfil alternativo `local-postgres` usa `game/infrastructure/postgres`, com JDBC, migrações Flyway e gravação transacional de estado e recibos. Veja o [contrato REST](../docs/api.md), [requests.http](requests.http) e as [instruções PostgreSQL](../docs/postgresql.md). Os assentos são vinculados a sessões privadas e os convites são de uso único. Contas e recuperação de acesso continuam fora do MVP.

