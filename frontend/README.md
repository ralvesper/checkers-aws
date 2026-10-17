# Frontend

Aplicação Angular 22.2.3. Requer Node 24.15+ (24 LTS), npm e backend Java 25 em localhost:8080.

```bash
npm ci
npm start
```

Abra http://localhost:4200, crie uma partida e abra o convite do adversário em outra aba ou navegador.
O link da própria aba permanece na URL para recuperar a sessão ao recarregar. O convite contém a
identidade do adversário: é um recurso do protótipo local, não autenticação nem autorização segura.

Selecione a peça, percorra os pousos destacados e confirme somente depois de completar o lance.
O tabuleiro das pretas é girado. Damas têm uma coroa. O backend fornece os lances legais e decide o resultado.
Em caso de falha de rede, "Tentar o mesmo lance" reutiliza o clientMoveId original para evitar duplicação.
Conflitos de versão recarregam o estado oficial e cancelam a seleção antiga.

O dev server encaminha /api ao backend. Não há necessidade de CORS aberto.
O WebSocket atualiza o adversário automaticamente. A interface mostra AO VIVO ou RECONECTANDO;
reconexões e lacunas de versão recuperam o estado por REST. Veja [realtime.md](../docs/realtime.md).

```bash
npm test
npm run build
npx playwright install chromium
npm run test:e2e
```

O teste de navegador exige a API rodando e inicia o frontend automaticamente.
O CI testa dois jogadores por HTTP real, sessão após reload e ausência de overflow em tela móvel.

