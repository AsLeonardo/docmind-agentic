package dev.asleonardo.docmind;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(FakeAiConfig.class)
class AuthTest {

    @Autowired
    MockMvc mvc;

    @Test
    void registerThenLogin() throws Exception {
        String body = "{\"email\":\"Ana@Empresa.com\",\"password\":\"senha-forte-123\"}";
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty());

        // e-mail é normalizado: maiúsculas não criam outra conta
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(body.replace("Ana@Empresa.com", "ana@empresa.com")))
                .andExpect(status().isConflict());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void wrongPasswordIsUnauthorized() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ninguem@empresa.com\",\"password\":\"qualquer-coisa\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shortPasswordIsRejected() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"b@empresa.com\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void protectedRoutesRequireToken() throws Exception {
        mvc.perform(get("/api/documents")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/documents").header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isUnauthorized());
    }
}
