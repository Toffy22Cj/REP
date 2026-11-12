package com.rep.controller;

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
import org.springframework.test.web.servlet.ResultActions;

import javax.crypto.SecretKey;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Contrato de errores: toda respuesta de error es un ApiError JSON con un código del
 * catálogo (docs/errores.md). Incluye los casos detectados al usar el panel web.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.security.rate-limit.login-per-15min=1000",
        "app.security.rate-limit.api-per-minute=10000"
})
class ApiErrorContractTest {

    @Autowired private MockMvc mvc;
    @Autowired private SecretKey secretKey;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private CursoRepository cursoRepository;
    @Autowired private MateriaRepository materiaRepository;

    private Usuario admin;
    private Curso curso;
    private String sufijo;

    @BeforeEach
    void setUp() {
        admin = usuarioRepository.findByIdentificacion("22").orElseThrow();
        sufijo = UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        curso = new Curso();
        curso.setGrado(1 + (int) (Math.random() * 12));
        curso.setGrupo(sufijo);
        curso = cursoRepository.save(curso);
    }

    // ------------------------------------------------------------------ casos del panel web

    @Test
    void tarjetaDeIdentidadConLongitudIncorrectaExplicaElMotivo() throws Exception {
        // Caso real: TI "363636" (6 dígitos) para una estudiante de 17 años
        crearUsuario(usuarioJson("363636", "TI", 17, "ESTUDIANTE", curso.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("DOCUMENTO_LONGITUD_INVALIDA"))
                .andExpect(jsonPath("$.message", containsString("exactamente 10 dígitos")))
                .andExpect(jsonPath("$.message", containsString("tiene 6")))
                .andExpect(jsonPath("$.details.longitud").value(6))
                .andExpect(jsonPath("$.path").value("/api/admin/usuarios"));
    }

    @Test
    void cedulaParaMenorDeEdadTieneCodigoPropio() throws Exception {
        crearUsuario(usuarioJson(ident(8), "CC", 15, "PROFESOR", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DOCUMENTO_CC_MENOR_DE_EDAD"));
    }

    @Test
    void camposInvalidosDevuelvenElDetallePorCampo() throws Exception {
        String body = """
                {"nombre":"A","apellido":"","correo":"no-es-correo","identificacion":"12ab",
                 "tipoIdentificacion":"XX","contraseña":"123","rol":"ROOT"}
                """;
        crearUsuario(body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CAMPOS_INVALIDOS"))
                .andExpect(jsonPath("$.details.correo").exists())
                .andExpect(jsonPath("$.details.identificacion").exists())
                .andExpect(jsonPath("$.details.contraseña").exists())
                .andExpect(jsonPath("$.details.rol").exists());
    }

    @Test
    void identificacionDuplicadaEsConflicto() throws Exception {
        String ident = ident(10);
        crearUsuario(usuarioJson(ident, "CC", 30, "PROFESOR", null)).andExpect(status().isOk());
        crearUsuario(usuarioJson(ident, "CC", 30, "PROFESOR", null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USUARIO_IDENTIFICACION_DUPLICADA"))
                .andExpect(jsonPath("$.details.identificacion").value(ident));
    }

    @Test
    void jsonMalformadoTieneCodigo() throws Exception {
        crearUsuario("{ esto no es json").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("JSON_MALFORMADO"));
    }

    // ------------------------------------------------------------------ seguridad

    @Test
    void peticionesSinSesionDevuelvenJsonConCodigo() throws Exception {
        mvc.perform(get("/api/admin/stats"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("NO_AUTENTICADO"));
        mvc.perform(get("/api/profesor/materias").header("Authorization", "Bearer basura"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_INVALIDO"));
    }

    @Test
    void loginIncorrectoDevuelveCredencialesInvalidas() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identificacion\":\"22\",\"password\":\"x\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("CREDENCIALES_INVALIDAS"));
    }

    @Test
    void rolSinPermisoDevuelveAccesoDenegado() throws Exception {
        Profesor profesor = profesor();
        mvc.perform(get("/api/admin/stats").header("Authorization", bearer(profesor)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESO_DENEGADO"));
    }

    @Test
    void profesorNoPuedeCrearActividadesEnCursosNoAsignados() throws Exception {
        // Antes se creaba la asignación automáticamente (escalada de privilegios)
        Profesor profesor = profesor();
        Materia materia = new Materia();
        materia.setNombre("Química " + sufijo);
        materia = materiaRepository.save(materia);
        String body = """
                {"titulo":"Quiz","tipo":"QUIZ","fechaEntrega":"%s","duracionMinutos":30,"materiaId":%d,"cursoId":%d}
                """.formatted(LocalDate.now().plusDays(3), materia.getId(), curso.getId());
        mvc.perform(post("/api/actividades").header("Authorization", bearer(profesor))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESO_DENEGADO"));
    }

    // ------------------------------------------------------------------ reglas de administración

    @Test
    void cursoInexistenteYCursoConEstudiantes() throws Exception {
        mvc.perform(get("/api/admin/cursos/999999").header("Authorization", bearer(admin)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CURSO_NO_ENCONTRADO"));

        crearUsuario(usuarioJson(ident(10), "TI", 14, "ESTUDIANTE", curso.getId())).andExpect(status().isOk());
        mvc.perform(delete("/api/admin/cursos/{id}", curso.getId()).header("Authorization", bearer(admin)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CURSO_CON_ESTUDIANTES"));
    }

    @Test
    void cursoDuplicadoEsConflicto() throws Exception {
        mvc.perform(post("/api/admin/cursos").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"grado\":%d,\"grupo\":\"%s\"}".formatted(curso.getGrado(), curso.getGrupo().toLowerCase())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CURSO_DUPLICADO"));
    }

    @Test
    void administradorNoPuedeDesactivarseASiMismo() throws Exception {
        mvc.perform(put("/api/admin/usuarios/{id}/estado", admin.getId()).param("activo", "false")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USUARIO_AUTOGESTION_NO_PERMITIDA"));
    }

    @Test
    void actualizacionParcialConservaLosCamposNoEnviados() throws Exception {
        // El cliente de escritorio no envía "apellido": antes quedaba en null y fallaba con 500
        Profesor profesor = profesor();
        mvc.perform(put("/api/admin/usuarios/{id}", profesor.getId()).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Nuevo\",\"correo\":\"nuevo-%s@test.local\",\"rol\":\"PROFESOR\",\"activo\":true}"
                                .formatted(sufijo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Nuevo"))
                .andExpect(jsonPath("$.apellido").value("Apellido"));
    }

    @Test
    void noSePuedeCambiarElRolDeUnProfesor() throws Exception {
        Profesor profesor = profesor();
        mvc.perform(put("/api/admin/usuarios/{id}", profesor.getId()).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rol\":\"ESTUDIANTE\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USUARIO_CAMBIO_ROL_NO_PERMITIDO"));
    }

    @Test
    void materiasSePuedenRenombrar() throws Exception {
        Materia materia = new Materia();
        materia.setNombre("Historia " + sufijo);
        materia = materiaRepository.save(materia);
        mvc.perform(put("/api/admin/materias/{id}", materia.getId()).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Historia universal " + sufijo + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Historia universal " + sufijo));
    }

    // ------------------------------------------------------------------ helpers

    private ResultActions crearUsuario(String body) throws Exception {
        return mvc.perform(post("/api/admin/usuarios").header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String usuarioJson(String ident, String tipo, int edad, String rol, Long cursoId) {
        return """
                {"nombre":"Natasha","apellido":"Pérez","correo":"%s@test.local","identificacion":"%s",
                 "tipoIdentificacion":"%s","contraseña":"Password123","rol":"%s","activo":true,
                 "edad":%d,"sexo":"FEMENINO","cursoId":%s}
                """.formatted(ident, ident, tipo, rol, edad, cursoId);
    }

    private static final java.util.Random RANDOM = new java.util.Random();

    /** Identificación aleatoria de N dígitos (sin cero inicial). */
    private static String ident(int digitos) {
        StringBuilder sb = new StringBuilder().append(1 + RANDOM.nextInt(9));
        while (sb.length() < digitos) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    private Profesor profesor() {
        Profesor p = new Profesor();
        p.setNombre("Profe");
        p.setApellido("Apellido");
        String ident = ident(9);
        p.setCorreo(ident + "@test.local");
        p.setIdentificacion(ident);
        p.setTipoIdentificacion(Usuario.TipoIdentificacion.CC);
        p.setContraseña(passwordEncoder.encode("Password123"));
        p.setRol(Usuario.Rol.PROFESOR);
        p.setActivo(true);
        return usuarioRepository.save(p);
    }

    private String bearer(Usuario u) {
        return "Bearer " + Jwts.builder()
                .setSubject(u.getIdentificacion())
                .claim("authorities", List.of(u.getRol().name()))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 600_000))
                .signWith(secretKey).compact();
    }
}
