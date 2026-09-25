# Fluxo de um lance (planejado)

Nas [regras escolhidas](../rules/README.md), uma captura múltipla constitui um lance completo. O cliente envia a casa inicial e todas as casas de pouso na ordem; o servidor troca a vez apenas ao concluir o lance.

```mermaid
sequenceDiagram
    participant A as Angular
    participant API as API de partidas
    participant G as Game (domínio)
    participant DB as PostgreSQL
    A->>API: lance(clientMoveId, expectedVersion, origem, pousos)
    API->>DB: carregar partida
    DB-->>API: tabuleiro, vez e version
    API->>API: conferir identidade e reenvio
    API->>G: aplicar lance completo
    G-->>API: novo estado ou erro de regra
    API->>DB: salvar estado e lance se version esperada
    alt versão válida
        DB-->>API: commit; nova version
        API-->>A: estado confirmado
    else conflito de versão
        DB-->>API: nenhuma atualização
        API-->>A: conflito; consultar estado atual
    end
```

## Invariantes

- O servidor confere se o jogador participa da partida, se é sua vez, se a partida está ativa e se o lance obedece às regras, inclusive captura obrigatória e lei da maioria.
- `expectedVersion` corresponde à versão exibida ao cliente; a gravação só ocorre se `game.version` ainda for essa versão. Estado, próxima vez, versão e histórico são confirmados na mesma transação.
- `clientMoveId` deve ser único por partida. Um reenvio devolve o resultado já confirmado sem aplicar o lance novamente; um identificador repetido com conteúdo diferente deve ser rejeitado.
- Ao receber conflito, detectar uma lacuna de versão ou reconectar, o cliente consulta o estado atual. No fluxo futuro, o WebSocket entrega o snapshot confirmado da partida após o commit; o cliente aplica apenas versões novas e consecutivas.

Detalhes de autenticação, empate e contrato HTTP serão definidos em etapas posteriores.
