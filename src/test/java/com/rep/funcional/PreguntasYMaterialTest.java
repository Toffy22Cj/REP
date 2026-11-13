package com.rep.funcional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rep.model.*;
import com.rep.repositories.*;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Preguntas (todos los tipos, adjuntos) y material de estudio, con control de acceso. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.security.rate-limit.api-per-minute=10000",
        "app.storage.dir=${java.io.tmpdir}/rep-test-storage"
})
class PreguntasYMaterialTest {
    private static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private SecretKey secretKey;
    @Autowired private PasswordEncoder encoder;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private CursoRepository cursos;
    @Autowired private MateriaRepository materias;
    @Autowired private ProfesorMateriaRepository asignaciones;
    @Autowired private ActividadRepository actividades;

    private Profesor profesor;
    private Profesor otroProfesor;
    private Estudiante estudiante;
    private Estudiante estudianteOtroCurso;
    private Curso curso;
    private Materia materia;
    private Actividad actividad;

    @BeforeEach
    void setUp() {
        String s = UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        curso = cursos.save(curso(s));
        Curso otroCurso = cursos.save(curso(s.substring(0, 4) + "Z"));
        materia = new Materia();
        materia.setNombre("Materia " + s);
        materia = materias.save(materia);
        profesor = usuario(new Profesor(), Usuario.Rol.PROFESOR);
        otroProfesor = usuario(new Profesor(), Usuario.Rol.PROFESOR);
        estudiante = new Estudiante();
        estudiante.setCurso(curso);
        estudiante.setEdad(15);
        estudiante = usuario(estudiante, Usuario.Rol.ESTUDIANTE);
        estudianteOtroCurso = new Estudiante();
        estudianteOtroCurso.setCurso(otroCurso);
        estudianteOtroCurso.setEdad(15);
        estudianteOtroCurso = usuario(estudianteOtroCurso, Usuario.Rol.ESTUDIANTE);

        ProfesorMateria pm = new ProfesorMateria();
        pm.setProfesor(profesor);
        pm.setMateria(materia);
        pm.setCurso(curso);
        pm = asignaciones.save(pm);

        actividad = new Actividad();
        actividad.setTitulo("Quiz " + s);
        actividad.setTipo(Actividad.TipoActividad.QUIZ);
        actividad.setProfesorMateria(pm);
        actividad.setFechaCreacion(LocalDateTime.now().minusHours(1));
        actividad.setFechaEntrega(LocalDate.now().plusDays(3));
        actividad = actividades.save(actividad);
    }

    // ------------------------------------------------------------------ preguntas

