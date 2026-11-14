package com.rep.funcional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rep.model.PeriodoAcademico;
import com.rep.model.Usuario;
import com.rep.repositories.PeriodoAcademicoRepository;
import com.rep.repositories.UsuarioRepository;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Periodos académicos: validaciones de la configuración y nota anual ponderada, sobre
 * los datos de demostración (4 periodos de 25 %, actividades en el cuarto).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.seed.demo=true",
        "spring.datasource.url=jdbc:h2:mem:periodos;DB_CLOSE_DELAY=-1;MODE=MySQL;NON_KEYWORDS=USER",
        "app.storage.dir=${java.io.tmpdir}/rep-periodos-storage",
        "app.security.rate-limit.api-per-minute=10000",
        "app.security.rate-limit.login-per-15min=1000"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PeriodosAcademicosTest {
    private static final String ADMIN = "22";
    private static final String MATEO = "1098765433";
    private static final String CARLOS = "1020304050";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private SecretKey secretKey;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private PeriodoAcademicoRepository periodos;

    private final int anio = LocalDate.now().getYear();

    @Test
    @Order(1)
    void todosLosRolesConsultanLosPeriodosVigentes() throws Exception {
        mvc.perform(get("/api/periodos").header("Authorization", bearer(MATEO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].nombre").value("Periodo 1"))
                .andExpect(jsonPath("$[3].porcentaje").value(25));
        // Solo lectura fuera de /api/admin
        mvc.perform(post("/api/periodos").header("Authorization", bearer(CARLOS))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(2)
    void soloElAdministradorGestionaPeriodos() throws Exception {
        mvc.perform(post("/api/admin/periodos").header("Authorization", bearer(CARLOS))
                        .contentType(MediaType.APPLICATION_JSON).content(periodo(anio + 1, 1, "2026-01-01", "2026-03-01", 30)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(3)
    void validacionesDeFechasNumerosYPorcentajes() throws Exception {
        int otro = anio + 1;
        crear(periodo(otro, 1, otro + "-01-20", otro + "-03-31", 30)).andExpect(status().isOk());
        // Fechas al revés
        crear(periodo(otro, 2, otro + "-06-01", otro + "-04-01", 30))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PERIODO_FECHAS_INVALIDAS"));
        // Cruce con el periodo 1
        crear(periodo(otro, 2, otro + "-03-15", otro + "-06-15", 30))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PERIODO_SOLAPADO"));
        // Número repetido
        crear(periodo(otro, 1, otro + "-07-01", otro + "-09-01", 10))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PERIODO_DUPLICADO"));
        // 30 + 80 > 100
        crear(periodo(otro, 2, otro + "-04-01", otro + "-06-15", 80))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PERIODO_PORCENTAJE_EXCEDIDO"));
        // Campos obligatorios
        crear("{\"anio\":" + otro + "}").andExpect(status().isBadRequest());

        mvc.perform(get("/api/periodos").param("anio", String.valueOf(otro)).header("Authorization", bearer(MATEO)))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @Order(4)
    void sinPeriodoElBoletinEsAnualYPonderado() throws Exception {
        JsonNode b = boletin(null);
        assertThat(b.get("ponderado").asBoolean()).isTrue();
        assertThat(b.get("periodos")).hasSize(4);
        JsonNode mate = materia(b, "Matemáticas");
        // Todas las actividades demo están en el periodo 4: los demás sin notas
        assertThat(mate.get("periodos").get(0).get("promedio").isNull()).isTrue();
        assertThat(mate.get("periodos").get(3).get("promedio").floatValue())
                .isEqualTo(mate.get("promedio").floatValue());
    }

    @Test
    @Order(5)
    void conPeriodoSoloCuentanSusActividades() throws Exception {
        long p1 = periodos.findByAnioOrderByNumeroAsc(anio).get(0).getId();
        JsonNode b = boletin(p1);
        assertThat(b.get("ponderado").asBoolean()).isFalse();
        assertThat(b.get("periodo").get("nombre").asText()).isEqualTo("Periodo 1");
        assertThat(materia(b, "Matemáticas").get("actividades")).isEmpty();

        mvc.perform(get("/api/profesor/notas").param("cursoId", "1").param("materiaId", "1")
                        .param("periodoId", String.valueOf(p1)).header("Authorization", bearer(CARLOS)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actividades", hasSize(0)))
                .andExpect(jsonPath("$.periodo.nombre").value("Periodo 1"));

        mvc.perform(get("/api/estudiante/{id}/notas", idDe(MATEO)).param("periodoId", "999999")
                        .header("Authorization", bearer(MATEO)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PERIODO_NO_ENCONTRADO"));
    }

    @Test
    @Order(6)
    void laNotaAnualPonderaLosPromediosDeCadaPeriodo() throws Exception {
        // Reparte las actividades: las ya vencidas pasan al periodo 3, las futuras al 4
        List<PeriodoAcademico> lista = periodos.findByAnioOrderByNumeroAsc(anio);
        PeriodoAcademico p3 = lista.get(2);
        PeriodoAcademico p4 = lista.get(3);
        LocalDate hoy = LocalDate.now();
        // (la suma del año nunca pasa de 100: 25 + 25 + 10 + 40)
        actualizar(p4, hoy, p4.getFechaFin(), 25);
        actualizar(p3, hoy.minusDays(60), hoy.minusDays(1), 10);
        actualizar(p4, hoy, p4.getFechaFin(), 40);

        JsonNode mate = materia(boletin(null), "Matemáticas");
        JsonNode porPeriodo = mate.get("periodos");
        double prom3 = porPeriodo.get(2).get("promedio").asDouble();
        double prom4 = porPeriodo.get(3).get("promedio").asDouble();
        assertThat(porPeriodo.get(2).get("promedio").isNull()).isFalse();
        assertThat(porPeriodo.get(3).get("promedio").isNull()).isFalse();
        double esperado = (prom3 * 10 + prom4 * 40) / 50;
        assertThat(mate.get("promedio").asDouble()).isCloseTo(esperado, within(0.051));

        // El PDF del boletín se genera con el desglose por periodo
        mvc.perform(get("/api/estudiante/{id}/notas/pdf", idDe(MATEO)).header("Authorization", bearer(MATEO)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"));
    }

    @Test
    @Order(7)
    void asistenciaConRetardoYEstadoInvalido() throws Exception {
        long mateo = idDe(MATEO);
        String fecha = LocalDate.now().plusDays(1).toString();
        mvc.perform(post("/api/profesor/asistencia").param("cursoId", "1").param("materiaId", "1").param("fecha", fecha)
                        .header("Authorization", bearer(CARLOS)).contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"estudianteId\":" + mateo + ",\"estado\":\"RETARDO\"}]"))
                .andExpect(status().isOk());
        assertThat(materia(boletin(null), "Matemáticas").get("asistencia").get("retardos").asInt()).isEqualTo(1);

        // Antes un estado desconocido se guardaba como falla sin avisar
        mvc.perform(post("/api/profesor/asistencia").param("cursoId", "1").param("materiaId", "1").param("fecha", fecha)
                        .header("Authorization", bearer(CARLOS)).contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"estudianteId\":" + mateo + ",\"estado\":\"TARDE\"}]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CAMPOS_INVALIDOS"));
        assertThat(materia(boletin(null), "Matemáticas").get("asistencia").get("retardos").asInt()).isEqualTo(1);
    }

    // ------------------------------------------------------------------ utilidades

    private void actualizar(PeriodoAcademico p, LocalDate inicio, LocalDate fin, int porcentaje) throws Exception {
        mvc.perform(put("/api/admin/periodos/" + p.getId()).header("Authorization", bearer(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(periodo(p.getAnio(), p.getNumero(), inicio.toString(), fin.toString(), porcentaje)))
                .andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.ResultActions crear(String json) throws Exception {
        return mvc.perform(post("/api/admin/periodos").header("Authorization", bearer(ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String periodo(int anio, int numero, String inicio, String fin, int porcentaje) {
        return "{\"anio\":" + anio + ",\"numero\":" + numero + ",\"nombre\":\"Periodo " + numero
                + "\",\"fechaInicio\":\"" + inicio + "\",\"fechaFin\":\"" + fin + "\",\"porcentaje\":" + porcentaje + "}";
    }

    private JsonNode boletin(Long periodoId) throws Exception {
        var peticion = get("/api/estudiante/{id}/notas", idDe(MATEO)).header("Authorization", bearer(MATEO));
        if (periodoId != null) {
            peticion = peticion.param("periodoId", String.valueOf(periodoId));
        }
        String json = mvc.perform(peticion).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return mapper.readTree(json);
    }

    private static JsonNode materia(JsonNode boletin, String nombre) {
        for (JsonNode m : boletin.get("materias")) {
            if (m.get("materia").asText().equals(nombre)) {
                return m;
            }
        }
        throw new AssertionError("Materia no encontrada: " + nombre);
    }

    private long idDe(String identificacion) {
        return usuarios.findByIdentificacion(identificacion).orElseThrow().getId();
    }

    private String bearer(String identificacion) {
        Usuario u = usuarios.findByIdentificacion(identificacion).orElseThrow();
        long ahora = System.currentTimeMillis();
        return "Bearer " + Jwts.builder().setSubject(identificacion)
                .claim("authorities", List.of(u.getRol().name()))
                .setIssuedAt(new Date(ahora))
                .setExpiration(new Date(ahora + 600_000))
                .signWith(secretKey).compact();
    }
}
