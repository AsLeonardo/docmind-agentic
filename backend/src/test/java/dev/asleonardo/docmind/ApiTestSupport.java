package dev.asleonardo.docmind;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

final class ApiTestSupport {

    private static final ObjectMapper JSON = new ObjectMapper();

    private ApiTestSupport() {
    }

    /** Cria um usuário novo e devolve o cabeçalho Authorization. */
    static String newUser(MockMvc mvc) throws Exception {
        String body = "{\"email\":\"" + UUID.randomUUID() + "@teste.com\",\"password\":\"senha-forte-123\"}";
        String response = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JSON.readTree(response).get("token").asText();
    }

    static long upload(MockMvc mvc, String auth, String filename, String content) throws Exception {
        var file = new MockMultipartFile("file", filename, "text/markdown", content.getBytes(StandardCharsets.UTF_8));
        String response = mvc.perform(multipart("/api/documents").file(file).header("Authorization", auth))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JSON.readTree(response).get("id").asLong();
    }
}
