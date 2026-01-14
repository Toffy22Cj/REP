// EstudianteController.java
package com.rep.controller.views;

import com.rep.config.SpringFXMLLoader;
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
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import javafx.stage.FileChooser;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.io.FileInputStream;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Component
public class EstudianteController extends BaseTokenController {
    private static final Logger logger = LoggerFactory.getLogger(EstudianteController.class);
    private final ActividadService actividadService;
    private final EstudianteService estudianteService;
    private final EstudianteApiService estudianteApiService;
    @FXML
    private Button btnResolverActividad;
    @FXML
    private Label lblNombreEstudiante;
    @FXML
    private Label lblEstado;
    @FXML
    private Button btnMaterias;
    @FXML
    private Button btnActividades;
    @FXML
    private Button btnNotificaciones;
    @FXML
    private Button btnRefrescar;
    @FXML
    private Button btnCerrarSesion;
    @FXML
    private VBox mainContainer;
    @FXML
    private StackPane contentPane;
    private final SpringFXMLLoader springFXMLLoader;

    @Autowired
    public EstudianteController(EstudianteService estudianteService, SpringFXMLLoader springFXMLLoader,
            EstudianteApiService estudianteApiService,
            JwtTokenHolder jwtTokenHolder, ActividadService actividadService) {
        this.estudianteService = estudianteService;
        this.springFXMLLoader = springFXMLLoader;
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
        // btnResolverActividad.setOnAction(e -> mostrarResolucionActividad());
        btnMaterias.setOnAction(e -> mostrarMaterias());
        btnActividades.setOnAction(e -> mostrarActividades());
        // btnNotificaciones.setOnAction(e -> mostrarNotificaciones());
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

        Label lblFecha = new Label(
                "Entrega: " + actividad.getFechaEntrega().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
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

    // private void mostrarNotificaciones() {
    // try {
    // Long estudianteId = jwtTokenHolder.getUserId();
    // List<Notificacion> notificaciones =
    // estudianteService.getNotificacionesByEstudiante(estudianteId, false);
    // notificaciones.addAll(estudianteService.getNotificacionesByEstudiante(estudianteId,
    // true));
    //
    // VBox contenedorNotificaciones = new VBox(10);
    // contenedorNotificaciones.setPadding(new Insets(15));
    //
    // if (notificaciones.isEmpty()) {
    // contenedorNotificaciones.getChildren().add(new Label("No hay
    // notificaciones"));
    // } else {
    // for (Notificacion notificacion : notificaciones) {
    // VBox card = new VBox(5);
    // card.setStyle("-fx-border-color: #ddd; -fx-border-radius: 5; -fx-padding: 10;
    // " +
    // (notificacion.isLeida() ? "" : "-fx-background-color: #f0f8ff;"));
    //
    // if (notificacion.getActividad() != null) {
    // Label lblActividad = new Label("Actividad: " +
    // notificacion.getActividad().getTitulo());
    // lblActividad.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");
    // card.getChildren().add(lblActividad);
    // }
    //
    // Label lblMensaje = new Label(notificacion.getMensaje());
    // lblMensaje.setWrapText(true);
    //
    // Label lblFecha = new Label(notificacion.getFechaCreacion()
    // .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
    // lblFecha.setStyle("-fx-text-fill: #7f8c8d;");
    //
    // if (!notificacion.isLeida()) {
    // Button btnMarcar = new Button("Marcar como leída");
    // btnMarcar.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
    // btnMarcar.setOnAction(e -> {
    // estudianteService.marcarNotificacionComoLeida(notificacion.getId());
    // card.setStyle("-fx-border-color: #ddd; -fx-border-radius: 5; -fx-padding:
    // 10;");
    // btnMarcar.setDisable(true);
    // });
    // card.getChildren().addAll(lblMensaje, lblFecha, btnMarcar);
    // } else {
    // card.getChildren().addAll(lblMensaje, lblFecha);
    // }
    //
    // contenedorNotificaciones.getChildren().add(card);
    // }
    // }
    //
    // ScrollPane scrollPane = new ScrollPane(contenedorNotificaciones);
    // scrollPane.setFitToWidth(true);
    // contentPane.getChildren().setAll(scrollPane);
    // } catch (Exception e) {
    // logger.error("Error al cargar notificaciones", e);
    // mostrarAlerta("Error", "No se pudieron cargar las notificaciones");
    // }
    // }
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

            // **AGREGAR: Mostrar archivo adjunto del profesor SI EXISTE**
            if (pregunta.isArchivoDisponible() && pregunta.getNombreArchivo() != null 
                    && !pregunta.getNombreArchivo().isEmpty()) {

                mostrarVistaPreviaArchivo(pregunta, contenidoPregunta);
            }

            if (pregunta.getTipo() == Pregunta.TipoPregunta.RESPUESTA_ABIERTA) {
                TextArea respuesta = new TextArea();
                respuesta.setPromptText("Escribe tu respuesta aquí...");
                respuesta.setPrefRowCount(3);
                respuesta.setWrapText(true);

                // Botón para que el estudiante adjunte su archivo
                Button btnAdjuntar = new Button("Adjuntar Archivo");
                btnAdjuntar.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 5 10;");
                Label lblArchivoEstudiante = new Label("");
                lblArchivoEstudiante.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");

                btnAdjuntar.setOnAction(e -> {
                    FileChooser fileChooser = new FileChooser();
                    fileChooser.setTitle("Seleccionar Archivo para adjuntar a tu respuesta");
                    fileChooser.getExtensionFilters().addAll(
                            new FileChooser.ExtensionFilter("Documentos", "*.pdf", "*.doc", "*.docx", "*.txt"),
                            new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg", "*.gif"),
                            new FileChooser.ExtensionFilter("Todos los archivos", "*.*"));
                    File archivo = fileChooser.showOpenDialog(contentPane.getScene().getWindow());
                    if (archivo != null) {
                        btnAdjuntar.setUserData(archivo);
                        lblArchivoEstudiante.setText("📎 Adjunto: " + archivo.getName());
                        lblArchivoEstudiante.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 12px;");
                    }
                });

                HBox archivoEstudianteBox = new HBox(10, btnAdjuntar, lblArchivoEstudiante);
                archivoEstudianteBox.setAlignment(Pos.CENTER_LEFT);
                archivoEstudianteBox.setPadding(new Insets(5, 0, 0, 0));

                contenidoPregunta.getChildren().addAll(respuesta, archivoEstudianteBox);
            } else {
                ToggleGroup grupoOpciones = new ToggleGroup();
                if (pregunta.getOpciones() != null && !pregunta.getOpciones().isEmpty()) {
                    for (OpcionDTO opcion : pregunta.getOpciones()) {
                        RadioButton rb = new RadioButton(opcion.getTexto());
                        rb.setToggleGroup(grupoOpciones);
                        rb.setUserData(opcion.getId());
                        rb.setWrapText(true);
                        HBox opcionRow = new HBox(8);
                        opcionRow.setAlignment(Pos.CENTER_LEFT);
                        opcionRow.getChildren().add(rb);

                        // Si la opción tiene archivo, mostrar botón/ver miniatura
                        if (opcion.isArchivoDisponible()) {
                            Button btnVerOpt = new Button("Ver");
                            btnVerOpt.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 11px;");
                            btnVerOpt.setOnAction(e -> descargarArchivoOpcion(opcion.getId(), opcion.getNombreArchivo()));
                            opcionRow.getChildren().add(btnVerOpt);
                        }

                        contenidoPregunta.getChildren().add(opcionRow);
                    }
                } else {
                    Label sinOpciones = new Label("Esta pregunta no tiene opciones definidas");
                    sinOpciones.setStyle("-fx-text-fill: #999; -fx-font-style: italic;");
                    contenidoPregunta.getChildren().add(sinOpciones);
                }
            }

            preguntaBox.getChildren().addAll(enunciado, contenidoPregunta);
            formularioContainer.getChildren().add(preguntaBox);
        }

        // Botón de enviar
        Button btnEnviar = new Button("Enviar respuestas");
        btnEnviar.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");
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

    private void descargarArchivoPregunta(Long preguntaId, String nombreArchivo) {
        new Thread(() -> {
            final long MAX_BYTES = 20L * 1024L * 1024L; // 20 MB
            final long AUTO_OPEN_LIMIT = 5L * 1024L * 1024L; // 5 MB

            Platform.runLater(() -> mostrarEstado("Descargando archivo...", Color.BLUE));

            try {
                org.springframework.web.client.RestTemplate restTemplate = crearRestTemplateConTimeout();
                org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                headers.set("Authorization", "Bearer " + obtenerToken());
                final String url = "http://localhost:8080/api/preguntas/" + preguntaId + "/archivo";

                // Directorio de descargas
                String downloadDir = System.getProperty("user.home") + "/Downloads/archivos_clase/";
                java.nio.file.Path dirPath = java.nio.file.Paths.get(downloadDir);
                if (!java.nio.file.Files.exists(dirPath)) {
                    java.nio.file.Files.createDirectories(dirPath);
                }
                java.nio.file.Path filePath = dirPath.resolve(nombreArchivo);

                org.springframework.web.client.RequestCallback requestCallback = request -> {
                    request.getHeaders().addAll(headers);
                };

                org.springframework.web.client.ResponseExtractor<Void> responseExtractor = response -> {
                    try (java.io.InputStream is = response.getBody();
                         java.io.OutputStream os = java.nio.file.Files.newOutputStream(filePath, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING)) {

                        byte[] buffer = new byte[8192];
                        int read;
                        long total = 0;
                        while ((read = is.read(buffer)) != -1) {
                            total += read;
                            if (total > MAX_BYTES) {
                                try { java.nio.file.Files.deleteIfExists(filePath); } catch (Exception ignored) {}
                                throw new RuntimeException("Archivo supera el tamaño máximo permitido de 20 MB");
                            }
                            os.write(buffer, 0, read);
                        }
                        os.flush();
                    }
                    return null;
                };

                try {
                    restTemplate.execute(url, org.springframework.http.HttpMethod.GET, requestCallback, responseExtractor);

                    long size = java.nio.file.Files.size(filePath);

                    Platform.runLater(() -> {
                        try {
                            if (size > AUTO_OPEN_LIMIT) {
                                mostrarAlerta("Archivo descargado",
                                        "El archivo se ha descargado en: " + filePath.toString()
                                                + "\n(Archivo grande: " + (size / 1024 / 1024) + " MB). Ábrelo manualmente.");
                                mostrarEstado("Archivo descargado (archivo grande)", Color.GREEN);
                                return;
                            }

                            abrirArchivoSegunTipo(filePath.toFile(), nombreArchivo);
                            mostrarEstado("Archivo descargado", Color.GREEN);
                        } catch (Exception ex) {
                            logger.error("Error al procesar archivo descargado", ex);
                            mostrarAlerta("Archivo descargado",
                                    "Archivo guardado en: " + filePath.toString());
                        }
                    });
                } catch (Exception streamEx) {
                    logger.error("Error streaming archivo de pregunta", streamEx);
                    Platform.runLater(() -> {
                        mostrarError("Error al descargar archivo: " + streamEx.getMessage());
                        mostrarEstado("Error", Color.RED);
                    });
                }
            } catch (Exception ex) {
                logger.error("Error descargando archivo de pregunta", ex);
                Platform.runLater(() -> {
                    mostrarError("Error al descargar archivo: " + ex.getMessage());
                    mostrarEstado("Error", Color.RED);
                });
            }
        }, "descarga-archivo-thread").start();
    }

    private void mostrarVistaPreviaArchivo(PreguntaConOpcionesDTO pregunta, VBox contenedor) {
        String nombreArchivo = pregunta.getNombreArchivo().toLowerCase();
        VBox archivoBox = new VBox(8);
        archivoBox.setPadding(new Insets(10));
        archivoBox.setStyle("-fx-background-color: #f0f8ff; -fx-border-color: #d6eaf8; -fx-border-radius: 5;");
        
        Label lblTitulo = new Label("📎 Archivo adjunto del profesor:");
        lblTitulo.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #2c3e50;");
        
        if (nombreArchivo.endsWith(".png") || nombreArchivo.endsWith(".jpg") || 
            nombreArchivo.endsWith(".jpeg") || nombreArchivo.endsWith(".gif") || 
            nombreArchivo.endsWith(".bmp")) {
            HBox imagenBox = new HBox(10);
            imagenBox.setAlignment(Pos.CENTER_LEFT);
            
            Label lblTipo = new Label("🖼️ Imagen: ");
            lblTipo.setStyle("-fx-font-size: 12px;");
            
            Button btnVerImagen = new Button("Ver imagen completa");
            btnVerImagen.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 11px;");
            btnVerImagen.setOnAction(e -> descargarYMostrarImagen(pregunta.getId(), pregunta.getNombreArchivo()));
            
            Label miniatura = new Label("[Vista previa de imagen]");
            miniatura.setStyle("-fx-padding: 5; -fx-background-color: #e8f4f8; -fx-border-color: #3498db;");
            
            imagenBox.getChildren().addAll(lblTipo, btnVerImagen);
            archivoBox.getChildren().addAll(lblTitulo, imagenBox);
        } else if (nombreArchivo.endsWith(".pdf")) {
            HBox pdfBox = new HBox(10);
            pdfBox.setAlignment(Pos.CENTER_LEFT);
            
            Label lblTipo = new Label("📄 Documento PDF: " + pregunta.getNombreArchivo());
            lblTipo.setStyle("-fx-font-size: 12px;");
            
            Button btnVerPdf = new Button("Abrir PDF");
            btnVerPdf.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 11px;");
            btnVerPdf.setOnAction(e -> descargarArchivoPregunta(pregunta.getId(), pregunta.getNombreArchivo()));
            
            Label iconoPdf = new Label("📄");
            iconoPdf.setStyle("-fx-font-size: 24px; -fx-padding: 0 10 0 0;");
            
            pdfBox.getChildren().addAll(iconoPdf, lblTipo, btnVerPdf);
            archivoBox.getChildren().addAll(lblTitulo, pdfBox);
        } else if (nombreArchivo.endsWith(".doc") || nombreArchivo.endsWith(".docx")) {
            HBox wordBox = new HBox(10);
            wordBox.setAlignment(Pos.CENTER_LEFT);
            
            Label lblTipo = new Label("📝 Documento Word: " + pregunta.getNombreArchivo());
            lblTipo.setStyle("-fx-font-size: 12px;");
            
            Button btnVerWord = new Button("Descargar");
            btnVerWord.setStyle("-fx-background-color: #2c3e50; -fx-text-fill: white; -fx-font-size: 11px;");
            btnVerWord.setOnAction(e -> descargarArchivoPregunta(pregunta.getId(), pregunta.getNombreArchivo()));
            
            Label iconoWord = new Label("📝");
            iconoWord.setStyle("-fx-font-size: 24px; -fx-padding: 0 10 0 0;");
            
            wordBox.getChildren().addAll(iconoWord, lblTipo, btnVerWord);
            archivoBox.getChildren().addAll(lblTitulo, wordBox);
        } else {
            HBox otroBox = new HBox(10);
            otroBox.setAlignment(Pos.CENTER_LEFT);
            
            Label lblTipo = new Label("📎 Archivo: " + pregunta.getNombreArchivo());
            lblTipo.setStyle("-fx-font-size: 12px;");
            
            Button btnDescargar = new Button("Descargar");
            btnDescargar.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-size: 11px;");
            btnDescargar.setOnAction(e -> descargarArchivoPregunta(pregunta.getId(), pregunta.getNombreArchivo()));
            
            Label iconoGen = new Label("📎");
            iconoGen.setStyle("-fx-font-size: 24px; -fx-padding: 0 10 0 0;");
            
            otroBox.getChildren().addAll(iconoGen, lblTipo, btnDescargar);
            archivoBox.getChildren().addAll(lblTitulo, otroBox);
        }
        
        contenedor.getChildren().add(archivoBox);
    }

    private void descargarYMostrarImagen(Long preguntaId, String nombreArchivo) {
        new Thread(() -> {
            try {
                Platform.runLater(() -> mostrarEstado("Cargando imagen...", Color.BLUE));
                
                org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
                org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                headers.set("Authorization", "Bearer " + obtenerToken());
                org.springframework.http.HttpEntity<Void> request = new org.springframework.http.HttpEntity<>(headers);
                
                org.springframework.http.ResponseEntity<byte[]> response = restTemplate.exchange(
                        "http://localhost:8080/api/preguntas/" + preguntaId + "/archivo",
                        org.springframework.http.HttpMethod.GET,
                        request,
                        byte[].class);
                
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("imagen_", "_" + nombreArchivo);
                    java.nio.file.Files.write(tempFile, response.getBody());
                    
                    Platform.runLater(() -> {
                        try {
                            Dialog<Void> dialog = new Dialog<>();
                            dialog.setTitle("Imagen: " + nombreArchivo);
                            
                            javafx.scene.image.Image image = new javafx.scene.image.Image(
                                new java.io.FileInputStream(tempFile.toFile()));
                            javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView(image);
                            
                            if (image.getWidth() > 800 || image.getHeight() > 600) {
                                imageView.setFitWidth(800);
                                imageView.setFitHeight(600);
                                imageView.setPreserveRatio(true);
                            }
                            imageView.setSmooth(true);
                            
                            ButtonType btnDescargar = new ButtonType("Guardar como...", ButtonBar.ButtonData.OK_DONE);
                            ButtonType btnCerrar = new ButtonType("Cerrar", ButtonBar.ButtonData.CANCEL_CLOSE);
                            dialog.getDialogPane().getButtonTypes().addAll(btnDescargar, btnCerrar);
                            
                            VBox content = new VBox(10);
                            content.setPadding(new Insets(10));
                            content.getChildren().add(imageView);
                            dialog.getDialogPane().setContent(content);
                            
                            dialog.setResultConverter(buttonType -> {
                                if (buttonType == btnDescargar) {
                                    FileChooser fileChooser = new FileChooser();
                                    fileChooser.setTitle("Guardar imagen como");
                                    fileChooser.setInitialFileName(nombreArchivo);
                                    fileChooser.getExtensionFilters().addAll(
                                        new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg"),
                                        new FileChooser.ExtensionFilter("Todos los archivos", "*.*"));
                                    
                                    File destino = fileChooser.showSaveDialog(contentPane.getScene().getWindow());
                                    if (destino != null) {
                                        try {
                                            java.nio.file.Files.copy(tempFile, destino.toPath(), 
                                                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                                            mostrarAlerta("Imagen guardada", "La imagen se guardó en: " + destino.getAbsolutePath());
                                        } catch (Exception ex) {
                                            mostrarError("Error al guardar imagen: " + ex.getMessage());
                                        }
                                    }
                                }
                                return null;
                            });
                            
                            dialog.showAndWait();
                        } catch (Exception ex) {
                            logger.error("Error al mostrar imagen", ex);
                            descargarArchivoPregunta(preguntaId, nombreArchivo);
                        }
                    });
                    Platform.runLater(() -> mostrarEstado("Imagen cargada", Color.GREEN));
                }
            } catch (Exception ex) {
                logger.error("Error al cargar imagen", ex);
                Platform.runLater(() -> {
                    mostrarError("Error al cargar imagen: " + ex.getMessage());
                    mostrarEstado("Error", Color.RED);
                });
            }
        }, "cargar-imagen-thread").start();
    }

    // Abre o muestra archivo según su tipo (imágenes --> diálogo, otros --> programa externo)
    private void abrirArchivoSegunTipo(File archivo, String nombreArchivo) {
        try {
            String nombreLower = nombreArchivo.toLowerCase();

            if (nombreLower.matches(".*\\.(png|jpg|jpeg|gif|bmp)$")) {
                // Para imágenes, mostrar en diálogo de JavaFX
                mostrarImagenEnDialogo(archivo, nombreArchivo);
            } else if (nombreLower.endsWith(".pdf")) {
                // Para PDFs, abrir con programa externo
                abrirConProgramaExterno(archivo);
            } else if (nombreLower.matches(".*\\.(doc|docx)$")) {
                // Para documentos Word
                abrirConProgramaExterno(archivo);
            } else if (nombreLower.matches(".*\\.(txt|rtf)$")) {
                // Para archivos de texto
                abrirConProgramaExterno(archivo);
            } else {
                // Para otros tipos, preguntar
                Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
                confirmDialog.setTitle("Abrir archivo");
                confirmDialog.setHeaderText("Archivo: " + nombreArchivo);
                confirmDialog.setContentText("¿Desea abrir este archivo con el programa predeterminado del sistema?");

                Optional<ButtonType> result = confirmDialog.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    abrirConProgramaExterno(archivo);
                } else {
                    mostrarAlerta("Archivo descargado", "Archivo guardado en: " + archivo.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            logger.error("Error abriendo archivo", e);
            mostrarAlerta("Archivo descargado", "Archivo guardado en: " + archivo.getAbsolutePath() +
                    "\nError al abrir: " + e.getMessage());
        }
    }

    private void mostrarImagenEnDialogo(File archivoImagen, String nombreArchivo) {
        try {
            javafx.scene.image.Image image = new javafx.scene.image.Image(
                    new java.io.FileInputStream(archivoImagen));

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Imagen: " + nombreArchivo);

            javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView(image);

            double maxWidth = 800;
            double maxHeight = 600;

            if (image.getWidth() > maxWidth || image.getHeight() > maxHeight) {
                imageView.setFitWidth(maxWidth);
                imageView.setFitHeight(maxHeight);
                imageView.setPreserveRatio(true);
            }

            imageView.setSmooth(true);

            ScrollPane scrollPane = new ScrollPane(imageView);
            scrollPane.setFitToWidth(true);
            scrollPane.setFitToHeight(true);
            scrollPane.setPrefViewportWidth(maxWidth);
            scrollPane.setPrefViewportHeight(maxHeight);

            VBox content = new VBox(10);
            content.setPadding(new Insets(10));
            content.getChildren().add(scrollPane);

            ButtonType btnDescargar = new ButtonType("Guardar como...", ButtonBar.ButtonData.OK_DONE);
            ButtonType btnCerrar = new ButtonType("Cerrar", ButtonBar.ButtonData.CANCEL_CLOSE);
            dialog.getDialogPane().getButtonTypes().addAll(btnDescargar, btnCerrar);

            dialog.getDialogPane().setContent(content);

            dialog.setResultConverter(buttonType -> {
                if (buttonType == btnDescargar) {
                    FileChooser fileChooser = new FileChooser();
                    fileChooser.setTitle("Guardar imagen como");
                    fileChooser.setInitialFileName(nombreArchivo);
                    fileChooser.getExtensionFilters().addAll(
                            new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"),
                            new FileChooser.ExtensionFilter("Todos los archivos", "*.*"));

                    File destino = fileChooser.showSaveDialog(contentPane.getScene().getWindow());
                    if (destino != null) {
                        try {
                            java.nio.file.Files.copy(
                                    archivoImagen.toPath(),
                                    destino.toPath(),
                                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                            mostrarAlerta("Imagen guardada", "La imagen se guardó en: " + destino.getAbsolutePath());
                        } catch (Exception ex) {
                            mostrarError("Error al guardar imagen: " + ex.getMessage());
                        }
                    }
                }
                return null;
            });

            dialog.showAndWait();

        } catch (Exception e) {
            logger.error("Error mostrando imagen", e);
            // Si falla mostrar imagen, intentar abrir con programa externo
            abrirConProgramaExterno(archivoImagen);
        }
    }

    private void abrirConProgramaExterno(File archivo) {
        new Thread(() -> {
            try {
                if (!archivo.exists() || !archivo.canRead()) {
                    Platform.runLater(() -> mostrarError("El archivo no existe o no se puede leer: " + archivo.getAbsolutePath()));
                    return;
                }

                if (java.awt.Desktop.isDesktopSupported()) {
                    java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                    if (desktop.isSupported(java.awt.Desktop.Action.OPEN)) {
                        desktop.open(archivo);
                        Platform.runLater(() -> mostrarAlerta("Archivo abierto", "El archivo se abrió con el programa predeterminado: " + archivo.getName()));
                        return;
                    }
                }

                String os = System.getProperty("os.name").toLowerCase();
                String[] command;

                if (os.contains("win")) {
                    command = new String[] {"cmd", "/c", "start", "\"\"", archivo.getAbsolutePath()};
                } else if (os.contains("mac")) {
                    command = new String[] {"open", archivo.getAbsolutePath()};
                } else {
                    command = new String[] {"xdg-open", archivo.getAbsolutePath()};
                }

                ProcessBuilder pb = new ProcessBuilder(command);
                pb.redirectErrorStream(true);
                Process process = pb.start();

                try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        logger.debug("Proceso ejecutado: " + line);
                    }
                }

                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    Platform.runLater(() -> mostrarAlerta("Archivo abierto", "Comando ejecutado exitosamente para abrir: " + archivo.getName()));
                } else {
                    throw new RuntimeException("Comando falló con código: " + exitCode);
                }

            } catch (Exception e) {
                logger.error("Error al abrir archivo con programa externo", e);
                Platform.runLater(() -> mostrarAlerta("Archivo descargado", "Archivo guardado en: " + archivo.getAbsolutePath() + "\nNo se pudo abrir automáticamente. Puede abrirlo manualmente." + "\nError: " + e.getMessage()));
            }
        }, "abrir-archivo-externo").start();
    }

    private org.springframework.web.client.RestTemplate crearRestTemplateConTimeout() {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000);
        factory.setReadTimeout(60000);
        return new org.springframework.web.client.RestTemplate(factory);
    }

