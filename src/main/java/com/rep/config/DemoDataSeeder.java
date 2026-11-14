package com.rep.config;

import com.rep.model.*;
import com.rep.repositories.*;
import com.rep.service.archivos.AlmacenamientoArchivosService;
import com.rep.service.archivos.AlmacenamientoArchivosService.ArchivoGuardado;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;

/**
 * Datos de demostración (solo con {@code app.seed.demo=true} / {@code SEED_DEMO=true}).
 *
 * <p>Se ejecuta una sola vez: si ya existe el profesor de demostración no hace nada.
 * Todas las cuentas de demostración usan la contraseña {@value #PASSWORD}.</p>
 *
 * <p>Crea cursos, materias, profesores con asignaciones, estudiantes y actividades
 * (EXAMEN, QUIZ, TALLER) en todos los estados: sin empezar, en curso (respuestas a
 * medias), entregadas con nota automática, pendientes de revisión manual, revisadas por
 * el profesor, vencidas e inactivas. Incluye preguntas con imagen, material de estudio
 * (PNG, PDF, TXT) y asistencia de la última semana.</p>
 */
@Slf4j
@Component
@Order(10)
@ConditionalOnProperty(name = "app.seed.demo", havingValue = "true")
public class DemoDataSeeder implements CommandLineRunner {

    static final String PASSWORD = "Demo12345";
    static final String PROFESOR_MARCADOR = "1020304050";

    private final UsuarioRepository usuarioRepository;
    private final CursoRepository cursoRepository;
    private final MateriaRepository materiaRepository;
    private final ProfesorMateriaRepository profesorMateriaRepository;
    private final ActividadRepository actividadRepository;
    private final PreguntaRepository preguntaRepository;
    private final RespuestaEstudianteRepository respuestaRepository;
    private final RecursoRepository recursoRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final PeriodoAcademicoRepository periodoRepository;
    private final BloqueHorarioRepository bloqueHorarioRepository;
    private final AlmacenamientoArchivosService almacenamiento;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transaction;

    private final Random random = new Random(42);
    private String passwordHash;

    public DemoDataSeeder(UsuarioRepository usuarioRepository, CursoRepository cursoRepository,
            MateriaRepository materiaRepository, ProfesorMateriaRepository profesorMateriaRepository,
            ActividadRepository actividadRepository, PreguntaRepository preguntaRepository,
            RespuestaEstudianteRepository respuestaRepository, RecursoRepository recursoRepository,
            AsistenciaRepository asistenciaRepository, PeriodoAcademicoRepository periodoRepository,
            BloqueHorarioRepository bloqueHorarioRepository,
            AlmacenamientoArchivosService almacenamiento,
            PasswordEncoder passwordEncoder, TransactionTemplate transaction) {
        this.usuarioRepository = usuarioRepository;
        this.cursoRepository = cursoRepository;
        this.materiaRepository = materiaRepository;
        this.profesorMateriaRepository = profesorMateriaRepository;
        this.actividadRepository = actividadRepository;
        this.preguntaRepository = preguntaRepository;
        this.respuestaRepository = respuestaRepository;
        this.recursoRepository = recursoRepository;
        this.asistenciaRepository = asistenciaRepository;
        this.periodoRepository = periodoRepository;
        this.bloqueHorarioRepository = bloqueHorarioRepository;
        this.almacenamiento = almacenamiento;
        this.passwordEncoder = passwordEncoder;
        this.transaction = transaction;
    }

    @Override
    public void run(String... args) {
        if (usuarioRepository.existsByIdentificacion(PROFESOR_MARCADOR)) {
            log.info("Datos de demostración ya presentes; no se vuelven a crear.");
            return;
        }
        transaction.executeWithoutResult(status -> sembrar());
    }

    // ================================================================== escenario

