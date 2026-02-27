package com.rep.repositories.ia;

import com.rep.model.Materia;
import com.rep.model.Usuario;
import com.rep.model.ia.AsistenciaIA;
import com.rep.repositories.MateriaRepository;
import com.rep.repositories.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class AsistenciaIARepositoryIntegrationTest {

    @Autowired
    private AsistenciaIARepository asistenciaIARepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private MateriaRepository materiaRepository;

    @Test
    public void testGuardarYBuscarAsistencia() {
        // 1. Crear y guardar datos maestros (Usuario y Materia)
        Usuario estudiante = new Usuario();
        estudiante.setNombre("Test");
        estudiante.setApellido("Student");
        estudiante.setCorreo("test.student@example.com");
        estudiante.setIdentificacion("1234567890");
        estudiante.setTipoIdentificacion(Usuario.TipoIdentificacion.CC);
        estudiante.setContraseña("password");
        estudiante.setRol(Usuario.Rol.ESTUDIANTE);
        estudiante = usuarioRepository.save(estudiante);

        Materia materia = new Materia();
        materia.setNombre("Matemáticas Avanzadas IA");
        materia = materiaRepository.save(materia);

        // 2. Crear y guardar AsistenciaIA
        AsistenciaIA asistencia = new AsistenciaIA();
        asistencia.setEstudiante(estudiante);
        asistencia.setMateria(materia);
        asistencia.setFecha(LocalDate.now());
        asistencia.setPresente(true);
        asistencia.setObservaciones("Asistencia de prueba");

        AsistenciaIA guardada = asistenciaIARepository.save(asistencia);

        // 3. Verificaciones
        assertThat(guardada.getId()).isNotNull();

        // Buscar por ID
        Optional<AsistenciaIA> encontrada = asistenciaIARepository.findById(guardada.getId());
        assertThat(encontrada).isPresent();
        assertThat(encontrada.get().getObservaciones()).isEqualTo("Asistencia de prueba");

        // Buscar por Estudiante
        List<AsistenciaIA> porEstudiante = asistenciaIARepository.findByEstudianteId(estudiante.getId());
        assertThat(porEstudiante).isNotEmpty();
        assertThat(porEstudiante.get(0).getEstudiante().getId()).isEqualTo(estudiante.getId());

        // Buscar por Materia
        List<AsistenciaIA> porMateria = asistenciaIARepository.findByMateriaId(materia.getId());
        assertThat(porMateria).isNotEmpty();
    }
}