    // Descargar y guardar sin abrir
    private void descargarArchivoOpcionSinAbrir(Long opcionId, String nombreArchivo) {
        new Thread(() -> {
            try {
                Platform.runLater(() -> mostrarEstado("Descargando archivo...", Color.BLUE));

                org.springframework.web.client.RestTemplate restTemplate = crearRestTemplateConTimeout();
                org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                headers.set("Authorization", "Bearer " + obtenerToken());
                org.springframework.http.HttpEntity<Void> request = new org.springframework.http.HttpEntity<>(headers);

                org.springframework.http.ResponseEntity<byte[]> response = restTemplate.exchange(
                    "http://localhost:8080/api/preguntas/opciones/" + opcionId + "/archivo",
                        org.springframework.http.HttpMethod.GET,
                        request,
                        byte[].class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    Platform.runLater(() -> {
                        FileChooser fileChooser = new FileChooser();
                        fileChooser.setTitle("Guardar archivo");
                        fileChooser.setInitialFileName(nombreArchivo);
                        File destino = fileChooser.showSaveDialog(contentPane.getScene().getWindow());
                        if (destino != null) {
                            try {
                                java.nio.file.Files.write(destino.toPath(), response.getBody(), java.nio.file.StandardOpenOption.CREATE);
                                mostrarAlerta("Archivo guardado", "Archivo guardado en: " + destino.getAbsolutePath());
                                mostrarEstado("Archivo descargado", Color.GREEN);
                            } catch (Exception ex) {
                                mostrarError("Error al guardar archivo: " + ex.getMessage());
                            }
                        }
                    });
                }
            } catch (Exception ex) {
                logger.error("Error descargando archivo", ex);
                Platform.runLater(() -> {
                    mostrarError("Error al descargar archivo: " + ex.getMessage());
                    mostrarEstado("Error", Color.RED);
                });
            }
        }, "descarga-archivo-solo").start();
    }

