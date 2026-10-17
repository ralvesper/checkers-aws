# Checkers AWS

Jogo de damas online construído em etapas com Java 25, Spring Boot, Angular e Terraform.

```text
backend/         API e regras do jogo (Spring Boot)
frontend/        aplicação Angular (etapa posterior)
infrastructure/  infraestrutura AWS em Terraform (etapa posterior)
```

## Etapa atual: domínio jogável e API REST local

O domínio está em [backend/README.md](backend/README.md): agregados Partida e Jogador, tabuleiro e lances completos de damas brasileiras. Inclui movimento, captura obrigatória e múltipla, lei da maioria, promoção e vitória. A [API REST local](docs/api.md) cria e consulta partidas e aceita lances com controle de versão e idempotência em memória. Ainda não há persistência durável, frontend ou infraestrutura AWS. Empates e relógio ficam para uma etapa posterior.

Requisitos: JDK 25 e Maven 3.6.3 ou superior.

```bash
cd backend
mvn test
mvn spring-boot:run
```

## Próximas etapas

1. Revisar o recorte jogável de damas brasileiras e validar a experiência de lance completo.
2. Validar a API local e evoluir a identidade para autenticação real antes de hospedar.
3. Persistência PostgreSQL e migrações.
4. Frontend Angular.
5. Terraform para infraestrutura AWS, revendo plano e custos antes de provisionar.

Não enviar credenciais nem arquivos de estado ao GitHub.

