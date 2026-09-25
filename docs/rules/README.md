# Regras — damas brasileiras de 64 casas

**Variante escolhida:** brasileira, tabuleiro de 8×8. Fonte de referência: [Regras Oficiais de Jogo de Damas da CBJD](https://midiasstoragesec.blob.core.windows.net/001/2019/04/cbjd-regras-damas-010113.pdf), especialmente páginas 3–6 do PDF.

## Base para o domínio

- Dois jogadores, 12 peças em casas escuras para cada um. **Brancas fazem o primeiro lance.**
- Pedra anda uma casa na diagonal para a frente; captura tanto para frente quanto para trás.
- Dama percorre diagonais em ambos os sentidos por qualquer número de casas livres; após capturar pode parar em qualquer casa livre depois da peça capturada na mesma diagonal.
- Captura é obrigatória. Entre sequências disponíveis deve ser escolhida uma que capture o **maior número de peças** (lei da maioria); pedra e dama valem uma peça cada para essa conta.
- Na captura em cadeia, a peça pode voltar a passar por uma casa vazia, mas não capturar a mesma peça novamente. As capturadas só são retiradas depois da sequência completa (tema turco).
- Pedra vira dama ao **terminar** o lance na linha de coroação. Se passar por ela durante uma captura e continuar o lance, permanece pedra.
- Vitória por capturar todas as peças ou deixar o adversário sem jogadas. Há regras oficiais adicionais de empate por repetição, contagem de lances e certos finais.

## Decisão de modelagem proposta

Um **lance completo**, inclusive todos os saltos de uma captura múltipla, será um único comando ao agregado `Game`: jogador, casa inicial e lista ordenada das casas de pouso. O agregado valida a sequência, atualiza o tabuleiro, muda a vez e incrementa `version` uma vez. Isso permite verificar a lei da maioria sobre o lance inteiro e evita publicar ao adversário um estado intermediário.

A decisão de enviar a sequência inteira em um comando precisa ser validada para a experiência desejada no Angular. Os casos de empate oficiais serão implementados em etapas; não declarar uma partida totalmente aderente às regras oficiais antes disso.

Veja os [cenários para testes](acceptance-cases.md).
