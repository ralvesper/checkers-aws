# Arquitetura da aplicação

> **Estado atual:** o backend contém o modelo inicial de domínio. Os casos de uso, API de partidas, Angular, persistência e infraestrutura AWS abaixo são uma proposta para as próximas etapas. O [desenho global](aws-deployment-global.md) é um estudo de escala posterior.

## Componentes da aplicação

```mermaid
flowchart TB
    UI["Angular: tabuleiro e interacao"]
    API["Spring Boot: API REST e WebSocket futuro"]
    APP["game/application: casos de uso"]
    DOMAIN["game/domain: agregado Game e regras"]
    PORT["game/application: porta de persistencia"]
    ADAPTER["game/infrastructure: adaptador PostgreSQL"]
    DB[("PostgreSQL: partidas, lances e versoes")]

    UI -->|"criar, consultar e enviar lance"| API
    API --> APP
    APP --> DOMAIN
    APP --> PORT
    ADAPTER -. "implementa" .-> PORT
    ADAPTER --> DB
    API -. "confirmacao ou aviso" .-> UI
```

| Componente | Responsabilidade |
|---|---|
| Angular | Exibe tabuleiro, turno e versão; monta o lance completo e envia comando. Ao reconectar ou detectar lacuna de versão, consulta o estado atual. |
| API REST | Recebe comandos e devolve confirmação ou erro ao autor da jogada; identifica o usuário autenticado. `GET /games/{id}` fornece o estado oficial. |
| WebSocket | Após implementado, avisa os participantes de uma mudança confirmada. Não decide o turno nem garante histórico de eventos. |
| `game/application` | Coordena carregamento, autorização, chamada ao domínio e persistência transacional; define portas para dependências externas. |
| `game/domain` | `Game` valida um lance inteiro conforme as [regras brasileiras](../rules/README.md), atualiza tabuleiro e troca a vez. Independe de Spring e PostgreSQL. |
| `game/infrastructure` | Adapta banco e transporte às portas da aplicação; atualiza o estado condicionalmente por `expectedVersion`. |
| PostgreSQL | Fonte oficial de tabuleiro, jogador da vez, versão, histórico e `clientMoveId` único. Estado e histórico devem ser gravados na mesma transação. |

Os pacotes são organizados **por funcionalidade**: `game/domain`, `game/application` e `game/infrastructure`. `Player` forma outro agregado, referenciado por `PlayerId`. A versão detecta comandos enviados sobre um estado antigo; `clientMoveId` torna o reenvio idempotente. Veja o [modelo de domínio](domain-model.md).

## Sequência de uma jogada

### Autor da jogada recebe a confirmação

```mermaid
sequenceDiagram
    participant A as Autor da jogada
    participant API as API de partidas
    participant G as Game no dominio
    participant DB as PostgreSQL writer
    A->>API: Envia lance, expectedVersion e clientMoveId
    API->>DB: Carrega partida
    DB-->>API: Tabuleiro, vez e versao
    API->>G: Valida e aplica lance completo
    G-->>API: Novo estado ou erro de regra
    API->>DB: Salva estado, historico e outbox na mesma transacao
    alt comando aceito
        DB-->>API: Commit com nova versao
        API-->>A: Confirma resultado da jogada
    else conflito ou reenvio
        DB-->>API: Estado atual ou resultado anterior
        API-->>A: Conflito ou resposta idempotente
    end
```

O domínio valida o lance antes da tentativa de gravação. A transação só confirma se a versão do banco ainda for a esperada; uma disputa entre requisições pode gerar conflito mesmo após a validação do domínio. A outbox entra no mesmo commit **quando for implementada**. A resposta da API informa ao autor que o estado foi persistido; não significa que o adversário já recebeu o aviso.

### Adversário recebe o aviso

```mermaid
sequenceDiagram
    participant O as Worker de outbox
    participant R as ElastiCache
    participant W as ECS WebSocket
    participant B as Adversario
    participant API as API de partidas
    O->>R: Publica gameId e nova versao
    R->>W: Distribui aviso para tarefa conectada
    W-->>B: Informa que a partida mudou
    B->>API: GET estado se necessario
    API-->>B: Estado oficial
```

Este segundo fluxo é **futuro**. O worker lê o evento já confirmado; ElastiCache distribui o aviso entre tarefas, sem gravar a jogada novamente. O adversário pode buscar o estado por REST. Em caso de perda do aviso ou reconexão, essa consulta recupera a versão oficial. Outras abas do autor também podem receber o evento; a aba que já recebeu a resposta da API ignora a mesma versão.

## Componentes AWS e sua finalidade

Este é o **mapeamento planejado** dos componentes da aplicação para a implantação AWS. A [arquitetura alvo](aws-deployment.md) detalha a VPC; a [arquitetura global](aws-deployment-global.md) expande o desenho para contingência e grande escala.

| Componente AWS | Onde fica | Função no jogo |
|---|---|---|
| Route 53 | DNS global | Resolve `damas.highonline.com.br` para a distribuição CloudFront. |
| CloudFront | Borda global | CDN para os arquivos Angular; encaminha API e, futuramente, WebSocket à origem, sem cache para comandos ou estado privado. |
| AWS WAF | Associado ao CloudFront | Filtra requisições e aplica limites de taxa na borda; não substitui autenticação nem validação de jogadas. |
| S3 privado com OAC | Fora da VPC | Guarda o build estático do Angular acessível pela distribuição. |
| VPC e Internet Gateway | Região AWS | Delimitam a rede da aplicação e permitem entrada nas subnets públicas. |
| Subnets públicas e ALB | Pelo menos duas AZs | ALB recebe HTTPS/WSS e encaminha à tarefa ECS saudável; subnets públicas dão a rota de entrada. |
| Subnets privadas de aplicação e ECS Fargate | Pelo menos duas AZs | Executam o Spring Boot e, futuramente, o worker, sem IP público. Tarefas podem escalar, mas não são fonte oficial do estado. |
| Subnets privadas de dados e RDS PostgreSQL Multi-AZ | Região AWS | Persistem partida e histórico com disponibilidade entre AZs. O writer confirma as jogadas. |
| Security Groups | VPC | Limitam tráfego ao necessário entre ALB, ECS e banco. Restringir acesso direto ao ALB para não contornar o WAF da borda. |
| NAT Gateway ou VPC endpoints | Conforme a necessidade | Permitem saída controlada das tarefas privadas para dependências externas ou serviços AWS. |
| CloudWatch, IAM e Secrets Manager | Região AWS | Monitoramento, permissões mínimas e gestão de credenciais. |
| ElastiCache e worker de outbox | Etapa futura, subnets privadas | Propagam avisos entre tarefas WebSocket; a outbox no banco permite retentar a publicação. ElastiCache não controla a vez. |

**RDS PostgreSQL Multi-AZ** é o alvo inicial de implantação. **Aurora PostgreSQL Global Database** pertence ao estudo de expansão entre regiões; trocar de banco exige análise de custo, carga, migração e recuperação. Nenhum desses serviços foi provisionado por este documento.
