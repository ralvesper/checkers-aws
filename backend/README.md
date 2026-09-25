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

A variante escolhida é [damas brasileiras de 64 casas](../docs/rules/README.md). Ainda não há movimento de peças nem mudança de turno. Os pacotes `application` e `infrastructure` serão adicionados por funcionalidade quando houver casos de uso e persistência.
