package com.rep.funcional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Solo los intentos fallidos cuentan para el bloqueo de inicio de sesión. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.security.rate-limit.login-per-15min=3",
        "app.bootstrap.admin-password=TestAdminPassword123"
})
class LimiteLoginTest {
    @Autowired private MockMvc mvc;

    @Test
    void losLoginsCorrectosNoBloqueanYLosFallidosSi() throws Exception {
        // Antes: 5 inicios de sesión correctos bloqueaban al usuario 15 minutos
        for (int i = 0; i < 6; i++) {
            login("TestAdminPassword123").andExpect(status().isOk());
        }
        for (int i = 0; i < 3; i++) {
            login("incorrecta").andExpect(status().isUnauthorized());
        }
        login("TestAdminPassword123")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("DEMASIADAS_SOLICITUDES"));
    }

    private org.springframework.test.web.servlet.ResultActions login(String password) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .with(r -> { r.setRemoteAddr("10.9.9.9"); return r; })
                .content("{\"identificacion\":\"22\",\"password\":\"" + password + "\"}"));
    }
}
