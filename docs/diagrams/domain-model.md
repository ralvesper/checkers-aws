# Modelo de domínio atual

```mermaid
classDiagram
    namespace player_domain {
        class Player
        class PlayerId
    }
    namespace game_domain {
        class Game
        class GameId
        class Board
        class Square
        class Piece
        class Color
        class Move
        class GameStatus
    }
    Player --> PlayerId : identidade
    Game --> GameId : identidade
    Game --> PlayerId : jogadores
    Game *-- Board : tabuleiro
    Game --> Move : valida lance completo
    Game --> GameStatus : estado
    Board *-- Piece : peças
    Board --> Square : posições
    Piece --> Color : cor
```

`Game` e `Player` são raízes de agregados separados. A partida referencia identidades de jogadores, sem carregar o agregado `Player`. `Board`, `Square`, `Piece`, `GameId` e `PlayerId` são value objects. `Move` representa um turno completo, inclusive capturas múltiplas. `Game.play` devolve um novo snapshot com tabuleiro, vez, versão, estado e vencedor. Lance inválido preserva o snapshot original.

