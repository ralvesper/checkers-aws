# Executar o MVP

## Docker Compose — caminho recomendado

Requer Docker com Compose v2 e acesso às imagens base. Na raiz do repositório:

```bash
cp .env.example .env
# Edite .env e escolha a senha local.
bash scripts/dev.sh up
```

No PowerShell:

```powershell
Copy-Item .env.example .env
# Edite .env e escolha a senha local.
./scripts/dev.ps1 up
```

Abra http://localhost:8081. Crie uma partida e abra o convite em outra aba/navegador.
Frontend, API e PostgreSQL são construídos/iniciados pelo Compose, com verificação de saúde.
A API é acessada pelo Nginx em /api; não fica exposta diretamente no host.
O PostgreSQL usa volume persistente e porta local 5432, configurável em CHECKERS_POSTGRES_PORT.
Se mudar a senha de um volume já inicializado, também será necessário alterar a senha no banco; mudar .env não a troca automaticamente.

| Ação | WSL / Bash | PowerShell |
|---|---|---|
| Iniciar/reconstruir | `bash scripts/dev.sh up` | `./scripts/dev.ps1 up` |
| Ver containers | `bash scripts/dev.sh status` | `./scripts/dev.ps1 status` |
| Ver logs | `bash scripts/dev.sh logs` | `./scripts/dev.ps1 logs` |
| Parar, preservando dados | `bash scripts/dev.sh down` | `./scripts/dev.ps1 down` |
| Apagar o banco local | `bash scripts/dev.sh reset --delete-data` | `./scripts/dev.ps1 reset -DeleteData` |

## Desenvolvimento sem containers de aplicação

Java 25 e Maven 3.6.3+ no backend, Node 24.15+ no frontend:

```bash
# terminal 1
cd backend
mvn spring-boot:run
# terminal 2, na raiz
cd frontend
npm ci
npm start
```

Abra http://localhost:4200. O perfil padrão guarda dados em memória e perde partidas ao reiniciar.
Para banco durável com aplicações fora de containers, siga [postgresql.md](postgresql.md).

## O que o MVP entrega

Damas brasileiras 8x8: movimentos, captura obrigatória, lei da maioria, cadeias, promoção e vitória.
Dois participantes por convite, tabuleiro responsivo e orientado por jogador, confirmação de lance completo,
controle de turno, versão, reenvio idempotente, atualização automática e recuperação na reconexão.
PostgreSQL mantém estado e lances em uma transação. Scripts e containers oferecem execução reproduzível.

O link identifica um participante apenas no protótipo. Não há login seguro, regras oficiais de empate,
relógio, matchmaking, ranking, bot ou histórico navegável. Não declarar aderência completa às regras oficiais.
O hub de eventos atende uma instância; escala horizontal exige outbox e distribuição entre processos.

## Verificação

O CI executa backend com PostgreSQL real, testes do frontend, build Angular, Playwright com interrupção de rede,
construção das imagens Docker e o mesmo teste de navegador através do Nginx/Compose.
Um smoke test adicional reinicia a API e confere por HTTP que estado e recibo de idempotência permanecem no banco.
Também executa terraform fmt/validate/test sem criar recursos AWS.
Terraform preparado não equivale a implantação validada: DNS, certificados, conta, custos e smoke test AWS dependem da etapa de implantação.