    /**
     * Cuatro periodos de 25 % que terminan alrededor de hoy: todas las actividades de la
     * demostración caen en el cuarto, y los anteriores aparecen sin notas.
     */
    /** "LMXJV" = lunes a viernes; franja "HH:mm-HH:mm". */
    private void bloques(ProfesorMateria pm, String aula, String patron) {
        String[] partes = patron.split(" ");
        String[] horas = partes[1].split("-");
        for (char c : partes[0].toCharArray()) {
            com.rep.model.BloqueHorario b = new com.rep.model.BloqueHorario();
            b.setProfesorMateria(pm);
            b.setDia(java.time.DayOfWeek.of("LMXJVSD".indexOf(c) + 1));
            b.setHoraInicio(java.time.LocalTime.parse(horas[0]));
            b.setHoraFin(java.time.LocalTime.parse(horas[1]));
            b.setAula(aula);
            bloqueHorarioRepository.save(b);
        }
    }

    private void periodos() {
        if (!periodoRepository.findAll().isEmpty()) {
            return;
        }
        LocalDate hoy = LocalDate.now();
        LocalDate fin = hoy.plusDays(45);
        for (int n = 4; n >= 1; n--) {
            com.rep.model.PeriodoAcademico p = new com.rep.model.PeriodoAcademico();
            p.setAnio(hoy.getYear());
            p.setNumero(n);
            p.setNombre("Periodo " + n);
            p.setFechaFin(fin);
            p.setFechaInicio(fin.minusDays(89));
            p.setPorcentaje(25);
            periodoRepository.save(p);
            fin = p.getFechaInicio().minusDays(1);
        }
    }

