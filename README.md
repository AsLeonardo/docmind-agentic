# DocMind

Assistente que responde perguntas sobre os **seus** documentos (PDF, Markdown, TXT) e **cita o trecho de onde tirou cada afirmação**. As respostas chegam em streaming, cada `[n]` no texto é clicável e abre o trecho original, e cada usuário só enxerga os próprios arquivos.

## Arquitetura

```
                    ┌──────────── ingestão ────────────┐
upload (PDF/MD/TXT) → leitura → chunking (250 tokens) → embeddings → pgvector
                                                               (metadado ownerId)

pergunta → embedding → busca vetorial filtrada por ownerId (top-k)
         → prompt com trechos numerados → LLM (streaming) → SSE → React
```

| Camada | Tecnologia |
|---|---|
| Front-end | React 19, TypeScript, Vite, Tailwind CSS 4 |
| API | Java 21, Spring Boot 3.5, Spring Security (JWT), Spring AI 1.1 |
| Dados | PostgreSQL 16 + pgvector (índice HNSW, distância cosseno), Flyway |
| LLM e embeddings | qualquer API compatível com OpenAI (padrão: Gemini, plano gratuito) |
| Infra | Docker Compose, Nginx, GitHub Actions |

## Decisões que valem explicar

- **Isolamento entre usuários dentro da busca vetorial.** Cada trecho é gravado com `ownerId` como metadado e a busca usa um filtro (`ownerId == usuário do JWT`) aplicado na própria consulta ao pgvector. Filtrar depois do top-k vazaria contexto e devolveria menos resultados. Há teste cobrindo isso.
- **Sem contexto, sem LLM.** Se a busca não encontra nada, a API responde "não encontrei" sem chamar o modelo: economiza tokens e elimina a chance de resposta inventada.
- **Citações verificáveis.** Os trechos vão numerados no prompt e o evento `sources` é enviado antes do primeiro token, então a interface mostra as fontes enquanto a resposta ainda está sendo escrita.
- **Streaming de ponta a ponta.** `ChatClient.stream()` do Spring AI → `Flux<ServerSentEvent>` no Spring MVC → Nginx com `proxy_buffering off` → `fetch` + `ReadableStream` no React (o `EventSource` nativo só faz GET). Cada token vai como JSON porque o protocolo SSE descarta o espaço inicial de uma linha `data:`.
- **Prompt injection.** O conteúdo dos documentos vai delimitado em `<fontes>` e o prompt de sistema manda tratá-lo como dado.
- **Consistência entre banco e vector store.** Os ids dos trechos ficam numa tabela própria para que apagar um documento apague seus embeddings; se a geração de embeddings falha no upload, o registro do documento é removido (compensação).
- **Provedor plugável.** Chat e embeddings podem vir de provedores diferentes (ex.: chat no Groq, embeddings no Gemini), só com variáveis de ambiente.

## Avaliação da recuperação

`eval/questions.jsonl` tem 25 perguntas sobre os documentos de exemplo em `docs/exemplos/` (uma base de conhecimento de TI fictícia), cada uma com o arquivo que deveria ser encontrado. O script cria um usuário, envia os documentos e mede **hit@k** (o arquivo certo apareceu entre os k primeiros?) e **MRR**:

```bash
python eval/run_eval.py --url http://localhost:8080 --upload docs/exemplos
```

| Embeddings | chunk | hit@1 | hit@3 | MRR |
|---|---|---|---|---|
| `gemini-embedding-001` (768 dims) | 250 | _a preencher_ | _a preencher_ | _a preencher_ |
| `gemini-embedding-001` (768 dims) | 500 | _a preencher_ | _a preencher_ | _a preencher_ |

Para testar outro tamanho de trecho, altere `docmind.rag.chunk-size` em `application.yml` e rode o script de novo.

## Rodando

```bash
cp .env.example .env    # coloque sua chave do Gemini e um JWT_SECRET
docker compose up --build
# http://localhost:8080 → crie uma conta e envie os arquivos de docs/exemplos
```