    private void descargarArchivoOpcion(Long opcionId, String nombreArchivo) {
        // Descarga por streaming para no cargar todo en memoria y con límites de tamaño
        new Thread(() -> {
            final long MAX_DOWNLOAD = 20L * 1024L * 1024L; // 20 MB
            final long AUTO_OPEN_LIMIT = 5L * 1024L * 1024L; // 5 MB
            Platform.runLater(() -> mostrarEstado("Descargando archivo de opción...", Color.BLUE));

            try {
                org.springframework.web.client.RestTemplate restTemplate = crearRestTemplateConTimeout();

                org.springframework.web.client.RequestCallback requestCallback = request -> {
                    request.getHeaders().add("Authorization", "Bearer " + obtenerToken());
                };

                java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("opcion_", "_" + nombreArchivo);

                org.springframework.web.client.ResponseExtractor<Void> extractor = response -> {
                    try (java.io.InputStream is = response.getBody();
                         java.io.OutputStream os = java.nio.file.Files.newOutputStream(tempFile, java.nio.file.StandardOpenOption.WRITE)) {
                        byte[] buffer = new byte[8192];
                        int read;
                        long total = 0;
                        while ((read = is.read(buffer)) != -1) {
                            os.write(buffer, 0, read);
                            total += read;
                            if (total > MAX_DOWNLOAD) {
                                os.close();
                                java.nio.file.Files.deleteIfExists(tempFile);
                                throw new RuntimeException("El archivo excede el límite máximo de descarga de " + (MAX_DOWNLOAD / (1024*1024)) + " MB");
                            }
                        }
                        os.flush();
                    }
                    return null;
                };

                restTemplate.execute("http://localhost:8080/api/preguntas/opciones/" + opcionId + "/archivo",
                        org.springframework.http.HttpMethod.GET,
                        requestCallback,
                        extractor);

                long size = java.nio.file.Files.size(tempFile);

                Platform.runLater(() -> {
                    try {
                        if ((nombreArchivo.toLowerCase().endsWith(".png") || nombreArchivo.toLowerCase().endsWith(".jpg") ||
                                nombreArchivo.toLowerCase().endsWith(".jpeg") || nombreArchivo.toLowerCase().endsWith(".gif")) && size <= MAX_DOWNLOAD) {
                            // Mostrar imagen en diálogo
                            try {
                                mostrarImagenEnDialogo(tempFile.toFile(), nombreArchivo);
                            } catch (Exception ex) {
                                logger.error("Error mostrando imagen de opción", ex);
                                mostrarAlerta("Archivo descargado", "Archivo guardado en: " + tempFile.toString());
                            }
                        } else {
                            // No abrir automáticamente archivos grandes
                            if (size <= AUTO_OPEN_LIMIT) {
                                abrirConProgramaExterno(tempFile.toFile());
                            } else {
                                mostrarAlerta("Archivo descargado", "Archivo guardado en: " + tempFile.toString());
                            }
                        }
                        mostrarEstado("Archivo descargado", Color.GREEN);
                    } catch (Exception ex) {
                        logger.error("Error al procesar archivo de opción", ex);
                        mostrarAlerta("Error", "El archivo fue descargado parcialmente o no se pudo procesar.");
                    }
                });

            } catch (Exception ex) {
                logger.error("Error descargando archivo de opción", ex);
                Platform.runLater(() -> mostrarError("Error al descargar archivo de opción: " + ex.getMessage()));
            }
        }, "descargar-opcion-thread").start();
    }