    private void sembrar() {
        passwordHash = passwordEncoder.encode(PASSWORD);
        periodos();

        Curso c10a = curso(10, "A");
        Curso c11a = curso(11, "A");

        Materia mate = materia("Matemáticas");
        Materia fisica = materia("Física");
        Materia lengua = materia("Lengua Castellana");
        Materia ingles = materia("Inglés");
        Materia ciencias = materia("Ciencias Naturales");

        Profesor carlos = profesor(PROFESOR_MARCADOR, "Carlos", "Ramírez", "carlos.ramirez", 41);
        Profesor laura = profesor("1030405060", "Laura", "Gómez", "laura.gomez", 35);
        Profesor andres = profesor("1040506070", "Andrés", "Torres", "andres.torres", 29);

        ProfesorMateria mate10 = asignar(carlos, mate, c10a);
        ProfesorMateria fisica10 = asignar(carlos, fisica, c10a);
        ProfesorMateria mate11 = asignar(carlos, mate, c11a);
        ProfesorMateria lengua10 = asignar(laura, lengua, c10a);
        ProfesorMateria lengua11 = asignar(laura, lengua, c11a);
        ProfesorMateria ingles11 = asignar(laura, ingles, c11a);
        ProfesorMateria ciencias10 = asignar(andres, ciencias, c10a);

        // Horario semanal (sin cruces de curso ni de profesor)
        bloques(mate10, "Aula 101", "LXV 07:00-08:00");
        bloques(fisica10, "Laboratorio", "MJ 07:00-08:00");
        bloques(lengua10, "Aula 101", "LX 08:00-09:00");
        bloques(ciencias10, "Laboratorio", "MJV 08:00-09:00");
        bloques(mate11, "Aula 102", "LX 09:00-10:00");
        bloques(lengua11, "Aula 102", "MJ 09:00-10:00");
        bloques(ingles11, "Aula 102", "LV 10:00-11:00");

        List<Estudiante> e10 = List.of(
                estudiante("1098765432", "Valentina", "Torres", 16, c10a),
                estudiante("1098765433", "Mateo", "Rojas", 15, c10a),
                estudiante("1098765434", "Sofía", "Herrera", 16, c10a),
                estudiante("1098765435", "Samuel", "Castro", 15, c10a),
                estudiante("1098765436", "Isabella", "Moreno", 16, c10a),
                estudiante("1098765437", "Santiago", "Vargas", 15, c10a));
        List<Estudiante> e11 = List.of(
                estudiante("1098765438", "Mariana", "López", 17, c11a),
                estudiante("1098765439", "Daniel", "Pérez", 16, c11a),
                estudiante("1098765440", "Gabriela", "Ruiz", 17, c11a),
                estudiante("1098765441", "Nicolás", "Díaz", 17, c11a));

        LocalDate hoy = LocalDate.now();

        // ---------------- Matemáticas 10-A (Carlos): todos los estados ----------------
        Actividad quizDerivadas = actividad(mate10, "Quiz: Derivadas", Actividad.TipoActividad.QUIZ,
                "Repaso de reglas de derivación", hoy.plusDays(7), 20, true);
        opcionMultiple(quizDerivadas, "¿Cuál es la derivada de x²?", 0, "2x", "x", "x²/2", "2");
        opcionMultiple(quizDerivadas, "¿Cuál es la derivada de una constante?", 1, "1", "0", "La misma constante");
        verdaderoFalso(quizDerivadas, "La derivada de sen(x) es cos(x).", true);
        // 3 entregas con nota automática, 1 en curso (a medias), resto sin empezar
        entregar(quizDerivadas, e10.get(0), 1.0, null, null);
        entregar(quizDerivadas, e10.get(1), 0.67, null, null);
        entregar(quizDerivadas, e10.get(2), 0.33, null, null);
        enCurso(quizDerivadas, e10.get(3), 1);

        Actividad examenFunciones = actividad(mate10, "Examen parcial: Funciones", Actividad.TipoActividad.EXAMEN,
                "Primer parcial del periodo. Tiempo límite 60 minutos.", hoy.minusDays(5), 60, false);
        opcionMultiple(examenFunciones, "Si f(x) = 2x + 3, ¿cuánto vale f(2)?", 2, "5", "6", "7", "4");
        opcionMultiple(examenFunciones, "¿Qué tipo de función es f(x) = x²?", 0, "Cuadrática", "Lineal", "Exponencial");
        verdaderoFalso(examenFunciones, "Toda función lineal tiene pendiente constante.", true);
        abierta(examenFunciones, "Explique con sus palabras qué es el dominio de una función.");
        // Vencido: todos entregaron; dos revisados por el profesor con observaciones
        for (int i = 0; i < e10.size(); i++) {
            double acierto = new double[] { 1.0, 0.67, 0.33, 1.0, 0.67, 0.0 }[i];
            if (i == 0) entregar(examenFunciones, e10.get(i), acierto, carlos, "Excelente trabajo, respuesta abierta muy completa.");
            else if (i == 5) entregar(examenFunciones, e10.get(i), acierto, carlos, "Debe repasar el tema; asista a tutoría.");
            else entregar(examenFunciones, e10.get(i), acierto, null, null);
        }

        Actividad tallerLimites = actividad(mate10, "Taller: Límites", Actividad.TipoActividad.TALLER,
                "Resuelva y justifique. Se revisa manualmente.", hoy.plusDays(3), 45, true);
        opcionMultiple(tallerLimites, "lim x→2 de (x² − 4)/(x − 2) es:", 1, "0", "4", "No existe");
        abierta(tallerLimites, "Describa un ejemplo de límite que no exista y explique por qué.");
        entregar(tallerLimites, e10.get(0), 1.0, null, null);   // pendiente de revisión manual
        entregar(tallerLimites, e10.get(4), 0.0, null, null);   // pendiente de revisión manual
        entregar(tallerLimites, e10.get(2), 1.0, carlos, "Buen ejemplo con funciones a trozos.");

        Actividad quizTrigo = actividad(mate10, "Quiz: Trigonometría", Actividad.TipoActividad.QUIZ,
                "Observe la figura antes de responder.", hoy.plusDays(14), 15, false);
        Pregunta conImagen = opcionMultiple(quizTrigo, "Observe el triángulo de la figura: ¿cuánto mide la hipotenusa?",
                1, "4", "5", "7", "12");
        adjuntarImagen(conImagen, "triangulo-3-4-5.png", "Triángulo rectángulo 3-4-5", new Color(79, 70, 229));

        Actividad inactiva = actividad(mate10, "Borrador: Integrales", Actividad.TipoActividad.QUIZ,
                "Actividad aún no publicada", hoy.plusDays(30), 20, false);
        opcionMultiple(inactiva, "∫ 2x dx =", 0, "x² + C", "2 + C", "x + C");
        inactiva.setActiva(false);

        // ---------------- Física 10-A (Carlos) ----------------
        Actividad cinematica = actividad(fisica10, "Taller: Cinemática", Actividad.TipoActividad.TALLER,
                "Movimiento rectilíneo uniforme", hoy.plusDays(5), 40, true);
        Pregunta grafica = opcionMultiple(cinematica, "Según la gráfica, ¿qué tipo de movimiento representa?",
                0, "Uniforme", "Acelerado", "En reposo");
        adjuntarImagen(grafica, "grafica-posicion-tiempo.png", "Posición vs. tiempo", new Color(14, 165, 233));
        verdaderoFalso(cinematica, "En el MRU la velocidad es constante.", true);
        abierta(cinematica, "¿Qué diferencia hay entre rapidez y velocidad?");
        entregar(cinematica, e10.get(1), 1.0, null, null);
        enCurso(cinematica, e10.get(5), 1);

        // ---------------- Matemáticas 11-A (Carlos) ----------------
        Actividad probabilidad = actividad(mate11, "Examen: Probabilidad", Actividad.TipoActividad.EXAMEN,
                "Probabilidad clásica y condicional", hoy.minusDays(2), 50, false);
        opcionMultiple(probabilidad, "Probabilidad de obtener cara al lanzar una moneda:", 0, "1/2", "1/3", "1");
        opcionMultiple(probabilidad, "Al lanzar un dado, P(número par) =", 2, "1/6", "1/3", "1/2");
        for (int i = 0; i < e11.size(); i++) {
            entregar(probabilidad, e11.get(i), new double[] { 1.0, 0.5, 1.0, 0.0 }[i], null, null);
        }

        // ---------------- Lengua (Laura) ----------------
        Actividad comprension = actividad(lengua11, "Examen: Comprensión lectora", Actividad.TipoActividad.EXAMEN,
                "Lea el texto del material de estudio antes de responder.", hoy.plusDays(2), 60, false);
        opcionMultiple(comprension, "La idea principal de un texto es:", 0,
                "El tema central que desarrolla el autor", "La primera oración", "El título");
        abierta(comprension, "Resuma en tres líneas el texto leído.");
        entregar(comprension, e11.get(0), 1.0, laura, "Muy buen resumen.");
        entregar(comprension, e11.get(1), 1.0, null, null);
        enCurso(comprension, e11.get(2), 1);

        Actividad ortografia = actividad(lengua10, "Quiz: Ortografía", Actividad.TipoActividad.QUIZ,
                "Uso de tildes y signos de puntuación", hoy.plusDays(10), 15, true);
        opcionMultiple(ortografia, "¿Cuál palabra está bien escrita?", 1, "Arbol", "Árbol", "Arból");
        verdaderoFalso(ortografia, "Las palabras agudas terminadas en vocal llevan tilde.", true);

        Actividad readingQuiz = actividad(ingles11, "Quiz: Reading comprehension", Actividad.TipoActividad.QUIZ,
                "Short text about daily routines", hoy.plusDays(6), 20, true);
        opcionMultiple(readingQuiz, "'She goes to school' is in the...", 0, "Present simple", "Past simple", "Future");

        // ---------------- Ciencias 10-A (Andrés) ----------------
        Actividad celula = actividad(ciencias10, "Taller: La célula", Actividad.TipoActividad.TALLER,
                "Partes de la célula animal y vegetal", hoy.minusDays(1), 30, false);
        opcionMultiple(celula, "¿Qué organelo realiza la fotosíntesis?", 2, "Mitocondria", "Núcleo", "Cloroplasto");
        verdaderoFalso(celula, "Las células animales tienen pared celular.", false);
        entregar(celula, e10.get(0), 0.5, andres, "Revise la diferencia entre célula animal y vegetal.");

        // ---------------- Material de estudio ----------------
        material(mate10, "Guía de derivadas", "Reglas básicas con ejemplos resueltos.", "guia-derivadas.pdf",
                pdf("Guía de derivadas", "Regla de la potencia: d/dx x^n = n x^(n-1)"));
        material(mate10, "Mapa conceptual: funciones", "Resumen visual de los tipos de funciones.",
                "mapa-funciones.png", imagen("Tipos de funciones", new Color(79, 70, 229)));
        material(fisica10, "Fórmulas de cinemática", null, "formulas-cinematica.txt",
                "v = d / t\na = (vf - vi) / t\nd = vi*t + (a*t^2)/2\n".getBytes(StandardCharsets.UTF_8));
        material(lengua11, "Texto para comprensión lectora", "Leer antes del examen.", "texto-lectura.txt",
                "La lectura es una de las herramientas más poderosas para aprender...\n".getBytes(StandardCharsets.UTF_8));
        material(ciencias10, "Diagrama de la célula", "Célula animal y vegetal.", "celula.png",
                imagen("La célula", new Color(22, 163, 74)));

        // ---------------- Asistencia de la última semana ----------------
        asistenciaSemana(mate10, e10, hoy);
        asistenciaSemana(lengua11, e11, hoy);

        log.warn("""

                ================================================================
                 DATOS DE DEMOSTRACIÓN CREADOS  (contraseña de todos: {})
                 Profesores:  {} (Carlos), 1030405060 (Laura), 1040506070 (Andrés)
                 Estudiantes 10-A: 1098765432 … 1098765437
                 Estudiantes 11-A: 1098765438 … 1098765441
                 Desactive SEED_DEMO en producción.
                ================================================================""", PASSWORD, PROFESOR_MARCADOR);
    }

