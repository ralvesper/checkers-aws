# Backend — modelo de domínio

Pacotes agrupados por funcionalidade; cada contexto terá suas próprias camadas quando forem necessárias:

```text
br.com.highonline.checkers
├── game
│   └── domain
│       ├── Game.java       # entidade e raiz do agregado Partida
│       ├── GameId.java     # value object
│       ├── Board.java      # value object imutável
│       ├── Square.java     # value object
│       ├── Piece.java      # value object
│       └── Color.java      # enum do domínio
└── player
    └── domain
        ├── Player.java     # entidade e raiz do agregado Jogador
        └── PlayerId.java   # value object
```

`Game` referencia jogadores por `PlayerId`, sem carregar o agregado `Player`. Duas pessoas distintas são necessárias para iniciar uma partida; **as brancas começam**, com 12 peças para cada lado. `Board` não expõe coleção mutável.

A variante escolhida é [damas brasileiras de 64 casas](../docs/rules/README.md). O domínio aceita lances completos, captura obrigatória, lei da maioria, capturas em cadeia, movimento de damas e coroação ao final do lance. Detecta vitória por ausência de peças ou movimentos do adversário. Empates e relógio ainda não estão implementados. Os pacotes `application` e `infrastructure` serão adicionados quando houver casos de uso e persistência.

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

O perfil padrão `local` disponibiliza a API com armazenamento volátil e identidade de protótipo via cabeçalho. O perfil alternativo `local-postgres` usa `game/infrastructure/postgres`, com JDBC, migrações Flyway e gravação transacional de estado e recibos. Veja o [contrato REST](../docs/api.md), [requests.http](requests.http) e as [instruções PostgreSQL](../docs/postgresql.md). Autenticação real continua pendente.

