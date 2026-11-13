package com.rep.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rep.model.Curso;
import com.rep.model.Materia;
import com.rep.model.Usuario;
import com.rep.repositories.*;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Alta de usuarios desde la administración: debe crear el subtipo correcto para que
 * los profesores puedan recibir asignaciones y los estudiantes tengan curso.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminUsuarioApiTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private SecretKey secretKey;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ProfesorRepository profesorRepository;
    @Autowired private EstudianteRepository estudianteRepository;
    @Autowired private CursoRepository cursoRepository;
    @Autowired private MateriaRepository materiaRepository;

    private String adminToken;
    private Curso curso;
    private Materia materia;

    @BeforeEach
    void setUp() {
        Usuario admin = usuarioRepository.findByIdentificacion("22").orElseThrow(); // creado por DataLoader
        adminToken = "Bearer " + Jwts.builder()
                .setSubject(admin.getIdentificacion())
                .claim("authorities", List.of("ADMIN"))
                .setExpiration(new Date(System.currentTimeMillis() + 600_000))
                .signWith(secretKey).compact();

        String sufijo = UUID.randomUUID().toString().substring(0, 5);
        curso = new Curso();
        curso.setGrado(1 + (int) (Math.random() * 12));
        curso.setGrupo(sufijo);
        curso = cursoRepository.save(curso);
        materia = new Materia();
        materia.setNombre("Mat-" + sufijo);
        materia = materiaRepository.save(materia);
    }

    @Test
    void crearProfesorPermiteAsignarleMaterias() throws Exception {
        String ident = String.valueOf(System.nanoTime()).substring(0, 9);
        String body = usuarioJson(ident, "PROFESOR", "CC", 35, null);

        String response = mvc.perform(post("/api/admin/usuarios").header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(response).doesNotContain("contrase");
        Long id = mapper.readTree(response).path("id").asLong();
        assertThat(profesorRepository.existsById(id)).isTrue();

        mvc.perform(post("/api/admin/asignaciones").header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"profesorId\":%d,\"materiaId\":%d,\"cursoId\":%d}"
                                .formatted(id, materia.getId(), curso.getId())))
                .andExpect(status().isOk());
    }

    @Test
    void crearEstudianteRequiereCursoYLoAsigna() throws Exception {
        String ident = String.valueOf(System.nanoTime()).substring(0, 10);
        mvc.perform(post("/api/admin/usuarios").header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(usuarioJson(ident, "ESTUDIANTE", "TI", 14, null)))
                .andExpect(status().isBadRequest());

        String response = mvc.perform(post("/api/admin/usuarios").header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(usuarioJson(ident, "ESTUDIANTE", "TI", 14, curso.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("ESTUDIANTE"))
                .andReturn().getResponse().getContentAsString();
        Long id = mapper.readTree(response).path("id").asLong();
        assertThat(estudianteRepository.findById(id)).get()
                .satisfies(e -> assertThat(e.getCurso().getId()).isEqualTo(curso.getId()));
    }

    @Test
    void reglaDocumentoSegunEdadSeMantiene() throws Exception {
        String ident = String.valueOf(System.nanoTime()).substring(0, 8);
        mvc.perform(post("/api/admin/usuarios").header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(usuarioJson(ident, "PROFESOR", "CC", 15, null)))
                .andExpect(status().isBadRequest());
    }

    private static String usuarioJson(String identificacion, String rol, String tipo, int edad, Long cursoId) {
        return """
                {"nombre":"Laura","apellido":"Gómez","correo":"%s@test.local","identificacion":"%s",
                 "tipoIdentificacion":"%s","contraseña":"Password123","rol":"%s","activo":true,
                 "edad":%d,"sexo":"FEMENINO","cursoId":%s}
                """.formatted(identificacion, identificacion, tipo, rol, edad, cursoId);
    }
}