    // ================================================================== creación de entidades

    private Curso curso(int grado, String grupo) {
        return cursoRepository.findByGradoAndGrupo(grado, grupo).orElseGet(() -> {
            Curso c = new Curso();
            c.setGrado(grado);
            c.setGrupo(grupo);
            return cursoRepository.save(c);
        });
    }

    private Materia materia(String nombre) {
        return materiaRepository.findByNombre(nombre).orElseGet(() -> {
            Materia m = new Materia();
            m.setNombre(nombre);
            return materiaRepository.save(m);
        });
    }

    private Profesor profesor(String ident, String nombre, String apellido, String correo, int edad) {
        Profesor p = new Profesor();
        datosComunes(p, ident, nombre, apellido, correo, edad, Usuario.TipoIdentificacion.CC, Usuario.Rol.PROFESOR);
        p.setEstado(Profesor.EstadoProfesor.activo);
        p.setFechaIngreso(LocalDate.now().minusYears(2));
        return usuarioRepository.save(p);
    }

    private Estudiante estudiante(String ident, String nombre, String apellido, int edad, Curso curso) {
        Estudiante e = new Estudiante();
        datosComunes(e, ident, nombre, apellido, nombre.toLowerCase() + "." + apellido.toLowerCase(), edad,
                Usuario.TipoIdentificacion.TI, Usuario.Rol.ESTUDIANTE);
        e.setCurso(curso);
        e.setEstado(Estudiante.EstadoEstudiante.activo);
        return usuarioRepository.save(e);
    }

