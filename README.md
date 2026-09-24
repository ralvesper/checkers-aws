# Checkers AWS

Jogo de damas online construído em etapas com Java 25, Spring Boot, Angular e Terraform.

```text
backend/         API e regras do jogo (Spring Boot)
frontend/        aplicação Angular (próxima etapa)
infrastructure/  infraestrutura AWS em Terraform (etapa posterior)
```

## Etapa atual: base do backend

Requisitos: JDK 25 e Maven 3.6.3 ou superior.

```bash
cd backend
mvn test
mvn spring-boot:run
```

A aplicação inicia em `http://localhost:8080`. Ainda não há tabuleiro nem endpoints de partidas. Não há recursos AWS criados.

## Próximas etapas

1. Modelo de partida, regras e testes de domínio.
2. API de criação de partida e jogadas com controle de versão.
3. Persistência PostgreSQL e migrações.
4. Frontend Angular e atualização dos jogadores.
5. Terraform: VPC, subnets e rotas; depois aplicação, banco e DNS.

Antes de provisionar infraestrutura, revisar o `terraform plan` e os custos previstos. Não enviar credenciais nem arquivos de estado ao GitHub.
