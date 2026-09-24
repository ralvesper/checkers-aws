# Checkers AWS

Jogo de damas online construído em etapas com Java 25, Spring Boot, Angular e Terraform.

```text
backend/         API e regras do jogo (Spring Boot)
frontend/        aplicação Angular (etapa posterior)
infrastructure/  infraestrutura AWS em Terraform (etapa posterior)
```

## Etapa atual: domínio do backend

O modelo inicial está em [backend/README.md](backend/README.md): agregados Partida e Jogador, identidades e tabuleiro. Ainda não há movimento, API de partidas, frontend ou infraestrutura AWS.

Requisitos: JDK 25 e Maven 3.6.3 ou superior.

```bash
cd backend
mvn test
mvn spring-boot:run
```

## Próximas etapas

1. Definir a variante de damas e implementar jogadas dentro do agregado Partida.
2. Casos de uso e API com controle de versão.
3. Persistência PostgreSQL e migrações.
4. Frontend Angular.
5. Terraform para infraestrutura AWS, revendo plano e custos antes de provisionar.

Não enviar credenciais nem arquivos de estado ao GitHub.