    private void mostrarVistaPreviaArchivoLocal(File archivo, VBox contenedor) {
        for (Node node : new ArrayList<>(contenedor.getChildren())) {
            if (node instanceof VBox && ((VBox) node).getProperties().containsKey("archivo-estudiante")) {
                contenedor.getChildren().remove(node);
                break;
            }
        }
        
        String nombreArchivo = archivo.getName().toLowerCase();
        VBox archivoBox = new VBox(8);
        archivoBox.setPadding(new Insets(10));
        archivoBox.setStyle("-fx-background-color: #f0fff0; -fx-border-color: #d4efdf; -fx-border-radius: 5; " +
                           "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        archivoBox.getProperties().put("archivo-estudiante", true);
        
        Label lblTitulo = new Label("📎 Tu archivo adjunto:");
        lblTitulo.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #27ae60;");
        
        if (nombreArchivo.endsWith(".png") || nombreArchivo.endsWith(".jpg") || 
            nombreArchivo.endsWith(".jpeg") || nombreArchivo.endsWith(".gif")) {
            try {
                javafx.scene.image.Image image = new javafx.scene.image.Image(new FileInputStream(archivo));
                javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView(image);
                imageView.setFitWidth(100);
                imageView.setFitHeight(100);
                imageView.setPreserveRatio(true);
                imageView.setSmooth(true);
                imageView.setOnMouseClicked(e -> {
                    Dialog<Void> dialog = new Dialog<>();
                    dialog.setTitle("Vista previa: " + archivo.getName());
                    javafx.scene.image.ImageView fullView = new javafx.scene.image.ImageView(image);
                    if (image.getWidth() > 800 || image.getHeight() > 600) {
                        fullView.setFitWidth(800);
                        fullView.setFitHeight(600);
                        fullView.setPreserveRatio(true);
                    }
                    VBox content = new VBox(10, fullView);
                    content.setPadding(new Insets(10));
                    dialog.getDialogPane().setContent(content);
                    dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
                    dialog.showAndWait();
                });
                HBox imagenBox = new HBox(10, imageView);
                imagenBox.setAlignment(Pos.CENTER_LEFT);
                archivoBox.getChildren().addAll(lblTitulo, imagenBox);
            } catch (Exception e) {
                Label lblInfo = new Label("🖼️ Imagen: " + archivo.getName());
                lblInfo.setStyle("-fx-font-size: 12px;");
                archivoBox.getChildren().addAll(lblTitulo, lblInfo);
            }
        } else {
            String icono = "📎";
            if (nombreArchivo.endsWith(".pdf")) icono = "📄";
            else if (nombreArchivo.endsWith(".doc") || nombreArchivo.endsWith(".docx")) icono = "📝";
            else if (nombreArchivo.endsWith(".txt")) icono = "📋";
            
            Label lblInfo = new Label(icono + " " + archivo.getName());
            lblInfo.setStyle("-fx-font-size: 12px;");
            long tamañoBytes = archivo.length();
            String tamaño = "";
            if (tamañoBytes < 1024) tamaño = tamañoBytes + " B";
            else if (tamañoBytes < 1024 * 1024) tamaño = String.format("%.1f KB", tamañoBytes / 1024.0);
            else tamaño = String.format("%.1f MB", tamañoBytes / (1024.0 * 1024.0));
            Label lblTamaño = new Label("Tamaño: " + tamaño);
            lblTamaño.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");
            archivoBox.getChildren().addAll(lblTitulo, lblInfo, lblTamaño);
        }
        
        int insertIndex = contenedor.getChildren().size();
        for (int i = 0; i < contenedor.getChildren().size(); i++) {
            if (contenedor.getChildren().get(i) instanceof TextArea) {
                insertIndex = i + 1;
                break;
            }
        }
        contenedor.getChildren().add(insertIndex, archivoBox);
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

                        VBox contenidoPregunta = (VBox) preguntaBox.getChildren().get(1); // El segundo nodo es el
                                                                                          // contenedor de respuesta
                        boolean respondida = false;

                        if (pregunta.getTipo() == Pregunta.TipoPregunta.RESPUESTA_ABIERTA) {
                            TextArea textArea = (TextArea) contenidoPregunta.getChildren().get(0);
                            if (textArea.getText() != null && !textArea.getText().trim().isEmpty()) {
                                respuesta.setRespuestaAbierta(textArea.getText());
                                respondida = true;
                            }

                            // Verificar si hay archivo adjunto
                            if (contenidoPregunta.getChildren().size() > 1) {
                                HBox archivoBox = (HBox) contenidoPregunta.getChildren().get(1);
                                if (archivoBox.getChildren().size() > 0) {
                                    Button btnAdjuntar = (Button) archivoBox.getChildren().get(0);
                                    File archivoAdjunto = (File) btnAdjuntar.getUserData();

                                    if (archivoAdjunto != null) {
                                        try {
                                            // Crear directorio si no existe
                                            String uploadDir = "uploads/respuestas/";
                                            java.nio.file.Path uploadPath = java.nio.file.Paths.get(uploadDir);
                                            if (!java.nio.file.Files.exists(uploadPath)) {
                                                java.nio.file.Files.createDirectories(uploadPath);
                                            }

                                            // Copiar archivo al directorio de uploads con un nombre único
                                            String nombreUnico = System.currentTimeMillis() + "_"
                                                    + archivoAdjunto.getName();
                                            java.nio.file.Path destino = uploadPath.resolve(nombreUnico);
                                            java.nio.file.Files.copy(archivoAdjunto.toPath(), destino,
                                                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                                            respuesta.setArchivoAdjunto(destino.toString());
                                            respuesta.setNombreArchivo(archivoAdjunto.getName());

                                        } catch (Exception e) {
                                            logger.error("Error al copiar archivo adjunto", e);
                                            mostrarError("Error al adjuntar archivo: " + e.getMessage());
                                        }
                                    }
                                }
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
                    request);

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
            // } else if (btnNotificaciones.getStyle().contains("-fx-background-color")) {
            // mostrarNotificaciones();
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

    private Stage obtenerVentanaActual() {
        // Buscar la ventana desde cualquier nodo de la escena
        if (mainContainer != null && mainContainer.getScene() != null) {
            return (Stage) mainContainer.getScene().getWindow();
        } else if (contentPane != null && contentPane.getScene() != null) {
            return (Stage) contentPane.getScene().getWindow();
        } else if (btnCerrarSesion != null && btnCerrarSesion.getScene() != null) {
            return (Stage) btnCerrarSesion.getScene().getWindow();
        }
        throw new IllegalStateException("No se pudo obtener la ventana actual");
    }

    private void navegarALogin() {
        try {
            // Usar SpringFXMLLoader para cargar la vista de login
            Parent root = springFXMLLoader.load("/view/Login.fxml");

            // Obtener la ventana actual
            Stage stage = obtenerVentanaActual();

            // Cambiar la escena
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Inicio de Sesión");
            stage.centerOnScreen();
            stage.show();

        } catch (Exception e) {
            logger.error("Error al navegar a login", e);
            mostrarAlerta("Error", "No se pudo cargar la pantalla de inicio de sesión");
        }
    }

    private void cerrarSesion() {
        try {
            // Limpiar el token
            if (jwtTokenHolder != null) {
                jwtTokenHolder.clearToken();
            }

            // Navegar de vuelta a login
            navegarALogin();

        } catch (Exception e) {
            logger.error("Error al cerrar sesión", e);
            mostrarAlerta("Error", "No se pudo cerrar la sesión");
        }
    }

}