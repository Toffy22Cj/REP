package com.rep.security;

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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.crypto.SecretKey;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de regresión de los hallazgos de seguridad e integridad corregidos en la
 * refactorización (ver CHANGELOG.md). Cada test reproduce el ataque original.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.security.rate-limit.login-per-15min=1000",
        "app.security.rate-limit.api-per-minute=10000"
})
class SecurityRegressionTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private SecretKey secretKey;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private CursoRepository cursoRepository;
    @Autowired private MateriaRepository materiaRepository;
    @Autowired private ProfesorMateriaRepository profesorMateriaRepository;
    @Autowired private ActividadRepository actividadRepository;
    @Autowired private PreguntaRepository preguntaRepository;
    @Autowired private OpcionRepository opcionRepository;

    private Curso curso;
    private Profesor profesorA;
    private Profesor profesorB;
    private Estudiante estudiante1;
    private Estudiante estudiante2;
    private Usuario admin;
    private Actividad actividad;
    private Pregunta pregunta;
    private Opcion correcta;
    private Opcion incorrecta;

    @BeforeEach
    void setUp() {
        String sufijo = UUID.randomUUID().toString().substring(0, 6);
        curso = new Curso();
        curso.setGrado(1 + (int) (Math.random() * 12));
        curso.setGrupo(sufijo.substring(0, 5));
        curso = cursoRepository.save(curso);

        Materia materia = new Materia();
        materia.setNombre("Materia-" + sufijo);
        materia = materiaRepository.save(materia);

        profesorA = guardar(new Profesor(), Usuario.Rol.PROFESOR, "pa" + sufijo);
        profesorB = guardar(new Profesor(), Usuario.Rol.PROFESOR, "pb" + sufijo);
        admin = guardar(new Usuario(), Usuario.Rol.ADMIN, "ad" + sufijo);

        estudiante1 = new Estudiante();
        estudiante1.setCurso(curso);
        estudiante1.setEdad(15);
        estudiante1 = guardar(estudiante1, Usuario.Rol.ESTUDIANTE, "e1" + sufijo);
        estudiante2 = new Estudiante();
        estudiante2.setCurso(curso);
        estudiante2.setEdad(15);
        estudiante2 = guardar(estudiante2, Usuario.Rol.ESTUDIANTE, "e2" + sufijo);

        ProfesorMateria pm = new ProfesorMateria();
        pm.setProfesor(profesorA);
        pm.setMateria(materia);
        pm.setCurso(curso);
        pm = profesorMateriaRepository.save(pm);

        actividad = new Actividad();
        actividad.setTitulo("Quiz " + sufijo);
        actividad.setTipo(Actividad.TipoActividad.QUIZ);
        actividad.setProfesorMateria(pm);
        actividad.setFechaCreacion(LocalDateTime.now().minusHours(1));
        actividad.setFechaEntrega(LocalDate.now().plusDays(7));
        actividad.setPermitirReintentos(true);
        actividad.setActiva(true);
        actividad = actividadRepository.save(actividad);

        pregunta = new Pregunta();
        pregunta.setActividad(actividad);
        pregunta.setEnunciado("2 + 2 = ?");
        pregunta.setTipo(Pregunta.TipoPregunta.OPCION_MULTIPLE);
        pregunta = preguntaRepository.save(pregunta);

        correcta = opcion("4", true);
        incorrecta = opcion("5", false);
    }

    // ---------------------------------------------------------------- registro

    @Test
    void registroPublicoNoPermiteCrearAdministradores() throws Exception {
        String body = """
                {"nombre":"Mal","apellido":"Actor","correo":"x%s@x.com","identificacion":"%s",
                 "tipoIdentificacion":"CC","contraseña":"Password123","rol":"ADMIN","activo":true}
                """.formatted(UUID.randomUUID(), String.valueOf(System.nanoTime()).substring(0, 10));
        mvc.perform(post("/api/registro").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void registroPublicoDeEstudianteNoExponeLaContrasena() throws Exception {
        String body = """
                {"nombre":"Ana","apellido":"Pérez","correo":"a%s@x.com","identificacion":"%s",
                 "tipoIdentificacion":"TI","contraseña":"Password123","rol":"ESTUDIANTE",
                 "activo":true,"edad":15,"cursoId":%d}
                """.formatted(UUID.randomUUID(), String.valueOf(System.nanoTime()).substring(0, 10), curso.getId());
        MvcResult r = mvc.perform(post("/api/registro").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        String json = r.getResponse().getContentAsString();
        assertThat(json).doesNotContain("contrase").doesNotContain("$2a$");
    }

    // ---------------------------------------------------------------- IDOR estudiante

    @Test
    void estudianteNoPuedeLeerDatosDeOtroEstudiante() throws Exception {
        mvc.perform(get("/api/estudiante/{id}/actividades", estudiante2.getId())
                        .header("Authorization", bearer(estudiante1)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/estudiante/{id}/actividades", estudiante1.getId())
                        .header("Authorization", bearer(estudiante1)))
                .andExpect(status().isOk());
    }

    @Test
    void estudianteNoPuedeEntregarEnNombreDeOtro() throws Exception {
        mvc.perform(post("/api/estudiante/{id}/actividades/{a}/resolver", estudiante2.getId(), actividad.getId())
                        .header("Authorization", bearer(estudiante1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resolucion(correcta)))
                .andExpect(status().isForbidden());
    }

    @Test
    void profesorNoPuedeUsarElPortalDelEstudiante() throws Exception {
        mvc.perform(get("/api/estudiante/{id}/materias", estudiante1.getId())
                        .header("Authorization", bearer(profesorA)))
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------- fuga de soluciones

    @Test
    void estudianteNoRecibeLaRespuestaCorrecta() throws Exception {
        MvcResult r = mvc.perform(get("/api/actividades/{id}/preguntas", actividad.getId())
                        .header("Authorization", bearer(estudiante1)))
                .andExpect(status().isOk()).andReturn();
        JsonNode opciones = mapper.readTree(r.getResponse().getContentAsString())
                .path("preguntas").get(0).path("opciones");
        assertThat(opciones.size()).isEqualTo(2);
        opciones.forEach(o -> assertThat(o.path("esCorrecta").isNull() || o.path("esCorrecta").isMissingNode()).isTrue());
    }

    @Test
    void profesorPropietarioSiveVeLaRespuestaCorrecta() throws Exception {
        MvcResult r = mvc.perform(get("/api/actividades/{id}/preguntas", actividad.getId())
                        .header("Authorization", bearer(profesorA)))
                .andExpect(status().isOk()).andReturn();
        assertThat(r.getResponse().getContentAsString()).contains("\"esCorrecta\":true");
    }

    // ---------------------------------------------------------------- propiedad de preguntas

    @Test
    void profesorNoPuedeModificarPreguntasDeOtroProfesor() throws Exception {
        mvc.perform(delete("/api/preguntas/{id}", pregunta.getId())
                        .header("Authorization", bearer(profesorB)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/preguntas/{id}", pregunta.getId())
                        .header("Authorization", bearer(profesorB)))
                .andExpect(status().isForbidden());
        assertThat(preguntaRepository.existsById(pregunta.getId())).isTrue();
    }

    @Test
    void profesorPropietarioPuedeLeerSuPregunta() throws Exception {
        mvc.perform(get("/api/preguntas/{id}", pregunta.getId())
                        .header("Authorization", bearer(profesorA)))
                .andExpect(status().isOk());
    }

    @Test
    void exportacionDeResultadosValidaLaPropiedad() throws Exception {
        mvc.perform(get("/api/profesor/reportes/resultados/export")
                        .param("cursoId", curso.getId().toString())
                        .param("actividadId", actividad.getId().toString())
                        .param("formato", "pdf")
                        .header("Authorization", bearer(profesorB)))
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------- API de administración

    @Test
    void apiAdminAceptaJwtDeAdministrador() throws Exception {
        mvc.perform(get("/api/admin/stats").header("Authorization", bearer(admin)))
                .andExpect(status().isOk());
    }

    @Test
    void apiAdminRechazaOtrosRolesYPeticionesAnonimas() throws Exception {
        mvc.perform(get("/api/admin/usuarios").header("Authorization", bearer(profesorA)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/usuarios"))
                .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------- tokens

    @Test
    void tokenDeCuentaDesactivadaEsRechazado() throws Exception {
        String token = bearer(estudiante1);
        estudiante1.setActivo(false);
        usuarioRepository.save(estudiante1);
        mvc.perform(get("/api/estudiante/{id}/actividades", estudiante1.getId()).header("Authorization", token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenConRolFalsificadoNoOtorgaPrivilegios() throws Exception {
        // Las autoridades salen de la BD, no de las claims del token
        String token = "Bearer " + Jwts.builder()
                .setSubject(estudiante1.getIdentificacion())
                .claim("authorities", List.of("ADMIN"))
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(secretKey).compact();
        mvc.perform(get("/api/admin/stats").header("Authorization", token))
                .andExpect(status().isForbidden());
    }

    @Test
    void tokenInvalidoDevuelve401() throws Exception {
        mvc.perform(get("/api/profesor/materias").header("Authorization", "Bearer no-es-un-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginDevuelveTokenUtilizable() throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identificacion\":\"" + estudiante1.getIdentificacion() + "\",\"password\":\"Secreta123\"}"))
                .andExpect(status().isOk()).andReturn();
        String token = mapper.readTree(r.getResponse().getContentAsString()).path("token").asText();
        mvc.perform(get("/api/estudiante/{id}/materias", estudiante1.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    // ---------------------------------------------------------------- integridad de notas

    @Test
    void reintentoNoAcumulaRespuestasAnteriores() throws Exception {
        double primera = resolver(correcta);
        assertThat(primera).isEqualTo(5.0);

        // Segundo intento incorrecto: antes la respuesta correcta anterior seguía
        // contando y la nota permanecía en 5.0
        double segunda = resolver(incorrecta);
        assertThat(segunda).isEqualTo(0.0);
    }

    @Test
    void respuestasDuplicadasNoInflanLaNota() throws Exception {
        Pregunta otra = new Pregunta();
        otra.setActividad(actividad);
        otra.setEnunciado("3 + 3 = ?");
        otra.setTipo(Pregunta.TipoPregunta.OPCION_MULTIPLE);
        preguntaRepository.save(otra);
        assertThat(preguntaRepository.findByActividadIdOrderByIdAsc(actividad.getId())).hasSize(2);

        // Misma pregunta respondida dos veces correctamente y la otra sin responder
        String body = """
                {"actividadId":%d,"estudianteId":%d,"respuestas":[
                  {"preguntaId":%d,"opcionId":%d},{"preguntaId":%d,"opcionId":%d}]}
                """.formatted(actividad.getId(), estudiante1.getId(),
                pregunta.getId(), correcta.getId(), pregunta.getId(), correcta.getId());
        MvcResult r = mvc.perform(post("/api/estudiante/{id}/actividades/{a}/resolver", estudiante1.getId(), actividad.getId())
                        .header("Authorization", bearer(estudiante1))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn();
        assertThat(mapper.readTree(r.getResponse().getContentAsString()).path("nota").asDouble()).isEqualTo(2.5);
    }

    // ---------------------------------------------------------------- helpers

    private double resolver(Opcion opcion) throws Exception {
        MvcResult r = mvc.perform(post("/api/estudiante/{id}/actividades/{a}/resolver", estudiante1.getId(), actividad.getId())
                        .header("Authorization", bearer(estudiante1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resolucion(opcion)))
                .andExpect(status().isOk()).andReturn();
        return mapper.readTree(r.getResponse().getContentAsString()).path("nota").asDouble();
    }

    private String resolucion(Opcion opcion) {
        return """
                {"actividadId":%d,"estudianteId":%d,"respuestas":[{"preguntaId":%d,"opcionId":%d}]}
                """.formatted(actividad.getId(), estudiante1.getId(), pregunta.getId(), opcion.getId());
    }

    private Opcion opcion(String texto, boolean esCorrecta) {
        Opcion o = new Opcion();
        o.setPregunta(pregunta);
        o.setTexto(texto);
        o.setEsCorrecta(esCorrecta);
        return opcionRepository.save(o);
    }

    private <T extends Usuario> T guardar(T u, Usuario.Rol rol, String identificacion) {
        u.setNombre("Nombre");
        u.setApellido("Apellido");
        u.setCorreo(identificacion + "@test.local");
        u.setIdentificacion(identificacion);
        u.setTipoIdentificacion(Usuario.TipoIdentificacion.CC);
        u.setContraseña(passwordEncoder.encode("Secreta123"));
        u.setRol(rol);
        u.setActivo(true);
        return usuarioRepository.save(u);
    }

    private String bearer(Usuario u) {
        return "Bearer " + Jwts.builder()
                .setSubject(u.getIdentificacion())
                .claim("userId", u.getId())
                .claim("authorities", List.of(u.getRol().name()))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(secretKey)
                .compact();
    }
}
