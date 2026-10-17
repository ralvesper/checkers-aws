# Atualização em tempo real

O endpoint WebSocket `/games/{id}/stream` envia snapshots confirmados. O cliente oferece os subprotocolos
`checkers.v1` e `guest.TOKEN`; o servidor seleciona checkers.v1 e valida o token e o assento no handshake.
Não há token em query string nem confiança em playerId fornecido pelo cliente. Use WSS na hospedagem.
Não registre Sec-WebSocket-Protocol ou Authorization nos logs. Uma sessão inválida recebe 401;
sem assento na sala, recebe 403. A validade é verificada ao abrir/reabrir a conexão.

O controlador publica depois de o caso de uso retornar: a transação do banco já confirmou estado e histórico.
Uma falha ou rollback não publica snapshot. Um recibo idempotente pode repetir uma versão, que o Angular ignora.
O servidor limita o buffer e fecha conexões que falham ou ficam lentas. Os snapshots não incluem credenciais.

O Angular aplica versões consecutivas mais novas, ignora versões antigas e duplicadas e consulta REST
quando encontra uma lacuna. Ao perder a conexão, reconecta com espera progressiva de 1 a 15 segundos e
reconsulta o estado oficial. Eventos de uma sala anterior são ignorados. O botão Atualizar permite recuperar
manualmente o estado. WebSocket não é o histórico nem a fonte oficial da partida.

## Limite de implantação

O hub desta etapa funciona em **uma instância da API**. A configuração inicial de ECS deve manter uma tarefa.
Antes de escalar horizontalmente, implementar outbox na transação da jogada e distribuição entre instâncias
(por exemplo, worker + Redis/ElastiCache), conforme os diagramas. Publicação atual é best effort:
uma queda entre commit e broadcast é recuperada ao reconectar/consultar REST, não por entrega durável.
Não há promessa de entrega exatamente uma vez. PostgreSQL mantém estado e recibos independentemente do socket.

Origens adicionais devem ser explicitamente configuradas em `CHECKERS_WEBSOCKET_ALLOWED_ORIGINS`;
o Compose permite apenas os endereços locais do frontend e o laboratório AWS permite o hostname do CloudFront.

O CI verifica handshake de participante, isolamento de sala, snapshot após aceite e recuperação na reconexão.
Playwright testa dois navegadores, interrupção de rede, atualização sem botão e recarregamento.