    private void datosComunes(Usuario u, String ident, String nombre, String apellido, String correo, int edad,
            Usuario.TipoIdentificacion tipo, Usuario.Rol rol) {
        u.setIdentificacion(ident);
        u.setNombre(nombre);
        u.setApellido(apellido);
        u.setCorreo(sinTildes(correo) + "@demo.rep.edu");
        u.setEdad(edad);
        u.setTipoIdentificacion(tipo);
        u.setRol(rol);
        u.setContraseña(passwordHash);
        u.setActivo(true);
    }

    private ProfesorMateria asignar(Profesor p, Materia m, Curso c) {
        ProfesorMateria pm = new ProfesorMateria();
        pm.setProfesor(p);
        pm.setMateria(m);
        pm.setCurso(c);
        return profesorMateriaRepository.save(pm);
    }

    private Actividad actividad(ProfesorMateria pm, String titulo, Actividad.TipoActividad tipo, String descripcion,
            LocalDate entrega, int duracion, boolean reintentos) {
        Actividad a = new Actividad();
        a.setTitulo(titulo);
        a.setTipo(tipo);
        a.setDescripcion(descripcion);
        a.setFechaEntrega(entrega);
        a.setDuracionMinutos(duracion);
        a.setProfesorMateria(pm);
        a.setFechaCreacion(LocalDateTime.now().minusDays(10));
        a.setActiva(true);
        a.setPermitirReintentos(reintentos);
        return actividadRepository.save(a);
    }

