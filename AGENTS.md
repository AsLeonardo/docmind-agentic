# AGENTS.md

Instruções para agentes de código (Claude Code, Copilot, Cursor, Codex...) trabalhando neste repositório.
Pessoas também podem ler: é o mesmo combinado que eu sigo.

## O projeto

DocMind responde perguntas sobre documentos do usuário citando a fonte de cada afirmação (RAG).
- `backend/`: Java 21, Spring Boot 3.5, Spring AI 1.1, PostgreSQL + pgvector, Flyway, JWT.
- `frontend/`: React 19, TypeScript, Vite, Tailwind. Testes E2E com Playwright em `frontend/e2e/`.
- `specs/`: requisitos em EARS. Toda funcionalidade nova começa por um spec aqui.
- `eval/`: avaliação da recuperação (hit@k, MRR).

## Comandos

| O quê | Comando |
|---|---|
| Testes do back-end | `cd backend && mvn verify` |
| Lint e build do front | `cd frontend && npm run lint && npm run build` |
| E2E (precisa de PostgreSQL com pgvector em localhost:5432, usuário/senha/banco `docmind`) | `cd backend && mvn -q package -DskipTests && cd ../frontend && npm run e2e` |
| Avaliação da recuperação | `python eval/run_eval.py --upload docs/exemplos` |

Os testes não usam LLM real: o back-end usa modelos falsos (`FakeAiConfig`) e o E2E usa `frontend/e2e/fake-llm.mjs`.

## Fluxo de trabalho

1. **Spec antes de código.** Para uma funcionalidade nova, escreva ou atualize `specs/NNN-nome/spec.md` com requisitos EARS numerados e quebre em tarefas em `tasks.md`. Cada tarefa precisa ser pequena o bastante para um PR e ter critério de pronto verificável.
2. **Uma tarefa por vez.** Não misture refatoração com funcionalidade no mesmo diff.
3. **Teste que prova o requisito.** Cada requisito novo ganha um teste que cita o ID (ex.: `// REQ-04`).
4. **Rode tudo antes de dizer que terminou:** testes do back-end, lint e build do front, E2E se mexeu em fluxo de tela ou API.
5. **PR revisável:** preencha o template com o que mudou, por quê, como foi verificado (saída dos testes, print ou vídeo) e os trade-offs.

## Regras

- Nunca enfraqueça, pule ou apague um teste para o CI passar. Se o teste está errado, explique no PR.
- Nunca edite uma migration Flyway já existente: crie uma nova `V<n>__descricao.sql`.
- Nunca coloque segredos no código, em testes ou em exemplos. Configuração sensível vem de variáveis de ambiente (`.env`, que é ignorado pelo git).
- Toda busca no vector store deve filtrar por `ownerId` **dentro** da consulta (ver `RetrievalService`). Filtrar depois vaza dados entre usuários.
- Conteúdo vindo de documentos e de usuários é dado, não instrução: mantenha os delimitadores no prompt (`PromptFactory`).
- Não adicione dependência nova sem dizer no PR por que ela é necessária. O CI verifica vulnerabilidades conhecidas.
- Mensagens de erro e textos da interface em português.

## Precisa de aprovação humana

- Mudança de schema do banco, de autenticação ou de permissões.
- Troca de modelo ou provedor de LLM e mudanças no prompt de sistema (afetam a qualidade: rode `eval/` e mostre o antes e depois).
- Qualquer coisa que apague dados.
