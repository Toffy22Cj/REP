package com.rep.funcional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rep.model.Usuario;
import com.rep.repositories.CursoRepository;
import com.rep.repositories.MateriaRepository;
import com.rep.repositories.UsuarioRepository;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Perfil, notas (boletín y planilla) y entregas, sobre los datos de demostración
 * (DemoDataSeeder), que tienen todos los estados de actividad.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.seed.demo=true",
        "spring.datasource.url=jdbc:h2:mem:perfilnotas;DB_CLOSE_DELAY=-1;MODE=MySQL;NON_KEYWORDS=USER",
        "app.storage.dir=${java.io.tmpdir}/rep-perfil-storage",
        "app.security.rate-limit.api-per-minute=10000",
        "app.security.rate-limit.login-per-15min=1000"
})
class PerfilYNotasTest {
    private static final String VALENTINA = "1098765432";
    private static final String MATEO = "1098765433";
    private static final String CARLOS = "1020304050";
    private static final String ANDRES = "1040506070";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private SecretKey secretKey;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private CursoRepository cursos;
    @Autowired private MateriaRepository materias;

    // ------------------------------------------------------------------ perfil

    @Test
    void perfilDelProfesorIncluyeSusAsignaciones() throws Exception {
        mvc.perform(get("/api/perfil").header("Authorization", bearer(CARLOS)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identificacion").value(CARLOS))
                .andExpect(jsonPath("$.rol").value("PROFESOR"))
                .andExpect(jsonPath("$.asignaciones", hasItem("Matemáticas · 10-A")))
                .andExpect(jsonPath("$.contraseña").doesNotExist());
    }

    @Test
    void elEstudianteActualizaSusDatosPeroNoLosInstitucionales() throws Exception {
        String correo = "nuevo-" + UUID.randomUUID().toString().substring(0, 6) + "@demo.rep.edu";
        mvc.perform(put("/api/perfil").header("Authorization", bearer("1098765441"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Nico\",\"apellido\":\"Díaz Ruiz\",\"correo\":\"" + correo.toUpperCase()
                                + "\",\"sexo\":\"MASCULINO\",\"identificacion\":\"999\",\"rol\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Nico"))
                .andExpect(jsonPath("$.correo").value(correo))
                .andExpect(jsonPath("$.curso").value("11-A"))
                // campos institucionales ignorados
                .andExpect(jsonPath("$.identificacion").value("1098765441"))
                .andExpect(jsonPath("$.rol").value("ESTUDIANTE"));
    }

    @Test
    void correoDeOtroUsuarioEsConflicto() throws Exception {
        String correoAjeno = usuarios.findByIdentificacion(CARLOS).orElseThrow().getCorreo();
        mvc.perform(put("/api/perfil").header("Authorization", bearer(MATEO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Mateo\",\"apellido\":\"Rojas\",\"correo\":\"" + correoAjeno + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USUARIO_CORREO_DUPLICADO"));
    }

    @Test
    void cambioDeContrasenaInvalidaLosTokensAnteriores() throws Exception {
        String usuario = "1098765440";
        String tokenViejo = bearer(usuario, 5); // emitido hace 5 s
        mvc.perform(put("/api/perfil/contrasena").header("Authorization", tokenViejo)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"actual\":\"mala\",\"nueva\":\"Nueva2026x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CONTRASENA_ACTUAL_INCORRECTA"));
        mvc.perform(put("/api/perfil/contrasena").header("Authorization", tokenViejo)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"actual\":\"Demo12345\",\"nueva\":\"soloLetras\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CONTRASENA_DEBIL"));

        String respuesta = mvc.perform(put("/api/perfil/contrasena").header("Authorization", tokenViejo)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"actual\":\"Demo12345\",\"nueva\":\"Nueva2026x\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String tokenNuevo = mapper.readTree(respuesta).path("token").asText();

        mvc.perform(get("/api/perfil").header("Authorization", tokenViejo))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_INVALIDO"));
        mvc.perform(get("/api/perfil").header("Authorization", "Bearer " + tokenNuevo))
                .andExpect(status().isOk());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identificacion\":\"" + usuario + "\",\"password\":\"Nueva2026x\"}"))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ boletín del estudiante

    @Test
    void boletinDelEstudianteConPromediosYEstados() throws Exception {
        JsonNode b = json(get("/api/estudiante/{id}/notas", idDe(MATEO)).header("Authorization", bearer(MATEO)));
        assertThat(b.path("curso").asText()).isEqualTo("10-A");
        JsonNode mate = materia(b, "Matemáticas");
        // Quiz derivadas 3.3 y examen funciones 3.3 (2 de 3 aciertos); taller y quiz pendientes no cuentan
        assertThat(mate.path("promedio").floatValue()).isEqualTo(3.3f);
        assertThat(mate.path("desempeno").asText()).isEqualTo("BASICO");
        assertThat(mate.path("aprobada").asBoolean()).isTrue();
        assertThat(estados(mate)).contains("CALIFICADA", "PENDIENTE");
        // La actividad inactiva (borrador) no aparece
        assertThat(mate.path("actividades").toString()).doesNotContain("Borrador");
        // Física: entregó un taller con pregunta abierta sin revisar -> ENTREGADA con nota preliminar
        assertThat(estados(materia(b, "Física"))).contains("ENTREGADA");
        // Ciencias: taller vencido sin entregar -> VENCIDA y cuenta como 0
        JsonNode ciencias = materia(b, "Ciencias Naturales");
        assertThat(estados(ciencias)).contains("VENCIDA");
        assertThat(ciencias.path("promedio").floatValue()).isEqualTo(0f);
        assertThat(ciencias.path("aprobada").asBoolean()).isFalse();
        assertThat(b.path("materiasReprobadas").asInt()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void unEstudianteNoVeElBoletinDeOtro() throws Exception {
        mvc.perform(get("/api/estudiante/{id}/notas", idDe(VALENTINA)).header("Authorization", bearer(MATEO)))
                .andExpect(status().isForbidden());
    }

    @Test
    void boletinEnPdf() throws Exception {
        byte[] pdf = mvc.perform(get("/api/estudiante/{id}/notas/pdf", idDe(VALENTINA)).header("Authorization", bearer(VALENTINA)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
    }

    // ------------------------------------------------------------------ planilla del profesor

    @Test
    void planillaDeNotasConVencidasYPromedios() throws Exception {
        long curso10 = cursos.findByGradoAndGrupo(10, "A").orElseThrow().getId();
        long ciencias = materias.findByNombre("Ciencias Naturales").orElseThrow().getId();
        JsonNode p = json(get("/api/profesor/notas").param("cursoId", "" + curso10).param("materiaId", "" + ciencias)
                .header("Authorization", bearer(ANDRES)));
        assertThat(p.path("estudiantes").size()).isEqualTo(6);
        assertThat(p.path("actividades").size()).isEqualTo(1);
        // Valentina entregó (2.5 + 0.5 de revisión = 3.0); el resto no entregó (vencida = 0.0)
        JsonNode valentina = fila(p, VALENTINA);
        assertThat(valentina.path("promedio").floatValue()).isEqualTo(3.0f);
        assertThat(valentina.path("aprobada").asBoolean()).isTrue();
        assertThat(p.path("aprobados").asInt()).isEqualTo(1);
        assertThat(p.path("reprobados").asInt()).isEqualTo(5);
        assertThat(fila(p, MATEO).path("notas").get(0).path("estado").asText()).isEqualTo("VENCIDA");
    }

    @Test
    void planillaSoloDeCursosAsignadosYExportaciones() throws Exception {
        long curso10 = cursos.findByGradoAndGrupo(10, "A").orElseThrow().getId();
        long mate = materias.findByNombre("Matemáticas").orElseThrow().getId();
        long ciencias = materias.findByNombre("Ciencias Naturales").orElseThrow().getId();
        mvc.perform(get("/api/profesor/notas").param("cursoId", "" + curso10).param("materiaId", "" + ciencias)
                        .header("Authorization", bearer(CARLOS)))
                .andExpect(status().isForbidden());

        byte[] xlsx = mvc.perform(get("/api/profesor/notas/export").param("cursoId", "" + curso10)
                        .param("materiaId", "" + mate).param("formato", "xlsx").header("Authorization", bearer(CARLOS)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString(".xlsx")))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(xlsx, 0, 2)).isEqualTo("PK");
        mvc.perform(get("/api/profesor/notas/export").param("cursoId", "" + curso10)
                        .param("materiaId", "" + mate).param("formato", "pdf").header("Authorization", bearer(CARLOS)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"));
    }

    @Test
    void entregasDeUnaActividadParaCalificar() throws Exception {
        long curso10 = cursos.findByGradoAndGrupo(10, "A").orElseThrow().getId();
        long mate = materias.findByNombre("Matemáticas").orElseThrow().getId();
        JsonNode p = json(get("/api/profesor/notas").param("cursoId", "" + curso10).param("materiaId", "" + mate)
                .header("Authorization", bearer(CARLOS)));
        long examen = 0;
        for (JsonNode a : p.path("actividades")) {
            if (a.path("titulo").asText().startsWith("Examen")) examen = a.path("actividadId").asLong();
        }
        mvc.perform(get("/api/profesor/actividades/{id}/respuestas", examen).header("Authorization", bearer(CARLOS)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(6)))
                .andExpect(jsonPath("$[0].nombreEstudiante").exists())
                .andExpect(jsonPath("$[*].calificada", hasItem(true)));
    }

    // ------------------------------------------------------------------ registro y archivos

    @Test
    void registroPublicoQuedaPendienteDeActivacion() throws Exception {
        long curso10 = cursos.findByGradoAndGrupo(10, "A").orElseThrow().getId();
        String ident = "10" + (System.nanoTime() % 100_000_000L + 100_000_000L);
        ident = ident.substring(0, 10);
        mvc.perform(post("/api/registro").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nombre":"Nuevo","apellido":"Alumno","correo":"%s@x.co","identificacion":"%s",
                         "tipoIdentificacion":"TI","contraseña":"Clave2026x","rol":"ESTUDIANTE","activo":true,
                         "edad":14,"cursoId":%d}""".formatted(ident, ident, curso10)))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identificacion\":\"" + ident + "\",\"password\":\"Clave2026x\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CUENTA_INACTIVA"));
    }

    @Test
    void archivosSeRevalidanConEtag() throws Exception {
        JsonNode materiales = json(get("/api/materiales").header("Authorization", bearer(VALENTINA)));
        String url = materiales.get(0).path("archivoUrl").asText();
        String etag = mvc.perform(get(url).header("Authorization", bearer(VALENTINA)))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andReturn().getResponse().getHeader("ETag");
        mvc.perform(get(url).header("Authorization", bearer(VALENTINA)).header("If-None-Match", etag))
                .andExpect(status().isNotModified());
    }

    // ------------------------------------------------------------------ helpers

    private JsonNode json(org.springframework.test.web.servlet.RequestBuilder req) throws Exception {
        return mapper.readTree(mvc.perform(req).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static JsonNode materia(JsonNode boletin, String nombre) {
        for (JsonNode m : boletin.path("materias")) if (m.path("materia").asText().equals(nombre)) return m;
        throw new AssertionError("Materia no encontrada: " + nombre);
    }

    private static List<String> estados(JsonNode materia) {
        java.util.List<String> e = new java.util.ArrayList<>();
        materia.path("actividades").forEach(a -> e.add(a.path("estado").asText()));
        return e;
    }

    private JsonNode fila(JsonNode planilla, String identificacion) {
        for (JsonNode f : planilla.path("estudiantes")) if (f.path("identificacion").asText().equals(identificacion)) return f;
        throw new AssertionError("Estudiante no encontrado: " + identificacion);
    }

    private long idDe(String identificacion) {
        return usuarios.findByIdentificacion(identificacion).orElseThrow().getId();
    }

    private String bearer(String identificacion) {
        return bearer(identificacion, 0);
    }

    private String bearer(String identificacion, int segundosAtras) {
        Usuario u = usuarios.findByIdentificacion(identificacion).orElseThrow();
        long ahora = System.currentTimeMillis() - segundosAtras * 1000L;
        return "Bearer " + Jwts.builder().setSubject(identificacion)
                .claim("authorities", List.of(u.getRol().name()))
                .setIssuedAt(new Date(ahora))
                .setExpiration(new Date(ahora + 600_000))
                .signWith(secretKey).compact();
    }
}
