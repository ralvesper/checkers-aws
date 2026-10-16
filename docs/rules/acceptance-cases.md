# Cenários de aceitação do domínio

Referência: [CBJD, Regras Oficiais de Jogo de Damas](https://midiasstoragesec.blob.core.windows.net/001/2019/04/cbjd-regras-damas-010113.pdf). O domínio suporta o recorte abaixo; veja `GameTest` e `MoveRulesTest`. Empates e relógio continuam fora deste recorte.

| Cenário | Resultado esperado |
| --- | --- |
| Começar a partida | 12 pedras de cada cor, brancas jogam primeiro, `version = 0`. |
| Pedra branca avançar uma casa diagonal sem captura disponível | Lance aceito, vez passa às pretas, versão aumenta uma vez. |
| Pedra tentar andar para trás sem captura | Lance rejeitado, estado não muda. |
| Pedra capturar para trás | Lance aceito se permitido pela posição e pela lei da maioria. |
| Existir captura e tentar movimento simples | Lance rejeitado. |
| Duas capturas possíveis: uma captura 1 peça, outra 2 | Somente o caminho que captura 2 é aceito. |
| Captura em cadeia com múltiplos pousos | Receber o caminho completo; só então trocar de jogador e aumentar a versão. |
| Dama atravessar casas livres na diagonal | Movimento aceito, se não houver captura obrigatória. |
| Pedra terminar o lance na última linha | Promovida a dama. Se apenas passar por essa linha em uma cadeia, continua pedra. |
| Repetir captura da mesma peça durante a cadeia | Lance rejeitado; peças capturadas permanecem bloqueando a linha até o fim. |
| Jogador sem peças ou sem lance disponível | Partida termina com vitória do adversário. |
| Versão esperada desatualizada | Comando rejeitado sem alterar o agregado. |

## Validar antes de implementar

- O frontend enviará o caminho completo em um comando, ou cada salto separadamente? **Proposta: caminho completo.**
- Qual orientação visual do tabuleiro será usada no Angular? Internamente, `row=0` é a base das pretas e `row=7` a base das brancas; isso corresponde ao primeiro lance das brancas subindo no array.
- Empates e relógio entram em qual etapa? Proposta: primeiro movimentos e vitória; depois repetição, contagens e tempo.

