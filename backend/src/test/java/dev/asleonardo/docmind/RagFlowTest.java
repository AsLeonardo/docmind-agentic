package dev.asleonardo.docmind;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static dev.asleonardo.docmind.ApiTestSupport.newUser;
import static dev.asleonardo.docmind.ApiTestSupport.upload;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(FakeAiConfig.class)
class RagFlowTest {

    static final String VPN = """
            # Manual da VPN
            Quando a VPN apresentar erro de autenticação, reinicie o cliente da VPN e confirme se a senha
            do domínio não expirou. Usuários em home office devem usar o endereço vpn.empresa.com.
            """;
    static final String PRINTER = """
            # Impressoras
            Para adicionar a impressora do RH, abra o painel de impressoras e selecione IMP-RH-01.
            Toner de reposição fica no almoxarifado do segundo andar.
            """;

    @Autowired
    MockMvc mvc;

    @Test
    void searchOnlyReturnsTheCallersOwnDocuments() throws Exception {
        String ana = newUser(mvc);
        String bruno = newUser(mvc);
        upload(mvc, ana, "vpn.md", VPN);
        upload(mvc, bruno, "impressoras.md", PRINTER);

        // Bruno pergunta sobre VPN: o documento da Ana é o mais parecido, mas não pode aparecer
        mvc.perform(get("/api/search").param("q", "erro de autenticação na VPN").header("Authorization", bruno))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].filename").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("impressoras.md"))));

        mvc.perform(get("/api/search").param("q", "erro de autenticação na VPN").header("Authorization", ana))
                .andExpect(jsonPath("$[0].filename").value("vpn.md"))
                .andExpect(jsonPath("$[0].n").value(1));
    }

    @Test
    void chatStreamsSourcesThenTokensAndPutsSourcesInPrompt() throws Exception {
        String ana = newUser(mvc);
        upload(mvc, ana, "vpn.md", VPN);

        MvcResult async = mvc.perform(post("/api/chat").header("Authorization", ana)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("{\"question\":\"Como resolvo erro de autenticação na VPN?\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();
        String body = mvc.perform(asyncDispatch(async)).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(body.indexOf("event:sources")).isLessThan(body.indexOf("event:token"));
        assertThat(body).contains("vpn.md").contains("{\"text\":\"o manual, \"}").contains("event:done");

        String prompt = FakeAiConfig.LAST_PROMPT.get().getInstructions().stream()
                .map(Message::getText).reduce("", String::concat);
        assertThat(prompt).contains("<fontes>").contains("[1] vpn.md").contains("vpn.empresa.com");
    }

    @Test
    void chatWithoutDocumentsDoesNotCallTheModel() throws Exception {
        String carla = newUser(mvc);
        FakeAiConfig.LAST_PROMPT.set(null);

        MvcResult async = mvc.perform(post("/api/chat").header("Authorization", carla)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"Qual a política de férias?\"}"))
                .andReturn();
        String body = mvc.perform(asyncDispatch(async)).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(body).contains("Não encontrei");
        assertThat(FakeAiConfig.LAST_PROMPT.get()).isNull();
    }

    @Test
    void deletingADocumentRemovesItsChunks() throws Exception {
        String ana = newUser(mvc);
        long id = upload(mvc, ana, "vpn.md", VPN);

        mvc.perform(get("/api/documents").header("Authorization", ana))
                .andExpect(jsonPath("$[0].chunkCount").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$[0].ownerId").doesNotExist());

        mvc.perform(delete("/api/documents/" + id).header("Authorization", ana)).andExpect(status().isNoContent());

        mvc.perform(get("/api/search").param("q", "VPN").header("Authorization", ana))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void cannotDeleteSomeoneElsesDocument() throws Exception {
        String ana = newUser(mvc);
        String bruno = newUser(mvc);
        long id = upload(mvc, ana, "vpn.md", VPN);

        mvc.perform(delete("/api/documents/" + id).header("Authorization", bruno)).andExpect(status().isNotFound());
    }

    @Test
    void rejectsUnsupportedFormat() throws Exception {
        String ana = newUser(mvc);
        var file = new org.springframework.mock.web.MockMultipartFile("file", "planilha.xlsx", "application/octet-stream", new byte[]{1, 2, 3});
        mvc.perform(multipart("/api/documents").file(file).header("Authorization", ana))
                .andExpect(status().isUnsupportedMediaType());
    }
}
