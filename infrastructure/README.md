# Infraestrutura

Terraform preparado para um laboratório regional. Nenhum recurso AWS foi provisionado por este trabalho.
Requer Terraform 1.9+ (CI: 1.14.4), AWS CLI autenticada e permissões na conta de destino para planejar/aplicar.

## Etapas

1. **Padrão, rede:** VPC, Internet Gateway e seis subnets em duas AZs: públicas, aplicação e dados.
   Públicas têm rota ao IGW; dados têm somente a rota local; aplicação recebe NAT somente na etapa completa.
   Cada subnet usa `cidrsubnet(vpc_cidr, 8, índice)`; o padrão /16 gera /24. Não há EC2, ECS, ALB, RDS ou NAT nessa etapa.
2. **Registry:** `enable_registry=true` cria ECR privado para publicar uma imagem com tag imutável.
3. **Aplicação:** `enable_application=true` adiciona NAT único, ECS Fargate privado, ALB HTTPS, RDS PostgreSQL privado,
   Secrets Manager gerenciado pelo RDS, logs CloudWatch, S3 privado/OAC e CloudFront com WAF restrito a IPs de teste.

```bash
terraform init
terraform fmt -check -recursive
terraform validate
terraform test
terraform plan -out=network.plan
```

Os testes usam providers simulados: não acessam a conta e não criam recursos. O plan real exige credenciais.
Revise o plano e os custos da região antes de executar qualquer apply. Não há workflow de deploy automático.

## Preparar imagem e entradas

Após revisar e aplicar a etapa de registry, obtenha `terraform output -raw ecr_repository`, faça login com
`aws ecr get-login-password` e publique o backend construído com `docker build -t REPOSITORY:TAG ../backend`.
Use tag exclusiva ou digest. O Terraform não publica imagens nem executa push automaticamente.

Crie localmente um arquivo `lab.tfvars` (ignorado pelo Git), com seus valores reais:

```hcl
enable_registry    = true
enable_application = true
backend_image      = "ACCOUNT.dkr.ecr.REGION.amazonaws.com/checkers-lab-backend:YOUR_TAG"
api_hostname       = "origin-damas.seu-dominio.com.br"
route53_zone_id    = "SUA_ZONE"
certificate_arn    = "ARN_ACM_VALIDADO_NA_MESMA_REGIAO_DO_ALB"
trusted_ipv4_cidrs = ["SEU_IP_PUBLICO/32"]
```

O certificado ACM deve existir, estar validado e cobrir o hostname de origem. O alias Route 53 aponta esse nome ao ALB.
O frontend usa o hostname HTTPS padrão do CloudFront. A lista de IPs é obrigatória e não permite 0.0.0.0/0.
Apenas a prefix list de origens CloudFront entra no ALB; um cabeçalho secreto adicional seleciona a regra de encaminhamento.
O bucket aceita leitura apenas da distribuição via OAC. A API não recebe cache; o CloudFront remove o prefixo /api.
O WebSocket usa o mesmo caminho e HTTPS/WSS. A aplicação aceita como origem somente o domínio da distribuição.

```bash
terraform plan -var-file=lab.tfvars -out=application.plan
# Depois de revisar o plano e os custos, o operador pode aplicar o plano salvo.
```

Depois da implantação autorizada, publique `frontend/dist/checkers/browser/` no bucket informado pelo output.
Use cache longo nos arquivos com hash e `Cache-Control: no-cache` no index.html; invalide `/index.html` no CloudFront.
Verifique no hostname do frontend `/api/actuator/health/readiness`, dois jogadores, persistência após reiniciar tarefa,
HTTPS e conexão WSS. Esses testes em AWS ainda não foram executados.

## Limites e operação

O laboratório usa sessões de visitante assinadas; WAF por IP não substitui autorização, contas ou quotas. Não é uma implantação pública de produção.
ECS mantém **uma tarefa** e substitui a antiga antes da nova, com uma interrupção durante deploy, para não dividir o hub WebSocket.
Antes de habilitar múltiplas tarefas, implementar outbox + distribuição de eventos. A reconexão recupera o estado pelo PostgreSQL.
O RDS padrão é Single-AZ para laboratório; `db_multi_az=true` habilita Multi-AZ. NAT único também é uma escolha de laboratório.
O cliente JDBC desta etapa ainda não tem pool. Não há autoscaling nem arquitetura global implementada.

A senha do banco é gerenciada pelo RDS; a chave de sessão é gerada no módulo. Ambas são entregues via Secrets Manager e não ficam nos tfvars.
O estado Terraform contém dados sensíveis, inclusive o token da origem: use backend privado e protegido antes de uso compartilhado.
Não envie `.tfstate`, planos, `.tfvars`, credenciais ou `.terraform/` ao GitHub. O lockfile de providers deve ser versionado.

**Custos a revisar:** NAT (horas e dados), IPv4 público, ALB/LCU, Fargate, RDS e armazenamento/backups, Secrets Manager,
CloudWatch, ECR/S3, CloudFront/transferência, WAF e Route 53. Há custos mesmo sem jogadores conectados; não assumir free tier.
Consulte o AWS Pricing Calculator e a região escolhida antes do apply. Nenhuma estimativa numérica ou gasto foi aprovado aqui.

Para teardown, pare/publicações, revise `terraform plan -destroy` e remova objetos/versionamentos do bucket conforme sua política.
O RDS vem com proteção contra exclusão: é necessário desativá-la explicitamente antes de destruir. Um snapshot final é obrigatório;
escolha `final_snapshot_identifier` exclusivo se já houver snapshot com o mesmo nome. Snapshots e imagens retidas podem continuar gerando custos.

