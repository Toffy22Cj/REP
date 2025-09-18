// EstudianteController.java
package com.rep.controller.views;

import com.rep.dto.actividad.*;
import com.rep.dto.tokens.JwtTokenHolder;
import com.rep.model.*;
import com.rep.service.funciones.EstudianteApiService;
import com.rep.service.logica.ActividadService;
import com.rep.service.logica.EstudianteService;
import jakarta.persistence.EntityNotFoundException;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class EstudianteController extends BaseTokenController {
    private static final Logger logger = LoggerFactory.getLogger(EstudianteController.class);
private final ActividadService actividadService;
    private final EstudianteService estudianteService;
    private final EstudianteApiService estudianteApiService;
    @FXML private Button btnResolverActividad;
    @FXML private Label lblNombreEstudiante;
    @FXML private Label lblEstado;
    @FXML private Button btnMaterias;
    @FXML private Button btnActividades;
    @FXML private Button btnNotificaciones;
    @FXML private Button btnRefrescar;
    @FXML private Button btnCerrarSesion;
    @FXML private StackPane contentPane;

    @Autowired
    public EstudianteController(EstudianteService estudianteService,
                                EstudianteApiService estudianteApiService,
                                JwtTokenHolder jwtTokenHolder,ActividadService actividadService) {
        this.estudianteService = estudianteService;
        this.actividadService = actividadService;
        this.estudianteApiService = estudianteApiService;
        setJwtTokenHolder(jwtTokenHolder);
    }

    @Override
    protected Label getEstadoLabel() {
        return lblEstado;
    }

    @FXML
    public void initialize() {
        logger.info("Inicializando controlador Estudiante...");

        if (lblNombreEstudiante == null || btnMaterias == null || btnActividades == null) {
            logger.error("Error: Componentes FXML no inyectados correctamente");
            mostrarEstado("Error de configuración", Color.RED);
            return;
        }

        configurarAccionesBotones();

        if (jwtTokenHolder != null && jwtTokenHolder.getToken() != null) {
            Platform.runLater(this::cargarDatosIniciales);
        } else {
            logger.warn("Token no disponible al inicializar, esperando inyección...");
            mostrarEstado("Cargando sesión...", Color.ORANGE);
        }
    }

    private void configurarAccionesBotones() {
//        btnResolverActividad.setOnAction(e -> mostrarResolucionActividad());
        btnMaterias.setOnAction(e -> mostrarMaterias());
        btnActividades.setOnAction(e -> mostrarActividades());
//        btnNotificaciones.setOnAction(e -> mostrarNotificaciones());
        btnRefrescar.setOnAction(e -> refrescarDatos());
        btnCerrarSesion.setOnAction(e -> cerrarSesion());
    }

    private void cargarDatosIniciales() {
        try {
            Long estudianteId = jwtTokenHolder.getUserId();

            Estudiante estudiante = estudianteService.getEstudianteById(estudianteId);
            lblNombreEstudiante.setText(estudiante.getNombre());
            mostrarEstado("Conectado", Color.GREEN);

            mostrarMaterias();
        } catch (Exception e) {
            logger.error("Error al cargar datos iniciales", e);
            mostrarEstado("Error al cargar datos", Color.RED);
        }
    }

    private void mostrarMaterias() {
        try {
            Long estudianteId = jwtTokenHolder.getUserId();
            List<MateriaDTO> materias = estudianteApiService.getMateriasByEstudiante(estudianteId);

            VBox contenedorMaterias = new VBox(10);
            contenedorMaterias.setPadding(new Insets(15));

            if (materias.isEmpty()) {
                contenedorMaterias.getChildren().add(new Label("No hay materias asignadas"));
            } else {
                materias.forEach(materia -> {
                    Hyperlink linkMateria = new Hyperlink(materia.getNombre());
                    linkMateria.setStyle("-fx-font-size: 14;");
                    linkMateria.setOnAction(e -> mostrarActividadesPorMateria(materia.getId()));
                    contenedorMaterias.getChildren().add(linkMateria);
                });
            }

            ScrollPane scrollPane = new ScrollPane(contenedorMaterias);
            scrollPane.setFitToWidth(true);
            contentPane.getChildren().setAll(scrollPane);
        } catch (Exception e) {
            logger.error("Error al cargar materias", e);
            mostrarAlerta("Error", "No se pudieron cargar las materias");
        }
    }

    private void mostrarActividades() {
        try {
            Long estudianteId = jwtTokenHolder.getUserId();
            List<ActividadDTO> actividades = estudianteApiService.getActividadesByEstudiante(estudianteId);

            ScrollPane scrollPane = new ScrollPane();
            VBox contenedorActividades = new VBox(10);
            contenedorActividades.setPadding(new Insets(15));

            if (actividades.isEmpty()) {
                contenedorActividades.getChildren().add(new Label("No hay actividades asignadas"));
            } else {
                Map<String, List<ActividadDTO>> actividadesPorMateria = actividades.stream()
                        .collect(Collectors.groupingBy(ActividadDTO::getMateriaNombre));

                actividadesPorMateria.forEach((materia, acts) -> {
                    Label lblMateria = new Label(materia);
                    lblMateria.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");
                    contenedorActividades.getChildren().add(lblMateria);

                    acts.forEach(actividad -> {
                        VBox card = crearCardActividad(actividad);
                        contenedorActividades.getChildren().add(card);
                    });
                });
            }

            scrollPane.setContent(contenedorActividades);
            scrollPane.setFitToWidth(true);
            contentPane.getChildren().setAll(scrollPane);
        } catch (Exception e) {
            logger.error("Error al cargar actividades", e);
            mostrarAlerta("Error", "No se pudieron cargar las actividades");
        }
    }

    private VBox crearCardActividad(ActividadDTO actividad) {
        VBox card = new VBox(5);
        card.setStyle("-fx-border-color: #ddd; -fx-border-radius: 5; -fx-padding: 10;");

        Label lblTitulo = new Label(actividad.getTitulo());
        lblTitulo.setStyle("-fx-font-weight: bold;");

        Label lblDescripcion = new Label(actividad.getDescripcion());
        lblDescripcion.setWrapText(true);

        Label lblFecha = new Label("Entrega: " + actividad.getFechaEntrega().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        Label lblTipo = new Label("Tipo: " + actividad.getTipo());
        Label lblMateria = new Label("Materia: " + actividad.getMateriaNombre());

        // Añadir botón de resolver si la actividad está pendiente
        Button btnResolver = new Button("Resolver actividad");
        btnResolver.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
        btnResolver.setOnAction(e -> mostrarResolucionActividad(actividad.getId()));

        card.getChildren().addAll(lblTitulo, lblDescripcion, lblMateria, lblFecha, lblTipo, btnResolver);
        return card;
    }

    private void mostrarActividadesPorMateria(Long materiaId) {
        try {
            Long estudianteId = jwtTokenHolder.getUserId();
            List<ActividadDTO> actividades = estudianteApiService.getActividadesByMateria(estudianteId, materiaId);

            VBox contenedor = new VBox(10);
            contenedor.setPadding(new Insets(15));

            if (actividades.isEmpty()) {
                contenedor.getChildren().add(new Label("No hay actividades para esta materia"));
            } else {
                actividades.forEach(actividad -> {
                    VBox card = crearCardActividad(actividad);
                    contenedor.getChildren().add(card);
                });
            }

            ScrollPane scrollPane = new ScrollPane(contenedor);
            scrollPane.setFitToWidth(true);
            contentPane.getChildren().setAll(scrollPane);
        } catch (Exception e) {
            logger.error("Error al cargar actividades por materia", e);
            mostrarAlerta("Error", "No se pudieron cargar las actividades de la materia");
        }
    }

//    private void mostrarNotificaciones() {
//        try {
//            Long estudianteId = jwtTokenHolder.getUserId();
//            List<Notificacion> notificaciones = estudianteService.getNotificacionesByEstudiante(estudianteId, false);
//            notificaciones.addAll(estudianteService.getNotificacionesByEstudiante(estudianteId, true));
//
//            VBox contenedorNotificaciones = new VBox(10);
//            contenedorNotificaciones.setPadding(new Insets(15));
//
//            if (notificaciones.isEmpty()) {
//                contenedorNotificaciones.getChildren().add(new Label("No hay notificaciones"));
//            } else {
//                for (Notificacion notificacion : notificaciones) {
//                    VBox card = new VBox(5);
//                    card.setStyle("-fx-border-color: #ddd; -fx-border-radius: 5; -fx-padding: 10; " +
//                            (notificacion.isLeida() ? "" : "-fx-background-color: #f0f8ff;"));
//
//                    if (notificacion.getActividad() != null) {
//                        Label lblActividad = new Label("Actividad: " + notificacion.getActividad().getTitulo());
//                        lblActividad.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");
//                        card.getChildren().add(lblActividad);
//                    }
//
//                    Label lblMensaje = new Label(notificacion.getMensaje());
//                    lblMensaje.setWrapText(true);
//
//                    Label lblFecha = new Label(notificacion.getFechaCreacion()
//                            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
//                    lblFecha.setStyle("-fx-text-fill: #7f8c8d;");
//
//                    if (!notificacion.isLeida()) {
//                        Button btnMarcar = new Button("Marcar como leída");
//                        btnMarcar.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
//                        btnMarcar.setOnAction(e -> {
//                            estudianteService.marcarNotificacionComoLeida(notificacion.getId());
//                            card.setStyle("-fx-border-color: #ddd; -fx-border-radius: 5; -fx-padding: 10;");
//                            btnMarcar.setDisable(true);
//                        });
//                        card.getChildren().addAll(lblMensaje, lblFecha, btnMarcar);
//                    } else {
//                        card.getChildren().addAll(lblMensaje, lblFecha);
//                    }
//
//                    contenedorNotificaciones.getChildren().add(card);
//                }
//            }
//
//            ScrollPane scrollPane = new ScrollPane(contenedorNotificaciones);
//            scrollPane.setFitToWidth(true);
//            contentPane.getChildren().setAll(scrollPane);
//        } catch (Exception e) {
//            logger.error("Error al cargar notificaciones", e);
//            mostrarAlerta("Error", "No se pudieron cargar las notificaciones");
//        }
//    }
// En EstudianteController.java

    // Añade estos métodos para manejar la resolución de actividades
    @FXML
    private void mostrarResolucionActividad(Long actividadId) {
        try {
            if (actividadId == null) {
                throw new IllegalArgumentException("ID de actividad no puede ser nulo");
            }

            ActividadConPreguntasDTO actividad = actividadService.getActividadConPreguntas(actividadId);
            if (actividad == null || actividad.getPreguntas() == null || actividad.getPreguntas().isEmpty()) {
                mostrarAdvertencia("La actividad no contiene preguntas");
                return;
            }

            mostrarFormulario(actividad);
        } catch (EntityNotFoundException e) {
            mostrarError("Actividad no encontrada");
        } catch (IllegalArgumentException e) {
            mostrarError(e.getMessage());
        } catch (Exception e) {
            logger.error("Error al cargar actividad", e);
            mostrarError("Error inesperado al cargar la actividad");
        }
    }
    private void mostrarFormulario(ActividadConPreguntasDTO actividad) {
        VBox formularioContainer = new VBox(10);
        formularioContainer.setPadding(new Insets(15));
        formularioContainer.setStyle("-fx-background-color: #f9f9f9;");

        // Título y descripción
        Label tituloActividad = new Label(actividad.getTitulo());
        tituloActividad.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-padding: 0 0 10px 0;");

        if (actividad.getDescripcion() != null && !actividad.getDescripcion().isEmpty()) {
            Label descripcion = new Label(actividad.getDescripcion());
            descripcion.setStyle("-fx-font-size: 14px; -fx-padding: 0 0 15px 0;");
            descripcion.setWrapText(true);
            formularioContainer.getChildren().addAll(tituloActividad, descripcion);
        } else {
            formularioContainer.getChildren().add(tituloActividad);
        }

        // Contador de preguntas
        Label contadorPreguntas = new Label("Pregunta 1 de " + actividad.getPreguntas().size());
        contadorPreguntas.setStyle("-fx-font-weight: bold; -fx-padding: 0 0 10px 0;");
        formularioContainer.getChildren().add(contadorPreguntas);

        // Lista de preguntas
        for (int i = 0; i < actividad.getPreguntas().size(); i++) {
            PreguntaConOpcionesDTO pregunta = actividad.getPreguntas().get(i);
            VBox preguntaBox = new VBox(10);
            preguntaBox.setPadding(new Insets(15));
            preguntaBox.setStyle("-fx-border-color: #ddd; -fx-border-radius: 5; -fx-background-color: white;");
            preguntaBox.setUserData(i); // Almacenar índice de la pregunta

            // Enunciado
            Label enunciado = new Label((i + 1) + ". " + pregunta.getEnunciado());
            enunciado.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
            enunciado.setWrapText(true);

            // Contenedor de respuesta
            VBox contenidoPregunta = new VBox(8);

            if (pregunta.getTipo() == Pregunta.TipoPregunta.RESPUESTA_ABIERTA) {
                TextArea respuesta = new TextArea();
                respuesta.setPromptText("Escribe tu respuesta aquí...");
                respuesta.setPrefRowCount(3);
                respuesta.setWrapText(true);
                contenidoPregunta.getChildren().add(respuesta);
            } else {
                ToggleGroup grupoOpciones = new ToggleGroup();
                if (pregunta.getOpciones() != null && !pregunta.getOpciones().isEmpty()) {
                    for (OpcionDTO opcion : pregunta.getOpciones()) {
                        RadioButton rb = new RadioButton(opcion.getTexto());
                        rb.setToggleGroup(grupoOpciones);
                        rb.setUserData(opcion.getId());
                        rb.setWrapText(true);
                        contenidoPregunta.getChildren().add(rb);
                    }
                } else {
                    Label sinOpciones = new Label("Esta pregunta no tiene opciones definidas");
                    sinOpciones.setStyle("-fx-text-fill: #999;");
                    contenidoPregunta.getChildren().add(sinOpciones);
                }
            }

            preguntaBox.getChildren().addAll(enunciado, contenidoPregunta);
            formularioContainer.getChildren().add(preguntaBox);
        }

        // Botón de enviar
        Button btnEnviar = new Button("Enviar respuestas");
        btnEnviar.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
        btnEnviar.setOnAction(e -> manejarEnvioRespuestas(actividad, formularioContainer));

        HBox botonera = new HBox(btnEnviar);
        botonera.setAlignment(Pos.CENTER_RIGHT);
        botonera.setPadding(new Insets(15, 0, 0, 0));

        formularioContainer.getChildren().add(botonera);

        // Configurar scroll pane
        ScrollPane scrollPane = new ScrollPane(formularioContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background: #f9f9f9; -fx-border-color: transparent;");

        contentPane.getChildren().clear();
        contentPane.getChildren().add(scrollPane);
    }
    private void manejarEnvioRespuestas(ActividadConPreguntasDTO actividad, VBox formularioContainer) {
        try {
            // Confirmación antes de enviar
            Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
            confirmacion.setTitle("Confirmar envío");
            confirmacion.setHeaderText("¿Estás seguro de que quieres enviar tus respuestas?");
            confirmacion.setContentText("No podrás modificarlas después de enviarlas.");

            Optional<ButtonType> resultado = confirmacion.showAndWait();
            if (!resultado.isPresent() || resultado.get() != ButtonType.OK) {
                return;
            }

            // Crear DTO de solicitud
            ActividadResueltaDTO request = new ActividadResueltaDTO();
            request.setActividadId(actividad.getId());
            request.setEstudianteId(jwtTokenHolder.getUserId());
            request.setFechaEnvio(LocalDateTime.now());

            // Recolectar respuestas
            List<RespuestaPreguntaDTO> respuestas = new ArrayList<>();
            boolean todasRespondidas = true;
            StringBuilder preguntasNoRespondidas = new StringBuilder();

            for (Node node : formularioContainer.getChildren()) {
                if (node instanceof VBox) {
                    VBox preguntaBox = (VBox) node;
                    Integer preguntaIndex = (Integer) preguntaBox.getUserData();
                    if (preguntaIndex != null) {
                        PreguntaConOpcionesDTO pregunta = actividad.getPreguntas().get(preguntaIndex);
                        RespuestaPreguntaDTO respuesta = new RespuestaPreguntaDTO();
                        respuesta.setPreguntaId(pregunta.getId());

                        VBox contenidoPregunta = (VBox) preguntaBox.getChildren().get(1); // El segundo nodo es el contenedor de respuesta
                        boolean respondida = false;

                        if (pregunta.getTipo() == Pregunta.TipoPregunta.RESPUESTA_ABIERTA) {
                            TextArea textArea = (TextArea) contenidoPregunta.getChildren().get(0);
                            if (textArea.getText() != null && !textArea.getText().trim().isEmpty()) {
                                respuesta.setRespuestaAbierta(textArea.getText());
                                respondida = true;
                            }
                        } else {
                            for (Node opcionNode : contenidoPregunta.getChildren()) {
                                if (opcionNode instanceof RadioButton && ((RadioButton) opcionNode).isSelected()) {
                                    respuesta.setOpcionId((Long) opcionNode.getUserData());
                                    respondida = true;
                                    break;
                                }
                            }
                        }

                        if (!respondida) {
                            todasRespondidas = false;
                            preguntasNoRespondidas.append("\n• Pregunta ").append(preguntaIndex + 1);
                        } else {
                            respuestas.add(respuesta);
                        }
                    }
                }
            }

            // Validar que todas las preguntas estén respondidas
            if (!todasRespondidas) {
                mostrarAdvertencia("Por favor responde todas las preguntas antes de enviar. Faltan:" +
                        preguntasNoRespondidas.toString());
                return;
            }

            request.setRespuestas(respuestas);

            // Mostrar estado de carga
            mostrarEstado("Enviando respuestas...", Color.BLUE);

            // Enviar al servicio
            ResultadoActividadDTO resultados = estudianteService.resolverActividad(
                    jwtTokenHolder.getUserId(),
                    actividad.getId(),
                    request
            );

            // Mostrar resultados
            mostrarResultados(resultados);

        } catch (Exception e) {
            logger.error("Error al enviar respuestas", e);
            mostrarError("Error al enviar las respuestas: " + e.getMessage());
        }
    }


    private void crearFormularioPreguntas(List<Pregunta> preguntas, VBox contenedor) {
        for (Pregunta pregunta : preguntas) {
            VBox preguntaBox = new VBox(5);
            preguntaBox.setStyle("-fx-border-color: #eee; -fx-border-radius: 5; -fx-padding: 10;");
            preguntaBox.setUserData(pregunta); // Almacenar la pregunta relacionada

            Label lblPregunta = new Label(pregunta.getEnunciado());
            lblPregunta.setStyle("-fx-font-weight: bold;");

            if (pregunta.getTipo() == Pregunta.TipoPregunta.RESPUESTA_ABIERTA) {
                TextArea textArea = new TextArea();
                textArea.setPromptText("Escribe tu respuesta aquí...");
                textArea.setUserData(pregunta); // Relacionar con la pregunta
                preguntaBox.getChildren().addAll(lblPregunta, textArea);
            } else {
                ToggleGroup toggleGroup = new ToggleGroup();
                VBox opcionesBox = new VBox(5);

                for (Opcion opcion : pregunta.getOpciones()) {
                    RadioButton radioButton = new RadioButton(opcion.getTexto());
                    radioButton.setToggleGroup(toggleGroup);
                    radioButton.setUserData(opcion); // Almacenar la opción relacionada
                    opcionesBox.getChildren().add(radioButton);
                }

                preguntaBox.getChildren().addAll(lblPregunta, opcionesBox);
            }

            contenedor.getChildren().add(preguntaBox);
        }
    }

    private void enviarRespuestas(Long actividadId, List<Pregunta> preguntas, VBox formulario) {
        try {
            Long estudianteId = jwtTokenHolder.getUserId();
            ActividadResueltaDTO request = new ActividadResueltaDTO();
            request.setActividadId(actividadId);
            request.setEstudianteId(estudianteId);

            List<RespuestaPreguntaDTO> respuestas = new ArrayList<>();

            // Iterar a través de los nodos hijos del formulario
            for (int i = 0; i < formulario.getChildren().size(); i++) {
                Node node = formulario.getChildren().get(i);
                if (node instanceof VBox) {
                    VBox preguntaBox = (VBox) node;
                    Pregunta pregunta = preguntas.get(i);
                    RespuestaPreguntaDTO respuesta = new RespuestaPreguntaDTO();
                    respuesta.setPreguntaId(pregunta.getId());

                    if (pregunta.getTipo() == Pregunta.TipoPregunta.RESPUESTA_ABIERTA) {
                        // Buscar el TextArea en los hijos del VBox
                        for (Node child : preguntaBox.getChildren()) {
                            if (child instanceof TextArea) {
                                respuesta.setRespuestaAbierta(((TextArea) child).getText());
                                break;
                            }
                        }
                    } else {
                        // Buscar el VBox de opciones y luego los RadioButtons
                        for (Node child : preguntaBox.getChildren()) {
                            if (child instanceof VBox) {
                                VBox opcionesBox = (VBox) child;
                                for (Node opcionNode : opcionesBox.getChildren()) {
                                    if (opcionNode instanceof RadioButton && ((RadioButton) opcionNode).isSelected()) {
                                        String opcionIdStr = opcionNode.getId().replace("opcion_", "");
                                        respuesta.setOpcionId(Long.parseLong(opcionIdStr));
                                        break;
                                    }
                                }
                                break;
                            }
                        }
                    }
                    respuestas.add(respuesta);
                }
            }

            request.setRespuestas(respuestas);

            // Enviar al servicio
            ResultadoActividadDTO resultado = estudianteService.resolverActividad(estudianteId, actividadId, request);

            // Mostrar resultados
            mostrarResultados(resultado);

        } catch (Exception e) {
            logger.error("Error al enviar respuestas", e);
            mostrarAlerta("Error", "No se pudieron enviar las respuestas");
        }
    }

    private void mostrarResultados(ResultadoActividadDTO resultado) {
        VBox contenedor = new VBox(15);
        contenedor.setPadding(new Insets(20));
        contenedor.setStyle("-fx-background-color: #f9f9f9;");

        Label lblTitulo = new Label("Resultados de la actividad");
        lblTitulo.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");

        // Mostrar nota con validación de rango
        double nota = Math.min(resultado.getNota(), 5.0); // Asegurar que no supere 5.0
        Label lblNota = new Label(String.format("Nota: %.1f/5.0", nota));
        lblNota.setStyle("-fx-font-size: 16;");

        // Mostrar resultados por pregunta (sin duplicados)
        VBox resultadosBox = new VBox(10);
        resultado.getResultadosPreguntas().forEach(resPregunta -> {
            VBox resultadoBox = new VBox(5);
            resultadoBox.setStyle("-fx-border-color: #eee; -fx-border-radius: 5; -fx-padding: 10; " +
                    (resPregunta.isEsCorrecta() ? "-fx-background-color: #e8f8f5;" : "-fx-background-color: #fdedec;"));

            Label lblPregunta = new Label("Pregunta ID: " + resPregunta.getPreguntaId());
            lblPregunta.setStyle("-fx-font-weight: bold;");

            Label lblCorrecta = new Label(resPregunta.isEsCorrecta() ? "✓ Correcta" : "✗ Incorrecta");
            lblCorrecta.setTextFill(resPregunta.isEsCorrecta() ? Color.GREEN : Color.RED);

            Label lblRetro = new Label(resPregunta.getRetroalimentacion());
            lblRetro.setWrapText(true);

            resultadoBox.getChildren().addAll(lblPregunta, lblCorrecta, lblRetro);
            resultadosBox.getChildren().add(resultadoBox);
        });

        Button btnVolver = new Button("Volver a actividades");
        btnVolver.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
        btnVolver.setOnAction(e -> mostrarActividades());

        contenedor.getChildren().addAll(lblTitulo, lblNota, resultadosBox, btnVolver);

        ScrollPane scrollPane = new ScrollPane(contenedor);
        scrollPane.setFitToWidth(true);
        contentPane.getChildren().setAll(scrollPane);
    }


    private void refrescarDatos() {
        mostrarEstado("Actualizando datos...", Color.BLUE);

        if (btnMaterias.getStyle().contains("-fx-background-color")) {
            mostrarMaterias();
        } else if (btnActividades.getStyle().contains("-fx-background-color")) {
            mostrarActividades();
//        } else if (btnNotificaciones.getStyle().contains("-fx-background-color")) {
//            mostrarNotificaciones();
}

        mostrarEstado("Datos actualizados", Color.GREEN);
    }

    protected void mostrarEstado(String mensaje, Color color) {
        if (lblEstado != null) {
            lblEstado.setText(mensaje);
            lblEstado.setTextFill(color);
        }
    }
    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarAdvertencia(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Advertencia");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
    private void cerrarSesion() {
        try {
            jwtTokenHolder.clearToken();
            mostrarEstado("Sesión cerrada", Color.GREEN);
        } catch (Exception e) {
            logger.error("Error al cerrar sesión", e);
            mostrarAlerta("Error", "No se pudo cerrar la sesión");
        }
    }
}