    @Test
    void lasPreguntasCreadasSeListanConSusOpciones() throws Exception {
        crearPregunta("""
                {"actividadId":%d,"enunciado":"2+2","tipo":"OPCION_MULTIPLE",
                 "opciones":[{"texto":"4","esCorrecta":true},{"texto":"5","esCorrecta":false},{"texto":"3","esCorrecta":false}]}
                """).andExpect(status().isOk()).andExpect(jsonPath("$.opciones", hasSize(3)));
        crearPregunta("""
                {"actividadId":%d,"enunciado":"El sol es una estrella","tipo":"VERDADERO_FALSO",
                 "opciones":[{"texto":"Verdadero","esCorrecta":true},{"texto":"Falso","esCorrecta":false}]}
                """).andExpect(status().isOk()).andExpect(jsonPath("$.opciones", hasSize(2)));
        crearPregunta("""
                {"actividadId":%d,"enunciado":"Explique la fotosíntesis","tipo":"RESPUESTA_ABIERTA"}
                """).andExpect(status().isOk());

        mvc.perform(get("/api/preguntas/actividad/{id}", actividad.getId()).header("Authorization", bearer(profesor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].opciones[0].texto").value("4"))
                .andExpect(jsonPath("$[1].tipo").value("VERDADERO_FALSO"))
                // Antes las opciones de verdadero/falso no se guardaban
                .andExpect(jsonPath("$[1].opciones", hasSize(2)));
    }

    @Test
    void reglasDeOpcionesPorTipo() throws Exception {
        crearPregunta("""
                {"actividadId":%d,"enunciado":"VF sin opciones","tipo":"VERDADERO_FALSO","opciones":[]}
                """).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("PREGUNTA_OPCIONES_INVALIDAS"));
        crearPregunta("""
                {"actividadId":%d,"enunciado":"Dos correctas","tipo":"OPCION_MULTIPLE",
                 "opciones":[{"texto":"a","esCorrecta":true},{"texto":"b","esCorrecta":true}]}
                """).andExpect(status().isBadRequest()).andExpect(jsonPath("$.message", containsString("exactamente una")));
    }

    @Test
    void adjuntosDePreguntaConImagen() throws Exception {
        long preguntaId = idDe(crearPregunta("""
                {"actividadId":%d,"enunciado":"Observe la figura","tipo":"RESPUESTA_ABIERTA"}
                """));
        mvc.perform(multipart("/api/preguntas/{id}/archivo", preguntaId)
                        .file(new MockMultipartFile("file", "figura.png", "image/png", PNG))
                        .header("Authorization", bearer(profesor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archivoEsImagen").value(true))
                .andExpect(jsonPath("$.nombreArchivo").value("figura.png"))
                .andExpect(jsonPath("$.archivoUrl").value("/api/preguntas/" + preguntaId + "/archivo"));

        // El estudiante del curso la ve en la actividad y puede descargarla en línea
        mvc.perform(get("/api/actividades/{id}/preguntas", actividad.getId()).header("Authorization", bearer(estudiante)))
                .andExpect(jsonPath("$.preguntas[0].archivoEsImagen").value(true))
                .andExpect(jsonPath("$.preguntas[0].archivoUrl", not(containsString(".rep"))));
        mvc.perform(get("/api/preguntas/{id}/archivo", preguntaId).header("Authorization", bearer(estudiante)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(header().string("Content-Disposition", startsWith("inline")))
                .andExpect(content().bytes(PNG));

        // Otro curso u otro profesor: prohibido
        mvc.perform(get("/api/preguntas/{id}/archivo", preguntaId).header("Authorization", bearer(estudianteOtroCurso)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/preguntas/{id}/archivo", preguntaId).header("Authorization", bearer(otroProfesor)))
                .andExpect(status().isForbidden());
    }

    @Test
    void tiposDeArchivoNoPermitidosSonRechazados() throws Exception {
        long preguntaId = idDe(crearPregunta("""
                {"actividadId":%d,"enunciado":"x","tipo":"RESPUESTA_ABIERTA"}
                """));
        mvc.perform(multipart("/api/preguntas/{id}/archivo", preguntaId)
                        .file(new MockMultipartFile("file", "virus.exe", "application/octet-stream", new byte[] { 1, 2 }))
                        .header("Authorization", bearer(profesor)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ARCHIVO_TIPO_NO_PERMITIDO"));
    }

    // ------------------------------------------------------------------ material de estudio

    @Test
    void materialDeEstudioCicloCompleto() throws Exception {
        String body = mvc.perform(multipart("/api/materiales")
                        .file(new MockMultipartFile("file", "guía.pdf", "application/pdf", "%PDF-1.4 demo".getBytes()))
                        .param("titulo", "Guía de repaso").param("descripcion", "Para el examen")
                        .param("materiaId", materia.getId().toString()).param("cursoId", curso.getId().toString())
                        .header("Authorization", bearer(profesor)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titulo").value("Guía de repaso"))
                .andExpect(jsonPath("$.nombreArchivo").value("guía.pdf"))
                .andExpect(jsonPath("$.tipoContenido").value("application/pdf"))
                .andReturn().getResponse().getContentAsString();
        long id = mapper.readTree(body).path("id").asLong();

        mvc.perform(get("/api/materiales").header("Authorization", bearer(estudiante)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem((int) id)));
        mvc.perform(get("/api/materiales/{id}/archivo", id).header("Authorization", bearer(estudiante)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"));

        // Estudiante de otro curso no lo ve ni lo descarga
        mvc.perform(get("/api/materiales").header("Authorization", bearer(estudianteOtroCurso)))
                .andExpect(jsonPath("$[*].id", not(hasItem((int) id))));
        mvc.perform(get("/api/materiales/{id}/archivo", id).header("Authorization", bearer(estudianteOtroCurso)))
                .andExpect(status().isForbidden());

        // Estudiantes no publican; otro profesor no elimina
        mvc.perform(multipart("/api/materiales")
                        .file(new MockMultipartFile("file", "x.txt", "text/plain", "x".getBytes()))
                        .param("materiaId", materia.getId().toString()).param("cursoId", curso.getId().toString())
                        .header("Authorization", bearer(estudiante)))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/materiales/{id}", id).header("Authorization", bearer(otroProfesor)))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/materiales/{id}", id).header("Authorization", bearer(profesor)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/materiales/{id}/archivo", id).header("Authorization", bearer(profesor)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MATERIAL_NO_ENCONTRADO"));
    }

    @Test
    void profesorSinAsignacionNoPublicaMaterial() throws Exception {
        mvc.perform(multipart("/api/materiales")
                        .file(new MockMultipartFile("file", "x.txt", "text/plain", "x".getBytes()))
                        .param("materiaId", materia.getId().toString()).param("cursoId", curso.getId().toString())
                        .header("Authorization", bearer(otroProfesor)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESO_DENEGADO"));
    }

    // ------------------------------------------------------------------ helpers

    private org.springframework.test.web.servlet.ResultActions crearPregunta(String json) throws Exception {
        return mvc.perform(post("/api/preguntas").header("Authorization", bearer(profesor))
                .contentType(MediaType.APPLICATION_JSON).content(json.formatted(actividad.getId())));
    }

    private long idDe(org.springframework.test.web.servlet.ResultActions r) throws Exception {
        JsonNode json = mapper.readTree(r.andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        return json.path("id").asLong();
    }

    private static Curso curso(String grupo) {
        Curso c = new Curso();
        c.setGrado(1 + new Random().nextInt(12));
        c.setGrupo(grupo);
        return c;
    }

    private <T extends Usuario> T usuario(T u, Usuario.Rol rol) {
        String id = String.valueOf(1_000_000_000L + new Random().nextInt(999_999_999));
        u.setNombre("Nombre");
        u.setApellido("Apellido");
        u.setCorreo(id + "@test.local");
        u.setIdentificacion(id);
        u.setTipoIdentificacion(Usuario.TipoIdentificacion.CC);
        u.setContraseña(encoder.encode("Password123"));
        u.setRol(rol);
        u.setActivo(true);
        return usuarios.save(u);
    }

    private String bearer(Usuario u) {
        return "Bearer " + Jwts.builder().setSubject(u.getIdentificacion())
                .claim("authorities", List.of(u.getRol().name()))
                .setExpiration(new Date(System.currentTimeMillis() + 600_000))
                .signWith(secretKey).compact();
    }
}
