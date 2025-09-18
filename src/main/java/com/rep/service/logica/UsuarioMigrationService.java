package com.rep.service.logica;

import com.rep.model.*;
import com.rep.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioMigrationService {
    private final UsuarioRepository usuarioRepository;
    private final EstudianteRepository estudianteRepository;
    private final ProfesorRepository profesorRepository;
    private final CursoRepository cursoRepository;

    public UsuarioMigrationService(UsuarioRepository usuarioRepository,
                                   EstudianteRepository estudianteRepository,
                                   ProfesorRepository profesorRepository,
                                   CursoRepository cursoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.estudianteRepository = estudianteRepository;
        this.profesorRepository = profesorRepository;
        this.cursoRepository = cursoRepository;
    }

    @Transactional
    public void migrateExistingUsers() {
        Curso cursoTemporal = obtenerCursoTemporal();

        usuarioRepository.findByRol(Usuario.Rol.ESTUDIANTE).forEach(usuario -> {
            if (!estudianteRepository.existsById(usuario.getId())) {
                // Solo actualiza campos específicos sin tocar el usuario base
                estudianteRepository.insertEstudiante(
                        usuario.getId(),
                        18,
                        Estudiante.EstadoEstudiante.activo.name(),
                        cursoTemporal.getId()
                );
            }
        });
    }

    private Curso obtenerCursoTemporal() {
        return cursoRepository.findByGradoAndGrupo(0, "TEMP")
                .orElseGet(() -> {
                    Curso curso = new Curso();
                    curso.setGrado(0);
                    curso.setGrupo("TEMP");
                    return cursoRepository.save(curso);
                });
    }

    private void copiarPropiedadesComunes(Usuario destino, Usuario origen) {
        // Solo copiar ID, no crear nuevo usuario
        destino.setId(origen.getId());

        // Copiar propiedades específicas de estudiante/profesor
        if (destino instanceof Estudiante) {
            ((Estudiante) destino).setEdad(18);
            ((Estudiante) destino).setEstado(Estudiante.EstadoEstudiante.activo);
        } else if (destino instanceof Profesor) {
            ((Profesor) destino).setEstado(Profesor.EstadoProfesor.activo);
            ((Profesor) destino).setFechaIngreso(origen.getFechaIngreso());
        }
    }

    @Transactional
    public void asignarCursosValidos() {
        // Obtener un curso válido (puedes modificar esta lógica)
        Curso cursoValido = cursoRepository.findByGradoAndGrupo(1, "A")
                .orElseThrow(() -> new RuntimeException("No se encontró curso válido"));

        // Asignar curso a estudiantes que tienen el curso temporal
        estudianteRepository.findByCurso_GradoAndCurso_Grupo(0, "TEMP")
                .forEach(estudiante -> {
                    estudiante.setCurso(cursoValido);
                    estudianteRepository.save(estudiante);
                });
    }
}