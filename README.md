# Checkers AWS

Jogo de damas online construído passo a passo com Java 25, Spring Boot e Terraform. Esta primeira etapa contém apenas o projeto Spring Boot e uma verificação de inicialização; nenhuma infraestrutura AWS é criada ainda.

## Executar

Requisitos: JDK 25 e Maven 3.6.3 ou superior.

```bash
mvn test
mvn spring-boot:run
```

A aplicação inicia em `http://localhost:8080`. Ainda não há tabuleiro nem endpoints de partidas.

## Etapas de aprendizado

1. Base Java 25 + Spring Boot e CI (**atual**).
2. Modelo de partida, regras e testes de domínio.
3. API de criação de partida e jogadas com controle de versão.
4. Persistência PostgreSQL e migrações.
5. Frontend e atualização dos jogadores.
6. Terraform: VPC, duas zonas, subnets e rotas.
7. Terraform: aplicação, banco, balanceador, observabilidade e DNS; estimar custos antes de provisionar.

A infraestrutura será definida como código antes de qualquer `terraform apply`. Nunca commitar `terraform.tfstate`, credenciais ou valores secretos.
