# Roadmap — MVP local entregue

| Etapa | Estado | Evidência |
|---|---|---|
| Domínio jogável | Implementado | Regras brasileiras, agregados, lances completos e testes |
| API e persistência | Implementado | REST, JDBC, migrações, versão otimista, idempotência e testes em PostgreSQL |
| Interface Angular | Implementado | Tabuleiro, turnos, captura múltipla, convite e Playwright com dois jogadores |
| Tempo real | Implementado em uma instância | WebSocket após commit, reconexão e recuperação por REST |
| Execução local | Implementado | Compose, Nginx, healthchecks e scripts Bash/PowerShell |
| Rede AWS | Código preparado | VPC, seis subnets, rotas e IGW; padrão sem aplicação/NAT |
| Aplicação AWS | Código preparado, não implantado | ECS de uma tarefa, RDS, ALB HTTPS, S3/CloudFront, WAF por IP e secrets |

O contrato de turno completo foi adotado e exercitado na interface: o jogador compõe todos os pousos antes
 de confirmar. A regra de empate e o relógio ficaram fora do recorte inicial, conforme a decisão de escopo.

## Evolução além do MVP

- Autenticação real e convites seguros antes de abrir acesso público.
- Regras oficiais de empate, desistência e relógio.
- Cadastro, matchmaking, ranking e histórico navegável, conforme necessidade.
- Pool de conexões, paginação e consulta de recibo específico.
- Outbox transacional e distribuição entre instâncias antes de escalar o hub.
- Planejamento e implantação AWS autorizada, smoke tests HTTPS/WSS e análise de custos.
- Multi-AZ, autoscaling e recuperação global conforme objetivos reais de disponibilidade.

Consulte [running.md](running.md) para executar e [infraestrutura](../infrastructure/README.md) para os limites da preparação AWS.