    private Pregunta opcionMultiple(Actividad a, String enunciado, int correcta, String... opciones) {
        Pregunta p = pregunta(a, enunciado, Pregunta.TipoPregunta.OPCION_MULTIPLE);
        for (int i = 0; i < opciones.length; i++) {
            Opcion o = new Opcion();
            o.setTexto(opciones[i]);
            o.setEsCorrecta(i == correcta);
            p.addOpcion(o);
        }
        return guardar(a, p);
    }

    private Pregunta verdaderoFalso(Actividad a, String enunciado, boolean esVerdadero) {
        Pregunta p = pregunta(a, enunciado, Pregunta.TipoPregunta.VERDADERO_FALSO);
        for (String texto : List.of("Verdadero", "Falso")) {
            Opcion o = new Opcion();
            o.setTexto(texto);
            o.setEsCorrecta(texto.equals("Verdadero") == esVerdadero);
            p.addOpcion(o);
        }
        return guardar(a, p);
    }

    private Pregunta abierta(Actividad a, String enunciado) {
        return guardar(a, pregunta(a, enunciado, Pregunta.TipoPregunta.RESPUESTA_ABIERTA));
    }

    private static Pregunta pregunta(Actividad a, String enunciado, Pregunta.TipoPregunta tipo) {
        Pregunta p = new Pregunta();
        p.setActividad(a);
        p.setEnunciado(enunciado);
        p.setTipo(tipo);
        return p;
    }

    private Pregunta guardar(Actividad a, Pregunta p) {
        Pregunta guardada = preguntaRepository.save(p);
        a.getPreguntas().add(guardada);
        return guardada;
    }

    private void adjuntarImagen(Pregunta p, String nombre, String titulo, Color color) {
        ArchivoGuardado g = almacenamiento.guardarBytes(imagen(titulo, color), nombre, "preguntas");
        p.setArchivoUrl(g.ruta());
        p.setArchivoTipo(g.tipoContenido());
        p.setNombreArchivo(g.nombreOriginal());
        preguntaRepository.save(p);
    }

