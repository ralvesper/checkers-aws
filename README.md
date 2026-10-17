# Checkers AWS

Jogo de damas online construído em etapas com Java 25, Spring Boot, Angular e Terraform.

```text
backend/         API e regras do jogo (Spring Boot)
frontend/        aplicação Angular (etapa posterior)
infrastructure/  infraestrutura AWS em Terraform (etapa posterior)
```

## Etapa atual: domínio jogável, API REST e PostgreSQL local

O domínio está em [backend/README.md](backend/README.md): agregados Partida e Jogador, tabuleiro e lances completos de damas brasileiras. Inclui movimento, captura obrigatória e múltipla, lei da maioria, promoção e vitória. A [API REST local](docs/api.md) cria e consulta partidas e aceita lances com controle de versão e idempotência. O perfil padrão usa memória; o perfil [local-postgres](docs/postgresql.md) persiste estado e histórico com JDBC e migrações Flyway. O frontend Angular já permite jogar com dois participantes; a infraestrutura AWS será preparada em etapa própria. Empates e relógio ficam para uma etapa posterior.

Requisitos: JDK 25 e Maven 3.6.3 ou superior.

```bash
cd backend
mvn test
mvn spring-boot:run
```

## Próximas etapas

1. Revisar o recorte jogável de damas brasileiras e validar a experiência de lance completo.
2. Validar a API local e evoluir a identidade para autenticação real antes de hospedar.
3. Validar persistência, concorrência e reenvio no perfil PostgreSQL local.
4. Frontend Angular.
5. Terraform para infraestrutura AWS, revendo plano e custos antes de provisionar.

Não enviar credenciais nem arquivos de estado ao GitHub.


## Interface

Com a API em execução, abra outro terminal:

```bash
cd frontend
npm ci
npm start
```

Abra http://localhost:4200, crie uma partida e compartilhe o convite do adversário. Veja [frontend/README.md](frontend/README.md).
