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
    }
    Player --> PlayerId : identidade
    Game --> GameId : identidade
    Game --> PlayerId : jogadores
    Game *-- Board : tabuleiro
    Board *-- Piece : peças
    Board --> Square : posições
    Piece --> Color : cor
```

`Game` e `Player` são raízes de agregados separados. A partida referencia identidades de jogadores, sem carregar o agregado `Player`. `Board`, `Square`, `Piece`, `GameId` e `PlayerId` são value objects. O modelo ainda não contém jogadas nem transição de turnos.
