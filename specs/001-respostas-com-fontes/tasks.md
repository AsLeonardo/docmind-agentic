# 001: Tarefas

Cada tarefa é pequena o bastante para um PR e para um agente executar sozinho, com critério de pronto verificável.

| # | Tarefa | Requisitos | Pronto quando | Status |
|---|---|---|---|---|
| T1 | Ingestão: leitura de PDF/MD/TXT, chunking, embeddings com metadado `ownerId` | REQ-01, REQ-02 | upload lista o documento com nº de trechos; formato inválido retorna 415 | feito |
| T2 | Busca vetorial filtrada por usuário + endpoint `/api/search` | REQ-07 | teste de isolamento entre dois usuários passa | feito |
| T3 | Chat em SSE: evento `sources` antes dos tokens, `done` no fim | REQ-03 | teste verifica a ordem dos eventos | feito |
| T4 | Prompt com fontes numeradas e delimitadas | REQ-04, REQ-06 | prompt enviado ao modelo contém `<fontes>` e `[1]` | feito |
| T5 | Curto-circuito sem contexto | REQ-05 | teste garante que o modelo não é chamado | feito |
| T6 | Exclusão com remoção dos embeddings e checagem de dono | REQ-08, REQ-09 | busca vazia após exclusão; 404 para documento alheio | feito |
| T7 | Interface: citações clicáveis e lista de fontes | REQ-04 | E2E clica no `[1]` e vê o trecho | feito |
| T8 | E2E como gate de merge no CI | todos | job `e2e` obrigatório no PR | feito |
| T9 | Teste para a compensação de falha de embeddings | REQ-10 | teste com vector store que lança erro: documento não fica no banco | a fazer |
| T10 | E2E do botão "Parar" | REQ-11 | E2E interrompe uma resposta lenta do LLM falso | a fazer |