Sem Docker: PostgreSQL com a extensão `vector`, Java 21 e Node 22.

```bash
cd backend && LLM_API_KEY=... mvn spring-boot:run
cd frontend && npm install && npm run dev      # http://localhost:5173
```

## API

| Método | Rota | |
|---|---|---|
| `POST` | `/api/auth/register`, `/api/auth/login` | `{email, password}` → `{token}` |
| `POST` | `/api/documents` | multipart `file` → documento indexado |
| `GET` | `/api/documents` | documentos do usuário |
| `DELETE` | `/api/documents/{id}` | apaga o documento e seus embeddings |
| `POST` | `/api/chat` | `{question, history}` → `text/event-stream` com eventos `sources`, `token`, `done` |
| `GET` | `/api/search?q=&k=` | só a recuperação, sem LLM (usado na avaliação) |

## Testes

Os testes sobem a aplicação inteira com H2, um vector store em memória e modelos falsos (embeddings determinísticos por "saco de palavras" e um chat que responde em pedaços), então rodam sem chave e sem Docker:

- `AuthTest`: cadastro, login, e-mail normalizado (sem conta duplicada por maiúsculas), senha curta, rotas protegidas e token inválido.
- `RagFlowTest`: isolamento entre usuários na busca, ordem dos eventos SSE (fontes antes dos tokens), fontes presentes no prompt, nenhuma chamada ao modelo sem contexto, exclusão removendo os trechos, exclusão de documento alheio (404) e formato não suportado (415).

**E2E (Playwright)**: sobe o sistema inteiro (PostgreSQL + pgvector, back-end, front-end e um LLM falso determinístico em `frontend/e2e/fake-llm.mjs`) e testa pelo navegador: resposta com citação clicável que abre a fonte, aviso de "não encontrei" sem documentos e isolamento entre dois usuários.

## Quality gates no CI

| Job | O que bloqueia o merge |
|---|---|
| `backend` | `mvn verify` (testes de integração com H2 e modelos falsos) |
| `frontend` | `eslint` e build com TypeScript estrito |
| `e2e` | os testes Playwright contra o sistema rodando |
| `security` | segredo no histórico (gitleaks) e vulnerabilidade CRITICAL com correção disponível ou Dockerfile inseguro (Trivy); HIGH aparece no relatório |

Marque os quatro jobs como obrigatórios em *Settings → Branches → Branch protection* para que funcionem como gate. O Dependabot abre PRs semanais de atualização, e os contêineres rodam sem root.

## Como este projeto é desenvolvido com agentes

- **`AGENTS.md`** diz a qualquer agente de código como trabalhar aqui: comandos, fluxo, o que nunca fazer (pular teste, editar migration, filtrar fora da consulta vetorial, commitar segredo) e o que precisa de aprovação humana.
- **Spec antes de código**: `specs/001-respostas-com-fontes/spec.md` traz os requisitos em EARS (*Quando…, o sistema deve…*; *Se…, então…*), cada um ligado ao teste que o verifica. `tasks.md` quebra a funcionalidade em tarefas do tamanho de um PR, com critério de pronto.
- **PR com evidência**: o template pede como foi verificado (saída de teste, print, avaliação antes/depois), os trade-offs, e uma confirmação de que quem abriu o PR consegue explicar cada linha do diff gerado.

## Deploy na AWS

Uma instância EC2 `t3.small` (ou `t2.micro` no free tier, com swap) com Docker:

```bash
sudo dnf install -y docker git && sudo systemctl enable --now docker
sudo mkdir -p /usr/local/lib/docker/cli-plugins
sudo curl -SL https://github.com/docker/compose/releases/download/v2.29.7/docker-compose-linux-x86_64 \
  -o /usr/local/lib/docker/cli-plugins/docker-compose && sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose
git clone https://github.com/AsLeonardo/DocMind && cd DocMind
cp .env.example .env && nano .env
PORT=80 sudo docker compose up -d --build
```

Libere a porta 80 no Security Group. Próximos passos naturais: RDS com pgvector no lugar do contêiner de banco e HTTPS com um Application Load Balancer.
