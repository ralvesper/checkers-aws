# Backend — modelo de domínio

Pacotes agrupados por funcionalidade; cada contexto terá suas próprias camadas quando elas forem necessárias:

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

`Game` referencia jogadores pelos seus `PlayerId`, sem carregar entidades do agregado `Player`. Duas pessoas distintas são necessárias para iniciar uma partida; as pretas começam, com 12 peças para cada lado. `Board` não expõe coleção mutável.

Ainda não há movimento de peças nem mudança de turno. Na próxima etapa, vamos definir a variante de damas, a entidade `Move` dentro da partida e as regras que atualizam tabuleiro e vez numa única operação do agregado. Os pacotes `application` e `infrastructure` serão adicionados quando houver casos de uso e persistência; não são pacotes globais por tipo.