    /**
     * Entrega completa. {@code proporcionAciertos} indica qué fracción de las preguntas
     * objetivas responde bien; la nota se calcula igual que en la corrección automática.
     */
    private void entregar(Actividad a, Estudiante e, double proporcionAciertos, Profesor revisor, String observaciones) {
        RespuestaEstudiante r = nuevaRespuesta(a, e);
        List<Pregunta> preguntas = ordenadas(a);
        long objetivas = preguntas.stream().filter(p -> p.getTipo() != Pregunta.TipoPregunta.RESPUESTA_ABIERTA).count();
        long aciertosObjetivo = Math.round(objetivas * proporcionAciertos);
        long aciertos = 0;
        for (Pregunta p : preguntas) {
            if (p.getTipo() == Pregunta.TipoPregunta.RESPUESTA_ABIERTA) {
                responderAbierta(r, e, p, "Respuesta de " + e.getNombre() + ": desarrollo propio del tema.");
            } else {
                boolean correcta = aciertos < aciertosObjetivo;
                if (correcta) aciertos++;
                responderOpcion(r, e, p, correcta);
            }
        }
        float nota = objetivas == 0 ? 0f : Math.round(aciertos * 50f / objetivas) / 10f;
        if (revisor != null) {
            // El profesor revisa las preguntas abiertas y ajusta la nota (+0.5 máx. 5.0)
            nota = Math.min(5f, nota + 0.5f);
            r.setCalificadoPor(revisor);
            r.setObservaciones(observaciones);
        }
        r.setNota(nota);
        r.setEntregado(true);
        r.setFechaEntrega(r.getFechaInicio().plusMinutes(5 + random.nextInt(Math.max(1, a.getDuracionMinutos() - 5))));
        r.setDuracionMinutos((int) java.time.Duration.between(r.getFechaInicio(), r.getFechaEntrega()).toMinutes());
        respuestaRepository.save(r);
    }

    /** Actividad empezada pero no entregada: solo algunas preguntas respondidas. */
    private void enCurso(Actividad a, Estudiante e, int respondidas) {
        RespuestaEstudiante r = nuevaRespuesta(a, e);
        r.setFechaInicio(LocalDateTime.now().minusMinutes(8));
        List<Pregunta> preguntas = ordenadas(a);
        for (int i = 0; i < Math.min(respondidas, preguntas.size()); i++) {
            Pregunta p = preguntas.get(i);
            if (p.getTipo() == Pregunta.TipoPregunta.RESPUESTA_ABIERTA) {
                responderAbierta(r, e, p, "Borrador sin terminar…");
            } else {
                responderOpcion(r, e, p, random.nextBoolean());
            }
        }
        r.setEntregado(false);
        respuestaRepository.save(r);
    }

    private RespuestaEstudiante nuevaRespuesta(Actividad a, Estudiante e) {
        RespuestaEstudiante r = new RespuestaEstudiante();
        r.setActividad(a);
        r.setEstudiante(e);
        LocalDateTime inicio = a.getFechaHoraEntrega().isBefore(LocalDateTime.now())
                ? a.getFechaHoraEntrega().minusDays(1)
                : LocalDateTime.now().minusDays(1 + random.nextInt(3));
        r.setFechaInicio(inicio);
        return r;
    }

    private static void responderOpcion(RespuestaEstudiante r, Estudiante e, Pregunta p, boolean correcta) {
        Opcion elegida = p.getOpciones().stream()
                .filter(o -> Boolean.TRUE.equals(o.getEsCorrecta()) == correcta)
                .findFirst().orElse(null);
        RespuestaPregunta rp = new RespuestaPregunta();
        rp.setPregunta(p);
        rp.setEstudiante(e);
        rp.setOpcion(elegida);
        rp.setEsCorrecta(elegida != null && Boolean.TRUE.equals(elegida.getEsCorrecta()));
        r.addRespuestaPregunta(rp);
    }

    private static void responderAbierta(RespuestaEstudiante r, Estudiante e, Pregunta p, String texto) {
        RespuestaPregunta rp = new RespuestaPregunta();
        rp.setPregunta(p);
        rp.setEstudiante(e);
        rp.setRespuestaAbierta(texto);
        r.addRespuestaPregunta(rp);
    }

    private static List<Pregunta> ordenadas(Actividad a) {
        return a.getPreguntas().stream().sorted(Comparator.comparing(Pregunta::getId)).toList();
    }

    private void material(ProfesorMateria pm, String titulo, String descripcion, String archivo, byte[] contenido) {
        ArchivoGuardado g = almacenamiento.guardarBytes(contenido, archivo, "material");
        Recurso r = new Recurso();
        r.setNombre(titulo);
        r.setDescripcion(descripcion);
        r.setTipo(g.tipoContenido());
        r.setUrl(g.ruta());
        r.setNombreArchivo(g.nombreOriginal());
        r.setTamanoBytes(g.tamanoBytes());
        r.setProfesorMateria(pm);
        r.setFechaSubida(LocalDateTime.now().minusDays(random.nextInt(10)));
        recursoRepository.save(r);
    }

