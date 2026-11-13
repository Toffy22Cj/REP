package com.rep.funcional;

import com.rep.repositories.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/** La semilla de demostración crea un escenario completo y consistente. */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.seed.demo=true",
        "spring.datasource.url=jdbc:h2:mem:seedtest;DB_CLOSE_DELAY=-1;MODE=MySQL;NON_KEYWORDS=USER",
        "app.storage.dir=${java.io.tmpdir}/rep-seed-storage"
})
class DemoDataSeederTest {
    @Autowired private UsuarioRepository usuarios;
    @Autowired private ProfesorRepository profesores;
    @Autowired private EstudianteRepository estudiantes;
    @Autowired private ActividadRepository actividades;
    @Autowired private RespuestaEstudianteRepository respuestas;
    @Autowired private RecursoRepository recursos;
    @Autowired private AsistenciaRepository asistencias;

    @Test
    void creaElEscenarioDeDemostracion() {
        assertThat(profesores.count()).isEqualTo(3);
        assertThat(estudiantes.count()).isEqualTo(10);
        assertThat(actividades.count()).isGreaterThanOrEqualTo(10);
        var todas = respuestas.findAll();
        assertThat(todas).anyMatch(r -> !r.getEntregado());                                   // en curso
        assertThat(todas).anyMatch(r -> r.getEntregado() && r.getCalificadoPor() == null);    // nota automática
        assertThat(todas).anyMatch(r -> r.getCalificadoPor() != null);                        // revisada
        assertThat(todas).filteredOn(r -> r.getNota() != null)
                .allMatch(r -> r.getNota() >= 0 && r.getNota() <= 5);
        assertThat(actividades.findAll()).anyMatch(a -> !a.getActiva());
        assertThat(recursos.count()).isEqualTo(5);
        assertThat(asistencias.count()).isPositive();
        assertThat(usuarios.existsByIdentificacion("1098765432")).isTrue();
    }
}
