# 001: Respostas com fontes

Especificação da funcionalidade central do DocMind, escrita em [EARS](https://alistairmavin.com/ears/) (Easy Approach to Requirements Syntax) para que cada requisito seja verificável por um teste.

## Contexto

Quem usa quer respostas rápidas sobre documentos internos (manuais, políticas), mas só confia se puder conferir de onde a resposta veio. Uma resposta inventada é pior que nenhuma resposta.

## Requisitos

| ID | Tipo EARS | Requisito | Verificado por |
|---|---|---|---|
| REQ-01 | Evento | **Quando** o usuário envia um arquivo PDF, Markdown ou TXT, o sistema **deve** dividir o texto em trechos, gerar embeddings e listá-lo com a quantidade de trechos. | `RagFlowTest.deletingADocumentRemovesItsChunks`, E2E |
| REQ-02 | Indesejado | **Se** o arquivo enviado tiver outro formato, **então** o sistema **deve** recusar com HTTP 415 e não gravar nada. | `RagFlowTest.rejectsUnsupportedFormat` |
| REQ-03 | Evento | **Quando** o usuário faz uma pergunta, o sistema **deve** enviar as fontes recuperadas antes do primeiro trecho da resposta. | `RagFlowTest.chatStreamsSourcesThenTokensAndPutsSourcesInPrompt` |
| REQ-04 | Ubíquo | O sistema **deve** marcar cada afirmação da resposta com o número da fonte, e cada número **deve** abrir o trecho original na interface. | E2E `responde com citação clicável...` |
| REQ-05 | Indesejado | **Se** nenhum trecho relevante for encontrado, **então** o sistema **deve** responder que não encontrou a informação **e não deve** chamar o LLM. | `RagFlowTest.chatWithoutDocumentsDoesNotCallTheModel`, E2E |
| REQ-06 | Ubíquo | O sistema **deve** tratar o conteúdo dos documentos como dado, delimitado no prompt, e instruir o modelo a ignorar ordens contidas nele. | `RagFlowTest` (prompt contém `<fontes>`) |
| REQ-07 | Ubíquo | O sistema **deve** restringir a busca vetorial aos documentos do usuário autenticado, aplicando o filtro dentro da consulta. | `RagFlowTest.searchOnlyReturnsTheCallersOwnDocuments`, E2E |
| REQ-08 | Evento | **Quando** o usuário apaga um documento, o sistema **deve** apagar também todos os seus embeddings. | `RagFlowTest.deletingADocumentRemovesItsChunks` |
| REQ-09 | Indesejado | **Se** um usuário tentar apagar documento de outro, **então** o sistema **deve** responder 404, sem revelar que o documento existe. | `RagFlowTest.cannotDeleteSomeoneElsesDocument` |
| REQ-10 | Indesejado | **Se** a geração de embeddings falhar durante o envio, **então** o sistema **deve** desfazer o registro do documento e informar o erro. | revisão de código (`IngestionService`), teste pendente |
| REQ-11 | Estado | **Enquanto** a resposta estiver sendo gerada, a interface **deve** permitir interromper o streaming. | manual, teste pendente |

## Fora do escopo

- OCR de PDFs escaneados (hoje o sistema responde que não encontrou texto).
- Compartilhar documentos entre usuários.

## Métricas de qualidade

A recuperação é medida com `eval/run_eval.py` (hit@1, hit@3, MRR). Mudanças em chunking, embeddings ou top-k precisam mostrar o antes e depois no PR.