    private void asistenciaSemana(ProfesorMateria pm, List<Estudiante> estudiantes, LocalDate hoy) {
        for (int d = 1; d <= 7; d++) {
            LocalDate fecha = hoy.minusDays(d);
            if (fecha.getDayOfWeek() == DayOfWeek.SATURDAY || fecha.getDayOfWeek() == DayOfWeek.SUNDAY) {
                continue;
            }
            for (Estudiante e : estudiantes) {
                Asistencia a = new Asistencia();
                a.setEstudiante(e);
                a.setProfesorMateria(pm);
                a.setFecha(fecha);
                int dado = random.nextInt(10);
                a.setEstado(dado < 8 ? Asistencia.EstadoAsistencia.PRESENTE
                        : dado == 8 ? Asistencia.EstadoAsistencia.EXCUSA : Asistencia.EstadoAsistencia.NO_INGRESO);
                if (a.getEstado() == Asistencia.EstadoAsistencia.EXCUSA) {
                    a.setTipoExcusa("MEDICA");
                    a.setObservacion("Presentó incapacidad médica");
                }
                asistenciaRepository.save(a);
            }
        }
    }

    // ================================================================== archivos generados

    /** Imagen PNG sencilla (figura + título) generada en memoria. */
    static byte[] imagen(String titulo, Color color) {
        BufferedImage img = new BufferedImage(640, 400, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(248, 250, 252));
        g.fillRect(0, 0, 640, 400);
        g.setColor(color);
        g.setStroke(new BasicStroke(6));
        g.drawPolygon(new int[] { 120, 120, 440 }, new int[] { 320, 80, 320 }, 3);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
        g.drawString("3", 90, 210);
        g.drawString("4", 270, 355);
        g.drawString("?", 300, 190);
        g.setColor(new Color(15, 23, 42));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
        g.drawString(titulo, 40, 45);
        g.dispose();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(img, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** PDF mínimo válido de una página con dos líneas de texto. */
    static byte[] pdf(String titulo, String linea) {
        String contenido = "BT /F1 22 Tf 72 720 Td (" + escaparPdf(titulo) + ") Tj ET\n"
                + "BT /F1 14 Tf 72 680 Td (" + escaparPdf(linea) + ") Tj ET\n";
        List<String> objetos = List.of(
                "<< /Type /Catalog /Pages 2 0 R >>",
                "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R "
                        + "/Resources << /Font << /F1 5 0 R >> >> >>",
                "<< /Length " + contenido.getBytes(StandardCharsets.ISO_8859_1).length + " >>\nstream\n" + contenido + "endstream",
                "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>");
        StringBuilder sb = new StringBuilder("%PDF-1.4\n");
        List<Integer> offsets = new ArrayList<>();
        for (int i = 0; i < objetos.size(); i++) {
            offsets.add(sb.toString().getBytes(StandardCharsets.ISO_8859_1).length);
            sb.append(i + 1).append(" 0 obj\n").append(objetos.get(i)).append("\nendobj\n");
        }
        int xref = sb.toString().getBytes(StandardCharsets.ISO_8859_1).length;
        sb.append("xref\n0 ").append(objetos.size() + 1).append("\n0000000000 65535 f \n");
        offsets.forEach(o -> sb.append(String.format("%010d 00000 n \n", o)));
        sb.append("trailer\n<< /Size ").append(objetos.size() + 1).append(" /Root 1 0 R >>\nstartxref\n")
                .append(xref).append("\n%%EOF\n");
        return sb.toString().getBytes(StandardCharsets.ISO_8859_1);
    }

    private static String escaparPdf(String s) {
        return s.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }

    private static String sinTildes(String s) {
        return java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}
