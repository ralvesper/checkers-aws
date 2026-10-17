# Checkers AWS

Demonstração de damas brasileiras com Java 25, Spring Boot, Angular 22, PostgreSQL e WebSocket.

Demonstração publicada: https://damas.highonline.com.br/. Veja o [guia Hostinger](docs/hostinger.md) antes de atualizar o servidor. A versão hospedada pode diferir da main.

## Rodar com Docker

```bash
cp .env.example .env
# Edite .env: senha do banco e chave de sessão (openssl rand -hex 32).
bash scripts/dev.sh up
```

No Windows, copie o mesmo arquivo e execute `./scripts/dev.ps1 up` no PowerShell.
Abra http://localhost:8081, crie uma partida e abra o convite do adversário em outra aba/navegador.
Para parar preservando o banco: `bash scripts/dev.sh down` ou `./scripts/dev.ps1 down`.
Veja [execução completa](docs/running.md), incluindo desenvolvimento sem containers e remoção de dados.

## Entregue

- Domínio puro: movimento de pedras/damas, captura obrigatória e múltipla, lei da maioria, coroação e vitória.
- API REST: criar/consultar partida e enviar um lance completo; turno, versão e reenvio idempotente.
- PostgreSQL: migrações Flyway, estado e histórico na mesma transação, controle otimista entre conexões.
- Angular: tabuleiro responsivo, convite, orientação por jogador, seleção de caminhos e confirmação.
- WebSocket: snapshots confirmados, isolamento de salas, reconexão e recuperação por REST.
- Docker Compose: frontend Nginx, API e banco com volume persistente, saúde e scripts WSL/PowerShell.
- Terraform: rede em duas AZs e aplicação regional opcional com ECS, RDS, ALB HTTPS, S3/CloudFront e WAF.
- CI: testes Java/PostgreSQL, frontend, navegadores, containers e Terraform simulado.

## Organização

| Pasta | Conteúdo |
|---|---|
| backend | Domínio, aplicação, REST, WebSocket, JDBC e migrações |
| frontend | Interface Angular e testes de navegador |
| infrastructure | Terraform em etapas; nenhum recurso provisionado neste trabalho |
| scripts | Iniciar, parar, consultar e remover containers locais |
| docs | Contratos, regras, operação e decisões de arquitetura |

## Limites do MVP

Sessões de visitante assinadas autorizam os assentos; convites são de uso único. Não há contas nem recuperação de acesso. Empates oficiais, relógio,
matchmaking, ranking, bot e histórico navegável ficam para evolução. O WebSocket atende uma instância;
antes de escalar horizontalmente, implementar outbox e distribuição de eventos. O perfil padrão do backend
usa memória; o Compose usa PostgreSQL. Não declarar aderência completa às regras oficiais.

Terraform validado não significa AWS implantada. [Infraestrutura](infrastructure/README.md) documenta entradas,
restrição por IP, custos, plano e teardown. Não há apply nem deploy automático.

## Documentação

- [Implantar e operar no Hostinger](docs/hostinger.md)
- [Rodar e testar](docs/running.md)
- [API REST](docs/api.md) e [requests.http](backend/requests.http)
- [PostgreSQL](docs/postgresql.md) e [tempo real](docs/realtime.md)
- [Regras](docs/rules/README.md), [roadmap](docs/roadmap.md) e [diagramas](docs/diagrams/README.md)

Nunca enviar credenciais, arquivos .env, planos ou estado Terraform ao GitHub.
