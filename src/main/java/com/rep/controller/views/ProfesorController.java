package com.rep.controller.views;

import javafx.geometry.Rectangle2D;  // Para Rectangle2D
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import com.rep.dto.actividad.ActividadCreateDTO;
import com.rep.dto.actividad.ActividadDTO;
import com.rep.dto.actividad.PreguntaPlantillaDTO;
import com.rep.dto.curso.CursoDTO;
import com.rep.dto.tokens.JwtTokenHolder;
import com.rep.model.*;
import com.rep.repositories.ProfesorMateriaRepository;
import com.rep.repositories.ProfesorRepository;
import com.rep.service.logica.ProfesorMateriaService;
import io.jsonwebtoken.io.IOException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
public class ProfesorController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String API_BASE_URL = "http://localhost:8080/api/profesor";
    private final String API_actividad_URL = "http://localhost:8080/api";
    private final String API_PREGUNTAS_URL = "http://localhost:8080/api/preguntas";
    // Elementos de la UI
    @FXML private ComboBox<Materia> materiaComboBox;
    @FXML private ComboBox<CursoDTO> cursoComboBox;
    @FXML private ComboBox<String> tipoActividadComboBox;
    @FXML private TextField tituloTextField;
    @FXML private DatePicker fechaEntregaPicker;
    @FXML private TextField duracionTextField;
    @FXML private Button crearActividadButton;
    @FXML private Label statusActividadLabel;
    @FXML private TableView<Actividad> actividadesTable;
    @FXML private TableColumn<Actividad, String> colTitulo;
    @FXML private TableColumn<Actividad, String> colTipo;
    @FXML private TableColumn<Actividad, LocalDate> colFecha;
    @FXML private TableColumn<Actividad, Integer> colDuracion;
    @Autowired
    private JwtTokenHolder jwtTokenHolder;
    @Autowired
    private ProfesorRepository pRepository;
    @Autowired
    private ProfesorMateriaRepository profesorMateriaRepository;
    @Autowired
    private ProfesorMateriaService profesorMateriaService;
    private ObservableList<Actividad> actividadesList = FXCollections.observableArrayList();


    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        // Verificación robusta del token
        if (jwtTokenHolder == null || jwtTokenHolder.getToken() == null || jwtTokenHolder.getToken().isEmpty()) {
            mostrarError("Error de autenticación: Token no disponible", Color.RED);
            throw new RuntimeException("Token JWT no disponible");
        }

        // Asegurar que el token comience con "Bearer "
        String token = jwtTokenHolder.getToken();
        if (!token.startsWith("Bearer ")) {
            token = "Bearer " + token;
        }

        headers.set("Authorization", token);

        // Añadir headers adicionales para seguridad
        headers.set("X-Requested-With", "XMLHttpRequest");

        return headers;
    }

    @FXML
    public void initialize() {

        actividadesTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                // Rellenar formulario con los datos de la actividad seleccionada
                tituloTextField.setText(newVal.getTitulo());
                fechaEntregaPicker.setValue(newVal.getFechaEntrega());
                duracionTextField.setText(String.valueOf(newVal.getDuracionMinutos()));
                tipoActividadComboBox.getSelectionModel().select(newVal.getTipo().toString().toLowerCase());
            }
        });


        // Configurar combobox de tipos de actividad
        tipoActividadComboBox.setItems(FXCollections.observableArrayList(
                "examen", "quiz", "taller"  // Valores en minúsculas para coincidir con la BD
        ));
        // Configurar tabla de actividades
        configurarTablaActividades();

        // Cargar datos iniciales
        cargarMaterias();
        cargarCursos();

        // Configurar listeners
        materiaComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                filtrarActividades();
            }
        });

        cursoComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                filtrarActividades();
            }
        });

        // Configurar botón de crear actividad
        crearActividadButton.setOnAction(event -> crearActividad());
    }

    private void configurarTablaActividades() {
        colTitulo.setCellValueFactory(new PropertyValueFactory<>("titulo"));
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fechaEntrega"));
        colDuracion.setCellValueFactory(new PropertyValueFactory<>("duracionMinutos"));

        actividadesTable.setItems(actividadesList);


    }



    private void cargarMaterias() {
        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<List<Materia>> response = restTemplate.exchange(
                    API_BASE_URL + "/materias",
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<Materia>>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                materiaComboBox.setItems(FXCollections.observableArrayList(response.getBody()));
                materiaComboBox.setCellFactory(lv -> new ListCell<Materia>() {
                    @Override
                    protected void updateItem(Materia item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty ? null : item.getNombre());
                    }
                });
                materiaComboBox.setButtonCell(new ListCell<Materia>() {
                    @Override
                    protected void updateItem(Materia item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty ? null : item.getNombre());
                    }
                });
            }
        } catch (Exception e) {
            mostrarError("Error al cargar materias: " + e.getMessage());
        }

    }

    private void cargarCursos() {
        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<List<CursoDTO>> response = restTemplate.exchange(
                    API_BASE_URL + "/cursos",
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<CursoDTO>>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                cursoComboBox.setItems(FXCollections.observableArrayList(response.getBody()));
            }
        } catch (Exception e) {
            mostrarError("Error al cargar cursos: " + e.getMessage());
        }
    }

    private void filtrarActividades() {
        try {
            Materia materiaSeleccionada = materiaComboBox.getSelectionModel().getSelectedItem();
            CursoDTO cursoSeleccionado = cursoComboBox.getSelectionModel().getSelectedItem();

            Long materiaId = materiaSeleccionada != null ? materiaSeleccionada.getId() : null;
            Long cursoId = cursoSeleccionado != null ? cursoSeleccionado.getId() : null;
            Long profesorId = jwtTokenHolder.getUserId();

            String url = API_actividad_URL + "/actividades";

            // Añadir parámetros solo si no son null
            if (materiaId != null || cursoId != null) {
                url += "?";
                if (materiaId != null) {
                    url += "materiaId=" + materiaId;
                }
                if (cursoId != null) {
                    if (materiaId != null) url += "&";
                    url += "cursoId=" + cursoId;
                }
            }

            ResponseEntity<List<ActividadDTO>> response = executeAuthenticatedRequest(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<ActividadDTO>>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                actividadesList.clear();
                response.getBody().forEach(dto -> {
                    Actividad actividad = new Actividad();
                    actividad.setId(dto.getId());
                    actividad.setTitulo(dto.getTitulo());
                    actividad.setTipo(dto.getTipo());
                    actividad.setFechaEntrega(dto.getFechaEntrega());
                    actividad.setDuracionMinutos(dto.getDuracionMinutos());
                    actividadesList.add(actividad);
                });
            } else if (response.getStatusCode() == HttpStatus.FORBIDDEN) {
                mostrarError("No tiene permisos para ver estas actividades", Color.RED);
            }
        } catch (Exception e) {
            mostrarError("Error al cargar actividades: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void crearActividad() {
        // Obtener valores de los campos
        Materia materia = materiaComboBox.getSelectionModel().getSelectedItem();
        CursoDTO cursoDTO = cursoComboBox.getSelectionModel().getSelectedItem();
        String tipoStr = tipoActividadComboBox.getSelectionModel().getSelectedItem();
        String titulo = tituloTextField.getText().trim();
        LocalDate fechaEntrega = fechaEntregaPicker.getValue();
        String duracionStr = duracionTextField.getText().trim();

        // Validación de campos obligatorios
        if (materia == null || cursoDTO == null || tipoStr == null ||
                titulo.isEmpty() || fechaEntrega == null || duracionStr.isEmpty()) {
            mostrarError("Todos los campos son obligatorios", Color.RED);
            return;
        }

        try {
            // Validar duración
            int duracion = Integer.parseInt(duracionStr);
            if (duracion <= 0) {
                mostrarError("La duración debe ser mayor a 0", Color.RED);
                return;
            }

            // Validar fecha no pasada
            if (fechaEntrega.isBefore(LocalDate.now())) {
                mostrarError("La fecha de entrega no puede ser en el pasado", Color.RED);
                return;
            }

            // Validar título
            if (titulo.length() < 3) {
                mostrarError("El título debe tener al menos 3 caracteres", Color.RED);
                return;
            }

            // Convertir tipo de actividad
            Actividad.TipoActividad tipo = Actividad.TipoActividad.forValue(tipoStr.toLowerCase());

            // Crear DTO para la nueva actividad
            ActividadCreateDTO actividadDTO = new ActividadCreateDTO();
            actividadDTO.setTitulo(titulo);
            actividadDTO.setTipo(tipo);
            actividadDTO.setFechaEntrega(fechaEntrega);
            actividadDTO.setDuracionMinutos(duracion);
            actividadDTO.setDescripcion(""); // Valor por defecto

            // Obtener IDs necesarios
            Long profesorId = jwtTokenHolder.getUserId();
            Long materiaId = materia.getId();
            Long cursoId = cursoDTO.getId();

            // Verificar relación Profesor-Materia-Curso
            Optional<ProfesorMateria> existente = profesorMateriaService
                    .findByProfesorIdAndMateriaIdAndCursoId(profesorId, materiaId, cursoId);

            ProfesorMateria profesorMateria;

            if (existente.isPresent()) {
                profesorMateria = existente.get();
            } else {
                profesorMateria = new ProfesorMateria();
                profesorMateria.setProfesor(pRepository.findById(profesorId)
                        .orElseThrow(() -> new RuntimeException("Profesor no encontrado")));
                profesorMateria.setMateria(new Materia(materiaId));
                profesorMateria.setCurso(new Curso(cursoId));
                profesorMateria = profesorMateriaService.guardarProfesorMateria(profesorMateria);
            }

            actividadDTO.setProfesorMateriaId(profesorMateria.getId());

            // Configurar headers
            HttpHeaders headers = createHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Enviar solicitud al servidor
            ResponseEntity<ActividadDTO> response = restTemplate.postForEntity(
                    API_actividad_URL + "/actividades",
                    new HttpEntity<>(actividadDTO, headers),
                    ActividadDTO.class
            );

            // Procesar respuesta
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                ActividadDTO actividadCreada = response.getBody();
                mostrarError("Actividad '" + actividadCreada.getTitulo() + "' creada exitosamente", Color.GREEN);
                limpiarFormulario();
                filtrarActividades(); // Refrescar la lista
            } else {
                mostrarError("Error al crear actividad. Código: " + response.getStatusCode(), Color.RED);
            }

        } catch (NumberFormatException e) {
            mostrarError("La duración debe ser un número válido", Color.RED);
        } catch (IllegalArgumentException e) {
            mostrarError("Tipo de actividad no válido. Use: Examen, Quiz o Taller", Color.RED);
        } catch (HttpClientErrorException.Forbidden e) {
            mostrarError("Acceso denegado: No tiene permisos para crear actividades", Color.RED);
        } catch (HttpClientErrorException.BadRequest e) {
            mostrarError("Datos inválidos: " + e.getResponseBodyAsString(), Color.RED);
        } catch (Exception e) {
            mostrarError("Error al crear actividad: " + e.getMessage(), Color.RED);
            e.printStackTrace();
        }
    }

    private void limpiarFormulario() {
        tituloTextField.clear();
        fechaEntregaPicker.setValue(null);
        duracionTextField.clear();
        tipoActividadComboBox.getSelectionModel().clearSelection();
    }

    private void mostrarError(String mensaje) {
        mostrarError(mensaje, Color.RED);
    }

    private void mostrarError(String mensaje, Color color) {
        statusActividadLabel.setTextFill(color);
        statusActividadLabel.setText(mensaje);
    }

    // Métodos adicionales para otras funcionalidades
    @FXML
    private void verResultadosPorActividad() {
        Actividad actividadSeleccionada = actividadesTable.getSelectionModel().getSelectedItem();
        if (actividadSeleccionada == null) {
            mostrarError("Seleccione una actividad para ver los resultados", Color.RED);
            return;
        }

        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<List<RespuestaEstudiante>> response = restTemplate.exchange(
                    API_actividad_URL + "/respuestas/actividad/" + actividadSeleccionada.getId(),
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<RespuestaEstudiante>>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                // Aquí puedes mostrar los resultados en una nueva ventana o diálogo
                mostrarResultados(response.getBody(), actividadSeleccionada.getTitulo());
            }
        } catch (Exception e) {
            mostrarError("Error al cargar resultados: " + e.getMessage());
        }
    }

    // Método para obtener preguntas de una actividad
    public ObservableList<Pregunta> obtenerPreguntasDeActividad(Long actividadId) {
        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<List<Pregunta>> response = restTemplate.exchange(
                    API_PREGUNTAS_URL + "/actividad/" + actividadId,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<Pregunta>>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return FXCollections.observableArrayList(response.getBody());
            }
        } catch (Exception e) {
            mostrarError("Error al cargar preguntas: " + e.getMessage());
        }
        return FXCollections.emptyObservableList();
    }

    public boolean crearPregunta(Pregunta pregunta) {
        try {
            HttpHeaders headers = createHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<Pregunta> response = restTemplate.postForEntity(
                    API_BASE_URL + "/preguntas",
                    new HttpEntity<>(pregunta, headers),
                    Pregunta.class
            );

            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Método para eliminar una pregunta
    public boolean eliminarPregunta(Long preguntaId) {
        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Void> response = restTemplate.exchange(
                    API_PREGUNTAS_URL + "/" + preguntaId,
                    HttpMethod.DELETE,
                    entity,
                    Void.class
            );

            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            mostrarError("Error al eliminar pregunta: " + e.getMessage());
            return false;
        }
    }

    @FXML
    private void mostrarEditorPreguntas() {
        // Verificar selección de actividad
        Actividad actividadSeleccionada = actividadesTable.getSelectionModel().getSelectedItem();
        if (actividadSeleccionada == null) {
            mostrarError("Seleccione una actividad primero", Color.RED);
            return;
        }

        try {
            // Cargar el FXML con verificación de existencia
            URL fxmlUrl = getClass().getResource("/view/editor_preguntas.fxml");
            if (fxmlUrl == null) {
                throw new IllegalStateException("No se encontró el archivo FXML: editor_preguntas.fxml");
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            // Configurar el controlador con validación
            EditorPreguntasController controller = loader.getController();
            if (controller == null) {
                throw new IllegalStateException("El controlador no fue inicializado correctamente");
            }

            // Inyectar dependencias con validación de token
            if (jwtTokenHolder == null || jwtTokenHolder.getToken() == null) {
                throw new IllegalStateException("Token JWT no disponible");
            }

            controller.setProfesorController(this);
            controller.setJwtTokenHolder(this.jwtTokenHolder);
            controller.setActividad(actividadSeleccionada);

            // Configurar la ventana modal con mejores dimensiones
            Stage dialog = new Stage();
            dialog.setTitle(String.format("Editor de Preguntas - %s", actividadSeleccionada.getTitulo()));

            // Configuración responsiva de la ventana
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            Scene scene = new Scene(root,
                    Math.min(1200, screenBounds.getWidth() * 0.85),
                    Math.min(850, screenBounds.getHeight() * 0.85));

            // Aplicar CSS si existe
            try {
                URL cssUrl = getClass().getResource("/css/editor_preguntas.css");
                if (cssUrl != null) {
                    scene.getStylesheets().add(cssUrl.toExternalForm());
                }
            } catch (Exception e) {
                System.err.println("No se pudo cargar el CSS: " + e.getMessage());
            }

            dialog.setScene(scene);
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(actividadesTable.getScene().getWindow());

            // Configuración de tamaño mínimo/máximo
            dialog.setMinWidth(900);
            dialog.setMinHeight(650);
            dialog.setMaxWidth(screenBounds.getWidth() * 0.9);
            dialog.setMaxHeight(screenBounds.getHeight() * 0.9);

            // Centrar la ventana
            dialog.centerOnScreen();

            // Manejar cierre de ventana
            dialog.setOnCloseRequest(event -> {
                // Opcional: Mostrar confirmación si hay cambios no guardados
                // if (controller.tieneCambiosNoGuardados()) {
                //     event.consume();
                //     mostrarConfirmacionCierre(dialog);
                // }
            });

            // Mostrar y esperar
            dialog.showAndWait();

        } catch (IllegalStateException e) {
            mostrarError("Error de configuración: " + e.getMessage(), Color.RED);
            e.printStackTrace();
        } catch (IOException e) {
            mostrarError("Error al cargar la interfaz: " + e.getMessage(), Color.RED);
            e.printStackTrace();
        } catch (Exception e) {
            mostrarError("Error inesperado: " + e.getMessage(), Color.RED);
            e.printStackTrace();
        }
    }
    private void mostrarResultados(List<RespuestaEstudiante> respuestas, String tituloActividad) {
        // Implementa la lógica para mostrar los resultados
        // Puedes crear un diálogo o una nueva ventana con una tabla
        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle("Resultados de " + tituloActividad);
        dialog.setHeaderText(null);

        // Crear tabla para mostrar los resultados
        TableView<RespuestaEstudiante> table = new TableView<>();

        TableColumn<RespuestaEstudiante, String> colEstudiante = new TableColumn<>("Estudiante");
        colEstudiante.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getEstudiante().getNombre()));

        TableColumn<RespuestaEstudiante, String> colNota = new TableColumn<>("Nota");
        colNota.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(String.valueOf(cellData.getValue().getNota())));

        table.getColumns().addAll(colEstudiante, colNota);
        table.setItems(FXCollections.observableArrayList(respuestas));

        dialog.getDialogPane().setContent(table);
        dialog.showAndWait();
    }
    private <T> ResponseEntity<T> executeAuthenticatedRequest(String url, HttpMethod method,
                                                              HttpEntity<?> requestEntity,
                                                              ParameterizedTypeReference<T> responseType) {
        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<?> entity = requestEntity != null ?
                    new HttpEntity<>(requestEntity.getBody(), headers) :
                    new HttpEntity<>(headers);

            return restTemplate.exchange(
                    url,
                    method,
                    entity,
                    responseType
            );
        } catch (HttpClientErrorException.Forbidden e) {
            mostrarError("Acceso denegado: No tiene permisos para esta acción", Color.RED);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (HttpClientErrorException.Unauthorized e) {
            mostrarError("Sesión expirada, por favor inicie sesión nuevamente", Color.RED);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            mostrarError("Error en la comunicación con el servidor: " + e.getMessage(), Color.RED);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Mantén también la versión original para Class<T>
    private <T> ResponseEntity<T> executeAuthenticatedRequest(String url, HttpMethod method,
                                                              HttpEntity<?> requestEntity,
                                                              Class<T> responseType) {
        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<?> entity = requestEntity != null ?
                    new HttpEntity<>(requestEntity.getBody(), headers) :
                    new HttpEntity<>(headers);

            return restTemplate.exchange(
                    url,
                    method,
                    entity,
                    responseType
            );
        } catch (HttpClientErrorException.Forbidden e) {
            mostrarError("Acceso denegado: No tiene permisos para esta acción", Color.RED);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (HttpClientErrorException.Unauthorized e) {
            mostrarError("Sesión expirada, por favor inicie sesión nuevamente", Color.RED);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            mostrarError("Error en la comunicación con el servidor: " + e.getMessage(), Color.RED);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    @FXML
    private void cerrarSesion() {
        try {
            // Limpiar el token
            jwtTokenHolder.clearToken();

            // Cargar la pantalla de login
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/login.fxml"));
            Parent root = loader.load();

            // Obtener la escena actual y cambiar su contenido
            Stage stage = (Stage) actividadesTable.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Inicio de Sesión");
            stage.show();

        } catch (Exception e) {
            mostrarError("Error al cerrar sesión: " + e.getMessage(), Color.RED);
            e.printStackTrace();
        }
    }

    @FXML
    private void actualizarActividad() {
        // Obtener la actividad seleccionada
        Actividad actividadSeleccionada = actividadesTable.getSelectionModel().getSelectedItem();
        if (actividadSeleccionada == null) {
            mostrarError("Seleccione una actividad para actualizar", Color.RED);
            return;
        }

        // Mostrar diálogo de confirmación
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirmar actualización");
        confirmDialog.setHeaderText("Actualizar actividad");
        confirmDialog.setContentText("¿Está seguro que desea actualizar esta actividad?");

        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Crear DTO con los nuevos valores del formulario
                ActividadCreateDTO actividadDTO = new ActividadCreateDTO();
                actividadDTO.setTitulo(tituloTextField.getText().trim());
                actividadDTO.setFechaEntrega(fechaEntregaPicker.getValue());
                actividadDTO.setDuracionMinutos(Integer.parseInt(duracionTextField.getText().trim()));
                actividadDTO.setTipo(Actividad.TipoActividad.forValue(tipoActividadComboBox.getSelectionModel().getSelectedItem().toLowerCase()));
                actividadDTO.setDescripcion(""); // O actualizar si tienes campo de descripción

                // Configurar headers
                HttpHeaders headers = createHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                // Enviar solicitud PUT al servidor
                ResponseEntity<ActividadDTO> response = restTemplate.exchange(
                        API_actividad_URL + "/actividades/" + actividadSeleccionada.getId(),
                        HttpMethod.PUT,
                        new HttpEntity<>(actividadDTO, headers),
                        ActividadDTO.class
                );

                // Procesar respuesta
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    mostrarError("Actividad actualizada exitosamente", Color.GREEN);
                    filtrarActividades(); // Refrescar la lista
                } else {
                    mostrarError("Error al actualizar actividad. Código: " + response.getStatusCode(), Color.RED);
                }
            } catch (NumberFormatException e) {
                mostrarError("La duración debe ser un número válido", Color.RED);
            } catch (Exception e) {
                mostrarError("Error al actualizar actividad: " + e.getMessage(), Color.RED);
                e.printStackTrace();
            }
        }
    }
    @FXML
    private void exportarResultados() {
        // 1. Validar selección de actividad y curso
        Actividad actividad = actividadesTable.getSelectionModel().getSelectedItem();
        if (actividad == null) {
            mostrarError("Seleccione una actividad primero", Color.RED);
            return;
        }
        if (cursoComboBox.getValue() == null) {
            mostrarError("Seleccione un curso primero", Color.RED);
            return;
        }

        // 2. Diálogo para elegir formato (PDF o Excel)
        ChoiceDialog<String> dialog = new ChoiceDialog<>("PDF", List.of("PDF", "Excel"));
        dialog.setTitle("Exportar resultados");
        dialog.setHeaderText("Seleccione el formato de exportación");
        Optional<String> resultado = dialog.showAndWait();

        if (resultado.isPresent()) {
            String formato = resultado.get().toLowerCase();
            String url = String.format(
                    "%s/reportes/resultados/export?cursoId=%d&actividadId=%d&formato=%s",
                    API_BASE_URL,
                    cursoComboBox.getValue().getId(),
                    actividad.getId(),
                    formato
            );

            try {
                // 3. Configurar headers con el token JWT
                HttpHeaders headers = new HttpHeaders();
                headers.set("Authorization", "Bearer " + jwtTokenHolder.getToken());
                HttpEntity<String> entity = new HttpEntity<>(headers);

                // 4. Hacer la petición al backend
                ResponseEntity<byte[]> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        byte[].class
                );

                // 5. Mostrar diálogo para guardar el archivo
                FileChooser fileChooser = new FileChooser();
                fileChooser.setInitialFileName(
                        String.format("resultados_%s.%s", actividad.getTitulo(), formato)
                );
                fileChooser.getExtensionFilters().addAll(
                        new FileChooser.ExtensionFilter(
                                formato.toUpperCase() + " Files", "*." + formato)
                );
                File file = fileChooser.showSaveDialog(actividadesTable.getScene().getWindow());

                if (file != null) {
                    // 6. Guardar el archivo localmente
                    Files.write(file.toPath(), response.getBody());
                    mostrarError("Archivo guardado en: " + file.getAbsolutePath(), Color.GREEN);
                }
            } catch (Exception e) {
                mostrarError("Error al exportar: " + e.getMessage(), Color.RED);
                e.printStackTrace();
            }
        }

    }

}