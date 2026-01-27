package com.rep.controller.views;

import com.rep.dto.actividad.*;
import com.rep.dto.tokens.JwtTokenHolder;
import com.rep.model.Actividad;
import com.rep.model.Opcion;
import com.rep.model.Pregunta;
import com.rep.model.Pregunta.TipoPregunta;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URL;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.stage.FileChooser;
import java.io.File;
import org.springframework.core.io.FileSystemResource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Controller
public class EditorPreguntasController implements Initializable {

    private static final String API_BASE_URL = "http://localhost:8080/api/preguntas";
    private static final Logger logger = LoggerFactory.getLogger(EditorPreguntasController.class);

    // UI Components
    @FXML
    private Spinner<Integer> spinnerLongitud;
    @FXML
    private VBox panelOpciones;
    @FXML
    private TableView<Pregunta> preguntasTable;
    @FXML
    private TableColumn<Pregunta, String> colEnunciado;
    @FXML
    private TableColumn<Pregunta, String> colTipo;
    @FXML
    private TextArea campoPregunta;
    @FXML
    private ComboBox<TipoPregunta> tipoComboBox;
    @FXML
    private ListView<String> listaOpciones;
    @FXML
    private TextField campoNuevaOpcion;
    @FXML
    private CheckBox checkCorrecta;
    @FXML
    private Button btnAgregarOpcion;
    @FXML
    private Button btnEliminarOpcion;
    @FXML
    private Button btnGuardar;
    @FXML
    private Button btnAgregarPregunta;
    @FXML
    private Button btnEliminarPregunta;
    @FXML
    private Label statusLabel;
    @FXML
    private RadioButton radioVerdadero;
    @FXML
    private RadioButton radioFalso;
    @FXML
    private Button btnAdjuntarArchivo;
    @FXML
    private Hyperlink linkArchivo;
    @FXML
    private Label lblNombreArchivo;
    private File archivoSeleccionado;
    private Map<Integer, File> archivosOpciones = new HashMap<>(); // clave: índice en listaOpciones
    private Map<Integer, String> archivosOpcionesExistentes = new HashMap<>(); // nombre de archivo para opciones ya guardadas
    @FXML
    private Button btnAdjuntarArchivoOpcion;
    @FXML
    private HBox panelArchivoOpcion;
    @FXML
    private Label lblArchivoOpcion;
    @FXML
    private Button btnEliminarArchivoOpcion;
    private File archivoOpcionActual;
    private int indiceOpcionParaArchivo = -1;
    private ToggleGroup toggleGroupVF;
    private ProfesorController profesorController;

    public void setProfesorController(ProfesorController profesorController) {
        this.profesorController = profesorController;
    }

    // Services and Data
    private final RestTemplate restTemplate = new RestTemplate();
    private JwtTokenHolder jwtTokenHolder;
    private Actividad actividad;
    private ObservableList<Pregunta> preguntasList = FXCollections.observableArrayList();
    private ObservableList<String> opcionesList = FXCollections.observableArrayList();
    private Pregunta preguntaActual;

    public void setJwtTokenHolder(JwtTokenHolder jwtTokenHolder) {
        this.jwtTokenHolder = jwtTokenHolder;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configurarTablaPreguntas();
        configurarComboboxTipo();
        inicializarToggleGroup(); // Añade esta línea
        configurarListeners();
        configurarBotones();
        listaOpciones.setItems(opcionesList);
        // Spinner inicial: oculto por defecto (solo visible para RESPUESTA_ABIERTA)
        if (spinnerLongitud != null) {
            try {
                spinnerLongitud.setValueFactory(new javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory(1, 5000, 200));
            } catch (Exception ignored) {}
            spinnerLongitud.setVisible(false);
            spinnerLongitud.setManaged(false);
        }
    }

    private void inicializarToggleGroup() {
        toggleGroupVF = new ToggleGroup();
        radioVerdadero.setToggleGroup(toggleGroupVF);
        radioVerdadero.setUserData(true);
        radioFalso.setToggleGroup(toggleGroupVF);
        radioFalso.setUserData(false);
    }

    private void configurarBotones() {
        btnGuardar.setOnAction(e -> guardarPregunta());
        btnAgregarPregunta.setOnAction(e -> agregarPregunta());
        btnEliminarPregunta.setOnAction(e -> eliminarPregunta());
    }

    private HttpHeaders createHeadersWithToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        if (jwtTokenHolder == null || jwtTokenHolder.getToken() == null || jwtTokenHolder.getToken().isEmpty()) {
            mostrarEstado("Error: Token no disponible", Color.RED);
            throw new RuntimeException("Token JWT no disponible");
        }

        headers.set("Authorization", "Bearer " + jwtTokenHolder.getToken());
        return headers;
    }

    private void configurarTablaPreguntas() {
        colEnunciado.setCellValueFactory(new PropertyValueFactory<>("enunciado"));
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        preguntasTable.setItems(preguntasList);
        preguntasTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> cargarPreguntaSeleccionada(newSelection));
    }

    @FXML
    private Label tituloLabel;

    public void setActividad(Actividad actividad) {
        this.actividad = actividad;
        if (actividad != null && tituloLabel != null) {
            tituloLabel.setText("Editor de Preguntas - " + actividad.getTitulo());
        }
        cargarPreguntas();
    }

    private void cargarPreguntas() {
        try {
            ResponseEntity<List<Pregunta>> response = restTemplate.exchange(
                    API_BASE_URL + "/actividad/" + actividad.getId(),
                    HttpMethod.GET,
                    new HttpEntity<>(createHeadersWithToken()),
                    new ParameterizedTypeReference<List<Pregunta>>() {
                    });

            if (response.getStatusCode() == HttpStatus.OK) {
                preguntasList.setAll(response.getBody());
                mostrarEstado("Preguntas cargadas", Color.GREEN);
            }
        } catch (Exception e) {
            mostrarEstado("Error al cargar preguntas: " + e.getMessage(), Color.RED);
        }
    }

    private void cargarPreguntaSeleccionada(Pregunta pregunta) {
        if (pregunta == null) {
            limpiarFormulario();
            return;
        }

        preguntaActual = pregunta;
        campoPregunta.setText(pregunta.getEnunciado());
        tipoComboBox.setValue(pregunta.getTipo());

        // Mostrar archivo si existe
        if (pregunta.getArchivoUrl() != null && !pregunta.getArchivoUrl().isEmpty()) {
            linkArchivo.setVisible(true);
            linkArchivo.setText("Ver Archivo");
            lblNombreArchivo.setText(new File(pregunta.getArchivoUrl()).getName());
        } else {
            linkArchivo.setVisible(false);
            lblNombreArchivo.setText("");
        }
        archivoSeleccionado = null; // Resetear selección local

        // Limpiar datos existentes
        opcionesList.clear();

        // No intentes deseleccionar si no hay toggle seleccionado
        Toggle selectedToggle = toggleGroupVF.getSelectedToggle();
        if (selectedToggle != null) {
            selectedToggle.setSelected(false);
        }

        switch (pregunta.getTipo()) {
            case OPCION_MULTIPLE:
                if (pregunta.getOpciones() != null) {
                    List<Opcion> opciones = new ArrayList<>(pregunta.getOpciones());
                    for (int i = 0; i < opciones.size(); i++) {
                        Opcion opcion = opciones.get(i);
                        String textoOpcion = opcion.getTexto();

                        // Indicar si la opción ya tiene un archivo asociado
                        if (opcion.getTieneArchivo() != null && opcion.getTieneArchivo()
                                && opcion.getNombreArchivo() != null && !opcion.getNombreArchivo().isEmpty()) {
                            textoOpcion += " 📎 [" + opcion.getNombreArchivo() + "]";
                            archivosOpcionesExistentes.put(i, opcion.getNombreArchivo());
                        }

                        if (opcion.getEsCorrecta()) {
                            textoOpcion += " (Correcta)";
                        }
                        opcionesList.add(textoOpcion);
                    }
                }
                break;

            case VERDADERO_FALSO:
                if (pregunta.getOpciones() != null) {
                    for (Opcion opcion : pregunta.getOpciones()) {
                        if (opcion.getEsCorrecta()) {
                            if (opcion.getTexto().equalsIgnoreCase("Verdadero")) {
                                radioVerdadero.setSelected(true);
                            } else if (opcion.getTexto().equalsIgnoreCase("Falso")) {
                                radioFalso.setSelected(true);
                            }
                        }
                    }
                }
                break;

            case RESPUESTA_ABIERTA:
                // if (pregunta.getLongitudMaxima() != null) {
                // spinnerLongitud.getValueFactory().setValue(pregunta.getLongitudMaxima());
                // }
                break;
        }

        Platform.runLater(() -> actualizarInterfazSegunTipo());
    }

    private void limpiarFormulario() {
        preguntaActual = null;
        campoPregunta.clear();
        tipoComboBox.getSelectionModel().clearSelection();
        opcionesList.clear();

        // Manejo seguro del ToggleGroup
        Toggle selectedToggle = toggleGroupVF.getSelectedToggle();
        if (selectedToggle != null) {
            selectedToggle.setSelected(false);
        }

        // Restablecer spinner
        if (spinnerLongitud != null) {
            spinnerLongitud.getValueFactory().setValue(200);
        }
    }

    @FXML
    private void agregarPregunta() {
        Dialog<PreguntaRequest> dialog = new Dialog<>();
        dialog.setTitle("Nueva Pregunta");

        // Setup dialog UI
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        TextField enunciadoField = new TextField();
        enunciadoField.setPromptText("Ingrese el enunciado");
        ComboBox<TipoPregunta> tipoCombo = new ComboBox<>(FXCollections.observableArrayList(TipoPregunta.values()));
        tipoCombo.setPromptText("Seleccione tipo");
        grid.addRow(0, new Label("Enunciado:"), enunciadoField);
        grid.addRow(1, new Label("Tipo:"), tipoCombo);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                PreguntaRequest request = new PreguntaRequest();
                request.setActividadId(actividad.getId());
                request.setEnunciado(enunciadoField.getText());
                request.setTipo(tipoCombo.getValue());
                return request;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(request -> {
            try {
                ResponseEntity<Pregunta> response = restTemplate.exchange(
                        API_BASE_URL,
                        HttpMethod.POST,
                        new HttpEntity<>(request, createHeadersWithToken()),
                        Pregunta.class);
                if (response.getStatusCode().is2xxSuccessful()) {
                    cargarPreguntas(); // Refresh list
                    mostrarEstado("Pregunta creada exitosamente", Color.GREEN);
                }
            } catch (Exception e) {
                mostrarEstado("Error al crear pregunta: " + e.getMessage(), Color.RED);
            }
        });
    }

    @FXML
    private void eliminarPregunta() {
        Pregunta seleccionada = preguntasTable.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            mostrarEstado("Seleccione una pregunta para eliminar", Color.RED);
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar eliminación");
        confirmacion.setHeaderText("¿Está seguro de eliminar esta pregunta?");
        confirmacion.setContentText("Esta acción no se puede deshacer.");

        Optional<ButtonType> resultado = confirmacion.showAndWait();
        if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
            try {
                ResponseEntity<Void> response = restTemplate.exchange(
                        API_BASE_URL + "/" + seleccionada.getId(),
                        HttpMethod.DELETE,
                        new HttpEntity<>(createHeadersWithToken()),
                        Void.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    cargarPreguntas();
                    mostrarEstado("Pregunta eliminada", Color.GREEN);
                }
            } catch (Exception e) {
                mostrarEstado("Error al eliminar: " + e.getMessage(), Color.RED);
            }
        }
    }

    @FXML
    private void guardarPregunta() {
        if (!validarFormulario()) return;

        try {
            PreguntaRequest request = new PreguntaRequest();
            request.setActividadId(actividad.getId());
            request.setEnunciado(campoPregunta.getText());
            request.setTipo(tipoComboBox.getValue());

            // Manejar cada tipo de pregunta según corresponda
            switch (request.getTipo()) {
                case OPCION_MULTIPLE:
                    request.setOpciones(convertirOpcionesUIaDTO());
                    break;

                case VERDADERO_FALSO:
                    List<OpcionRequest> opcionesVF = new ArrayList<>();
                    OpcionRequest opcionVerdadero = new OpcionRequest();
                    opcionVerdadero.setTexto("Verdadero");
                    opcionVerdadero.setEsCorrecta(radioVerdadero.isSelected());
                    opcionesVF.add(opcionVerdadero);
                    OpcionRequest opcionFalso = new OpcionRequest();
                    opcionFalso.setTexto("Falso");
                    opcionFalso.setEsCorrecta(radioFalso.isSelected());
                    opcionesVF.add(opcionFalso);
                    request.setOpciones(opcionesVF);
                    break;

                case RESPUESTA_ABIERTA:
                    // spinner solo visible para respuesta abierta; nothing to send in DTO currently
                    break;
            }

            if (preguntaActual == null) {
                // Crear nueva pregunta
                ResponseEntity<Pregunta> response = restTemplate.exchange(
                        API_BASE_URL,
                        HttpMethod.POST,
                        new HttpEntity<>(request, createHeadersWithToken()),
                        Pregunta.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    Pregunta creada = response.getBody();
                    preguntaActual = creada;

                    // Subir archivo de pregunta si fue seleccionado
                    if (archivoSeleccionado != null) {
                        boolean subio = subirArchivo(creada.getId(), archivoSeleccionado);
                        if (subio) {
                            linkArchivo.setVisible(true);
                            linkArchivo.setText("Ver Archivo: " + archivoSeleccionado.getName());
                            lblNombreArchivo.setText(archivoSeleccionado.getName());
                        }
                    }

                    // Subir archivos de opciones
                    try { subirArchivosOpciones(creada.getId()); } catch (Exception ex) {
                        logger.error("Error subiendo archivos de opciones tras crear pregunta", ex);
                    }

                    cargarPreguntas();
                    mostrarEstado("Pregunta creada y guardada", Color.GREEN);
                    return;
                } else {
                    mostrarEstado("Error creando pregunta: " + response.getStatusCode(), Color.RED);
                    return;
                }
            } else {
                // Actualizar existente
                ResponseEntity<Pregunta> response = restTemplate.exchange(
                        API_BASE_URL + "/" + preguntaActual.getId(),
                        HttpMethod.PUT,
                        new HttpEntity<>(request, createHeadersWithToken()),
                        Pregunta.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    if (archivoSeleccionado != null) {
                        boolean archivoSubido = subirArchivo(preguntaActual.getId(), archivoSeleccionado);
                        if (archivoSubido) {
                            linkArchivo.setVisible(true);
                            linkArchivo.setText("Ver Archivo: " + archivoSeleccionado.getName());
                            lblNombreArchivo.setText(archivoSeleccionado.getName());
                        }
                    }

                    try {
                        Pregunta preguntaActualizada = response.getBody();
                        if (preguntaActualizada != null && preguntaActualizada.getId() != null) {
                            subirArchivosOpciones(preguntaActualizada.getId());
                        }
                    } catch (Exception ex) {
                        logger.error("Error al subir archivos de opciones después de guardar pregunta", ex);
                    }

                    cargarPreguntas();
                    mostrarEstado("Pregunta actualizada correctamente" +
                            (archivoSeleccionado != null ? " - Archivo adjuntado" : ""), Color.GREEN);
                } else {
                    mostrarEstado("Error actualizando pregunta: " + response.getStatusCode(), Color.RED);
                }
            }
        } catch (HttpClientErrorException e) {
            mostrarEstado("Error del servidor: " + e.getResponseBodyAsString(), Color.RED);
        } catch (Exception e) {
            mostrarEstado("Error al guardar: " + e.getMessage(), Color.RED);
            logger.error("Error en guardarPregunta", e);
        }
    }

    @FXML
    private void adjuntarArchivo() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar Archivo");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Todos los archivos", "*.*"),
                new FileChooser.ExtensionFilter("Archivos de Imagen", "*.png", "*.jpg", "*.jpeg"),
                new FileChooser.ExtensionFilter("Documentos PDF", "*.pdf"),
                new FileChooser.ExtensionFilter("Documentos Word", "*.docx"));

        File file = fileChooser.showOpenDialog(btnAdjuntarArchivo.getScene().getWindow());
        if (file != null) {
            int selectedIndex = listaOpciones.getSelectionModel().getSelectedIndex();
            // Si hay una opción seleccionada y la pregunta existe en backend, subir archivo a la opción
            if (selectedIndex >= 0 && preguntaActual != null && preguntaActual.getId() != null) {
                // Obtener id de la opción según índice: intentar recuperar de la lista de opciones de la pregunta
                List<Opcion> opciones = new ArrayList<>(preguntaActual.getOpciones());
                if (selectedIndex < opciones.size() && opciones.get(selectedIndex).getId() != null) {
                    Long opcionId = opciones.get(selectedIndex).getId();
                    try {
                        org.springframework.http.HttpHeaders headers = createHeadersWithToken();
                        org.springframework.util.LinkedMultiValueMap<String, Object> body = new org.springframework.util.LinkedMultiValueMap<>();
                        body.add("file", new org.springframework.core.io.FileSystemResource(file));
                        HttpEntity<org.springframework.util.MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
                        ResponseEntity<com.rep.dto.actividad.OpcionResponse> resp = restTemplate.postForEntity(
                                API_BASE_URL + "/" + preguntaActual.getId() + "/opciones/" + opcionId + "/archivo",
                                requestEntity,
                                com.rep.dto.actividad.OpcionResponse.class);
                        if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                            lblNombreArchivo.setText("📎 " + file.getName() + " (subido a opción)");
                            lblNombreArchivo.setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold;");
                            mostrarEstado("Archivo subido a la opción: " + file.getName(), Color.GREEN);
                            // refrescar preguntas para mostrar estado
                            cargarPreguntas();
                        } else {
                            mostrarEstado("Error subiendo archivo a la opción", Color.RED);
                        }
                    } catch (Exception ex) {
                        mostrarEstado("Error al subir archivo a la opción: " + ex.getMessage(), Color.RED);
                    }
                    return;
                }
            }

            // Si no hay opción seleccionada, comportamiento por defecto: archivo para la pregunta
            archivoSeleccionado = file;
            lblNombreArchivo.setText("📎 " + file.getName() + " (pendiente de subir)");
            lblNombreArchivo.setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold;");
            linkArchivo.setVisible(false); // Ocultar link anterior hasta que se guarde
            mostrarEstado("Archivo seleccionado. Presione 'Guardar Cambios' para subir: " + file.getName(), Color.BLUE);
        }
    }

    @FXML
    private void verArchivo() {
        if (preguntaActual == null || preguntaActual.getArchivoUrl() == null) {
            mostrarEstado("No hay archivo para ver", Color.RED);
            return;
        }

        File file = new File(preguntaActual.getArchivoUrl());
        if (!file.exists()) {
            mostrarEstado("Archivo no encontrado: " + preguntaActual.getArchivoUrl(), Color.RED);
            return;
        }

        // Ejecutar apertura en hilo separado para evitar bloquear JavaFX
        new Thread(() -> {
            String uri = file.toURI().toString();
            try {
                javafx.application.HostServices hs = null;
                if (btnAdjuntarArchivo != null && btnAdjuntarArchivo.getScene() != null
                        && btnAdjuntarArchivo.getScene().getWindow() != null) {
                    Object ud = btnAdjuntarArchivo.getScene().getWindow().getUserData();
                    if (ud instanceof javafx.application.HostServices) {
                        hs = (javafx.application.HostServices) ud;
                    }
                }

                if (hs != null) {
                    hs.showDocument(uri);
                    Platform.runLater(() -> mostrarEstado("Archivo abierto", Color.GREEN));
                    return;
                }

                // Fallback: intentar Desktop.browse
                try {
                    if (java.awt.Desktop.isDesktopSupported()) {
                        java.awt.Desktop.getDesktop().browse(file.toURI());
                        Platform.runLater(() -> mostrarEstado("Archivo abierto", Color.GREEN));
                        return;
                    }
                } catch (Exception ex) {
                    // Ignorar e intentar comando del sistema
                }

                // Usar path absoluto para comandos del sistema (xdg-open/open/rundll32)
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("linux")) {
                    Runtime.getRuntime().exec(new String[] {"xdg-open", file.getAbsolutePath()});
                } else if (os.contains("mac")) {
                    Runtime.getRuntime().exec(new String[] {"open", file.getAbsolutePath()});
                } else if (os.contains("win")) {
                    Runtime.getRuntime().exec(new String[] {"rundll32", "url.dll,FileProtocolHandler", file.getAbsolutePath()});
                } else {
                    Platform.runLater(() -> mostrarEstado("No se pudo abrir el archivo en esta plataforma", Color.RED));
                    return;
                }

                Platform.runLater(() -> mostrarEstado("Comando de apertura enviado", Color.GREEN));
            } catch (Exception e) {
                Platform.runLater(() -> mostrarEstado("Error al abrir archivo: " + e.getMessage(), Color.RED));
            }
        }, "open-file-thread").start();
    }

    private boolean subirArchivo(Long preguntaId, File file) {
        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(file));
            HttpHeaders headers = createHeadersWithToken();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<PreguntaResponse> response = restTemplate.exchange(
                    API_BASE_URL + "/" + preguntaId + "/archivo",
                    HttpMethod.POST,
                    requestEntity,
                    PreguntaResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                PreguntaResponse preguntaActualizada = response.getBody();
                if (preguntaActual != null) {
                    preguntaActual.setArchivoUrl(preguntaActualizada.getArchivoUrl());
                    preguntaActual.setArchivoTipo(preguntaActualizada.getArchivoTipo());
                }
                archivoSeleccionado = null;
                mostrarEstado("Archivo subido exitosamente: " + file.getName(), Color.GREEN);
                return true;
            } else {
                logger.warn("Subida de archivo para pregunta {} devolvió {}", preguntaId, response.getStatusCode());
            }
            return false;
        } catch (org.springframework.web.client.HttpClientErrorException he) {
            String body = he.getResponseBodyAsString();
            logger.error("HTTP error subiendo archivo para pregunta {}: {} - {}", preguntaId, he.getStatusCode(), body);
            mostrarEstado("Error al subir archivo: " + (body != null && !body.isEmpty() ? body : he.getMessage()), Color.RED);
            return false;
        } catch (Exception e) {
            mostrarEstado("Error al subir archivo: " + e.getMessage(), Color.RED);
            logger.error("Error inesperado al subir archivo", e);
            return false;
        }
    }

    private void subirArchivosOpciones(Long preguntaId) {
        if (archivosOpciones == null || archivosOpciones.isEmpty()) return;

        for (Map.Entry<Integer, File> entry : new HashMap<>(archivosOpciones).entrySet()) {
            try {
                MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                body.add("file", new FileSystemResource(entry.getValue()));
                body.add("opcionIndex", Integer.toString(entry.getKey()));

                HttpHeaders headers = createHeadersWithToken();
                headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

                ResponseEntity<String> response = restTemplate.exchange(
                        API_BASE_URL + "/" + preguntaId + "/opciones/archivo",
                        HttpMethod.POST,
                        requestEntity,
                        String.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    logger.info("Archivo subido para opción índice {}", entry.getKey());
                    // Actualizar mapas locales para reflejar archivo existente
                    int idx = entry.getKey();
                    String nombre = entry.getValue().getName();
                    archivosOpcionesExistentes.put(idx, nombre);

                    // Actualizar texto en la lista de opciones para mostrar el icono
                    if (idx >= 0 && idx < opcionesList.size()) {
                        String texto = opcionesList.get(idx);
                        // remover indicador previo si existe
                        texto = texto.replaceAll(" 📎 \\[[^\\]]*\\]", "");
                        // mantener marcador (Correcta)
                        boolean correcta = texto.contains("(Correcta)");
                        texto = texto.replace(" (Correcta)", "").trim();
                        String nuevo = texto + " 📎 [" + nombre + "]" + (correcta ? " (Correcta)" : "");
                        opcionesList.set(idx, nuevo);
                    }
                } else {
                    logger.warn("No se pudo subir archivo para opción índice {}: {} - {}", entry.getKey(), response.getStatusCode(), response.getBody());
                    Platform.runLater(() -> mostrarEstado("No se pudo subir archivo para opción índice " + entry.getKey(), Color.RED));
                }
            } catch (Exception e) {
                if (e instanceof org.springframework.web.client.HttpClientErrorException) {
                    org.springframework.web.client.HttpClientErrorException he = (org.springframework.web.client.HttpClientErrorException) e;
                    String body = he.getResponseBodyAsString();
                    logger.error("HTTP error subiendo archivo opción {}: {} - {}", entry.getKey(), he.getStatusCode(), body);
                    Platform.runLater(() -> mostrarEstado("Error subiendo archivo opción: " + (body != null && !body.isEmpty() ? body : he.getMessage()), Color.RED));
                } else {
                    logger.error("Error subiendo archivo para opción index {}", entry.getKey(), e);
                    Platform.runLater(() -> mostrarEstado("Error subiendo archivo para opción: " + e.getMessage(), Color.RED));
                }
            }
        }

        // Limpiar mapa después de intentar subir
        archivosOpciones.clear();
    }

    private boolean validarFormulario() {
        if (campoPregunta.getText().isEmpty()) {
            mostrarEstado("El enunciado no puede estar vacío", Color.RED);
            return false;
        }

        if (tipoComboBox.getValue() == null) {
            mostrarEstado("Seleccione un tipo de pregunta", Color.RED);
            return false;
        }

        switch (tipoComboBox.getValue()) {
            case OPCION_MULTIPLE:
                if (opcionesList.isEmpty()) {
                    mostrarEstado("Debe agregar al menos una opción", Color.RED);
                    return false;
                }
                break;

            case VERDADERO_FALSO:
                if (toggleGroupVF.getSelectedToggle() == null) {
                    mostrarEstado("Seleccione si la respuesta correcta es Verdadero o Falso", Color.RED);
                    return false;
                }
                break;

            case RESPUESTA_ABIERTA:
                if (spinnerLongitud.getValue() <= 0) {
                    mostrarEstado("La longitud máxima debe ser mayor que cero", Color.RED);
                    return false;
                }
                break;
        }

        return true;
    }

    private List<OpcionRequest> convertirOpcionesUIaDTO() {
        List<OpcionRequest> opciones = new ArrayList<>();
        for (int i = 0; i < opcionesList.size(); i++) {
            String opcionTexto = opcionesList.get(i);
            OpcionRequest opcion = new OpcionRequest();

            // Limpiar texto (remover indicador de archivo)
            String textoLimpio = opcionTexto.replace(" (Correcta)", "").replaceAll(" 📎 \\[.*?\\]", "");
            opcion.setTexto(textoLimpio);
            opcion.setEsCorrecta(opcionTexto.endsWith(" (Correcta)"));

            // Si hay archivo asociado
            if (archivosOpciones.containsKey(i)) {
                opcion.setTieneArchivo(true);
                opcion.setNombreArchivo(archivosOpciones.get(i).getName());
            } else if (archivosOpcionesExistentes.containsKey(i)) {
                opcion.setTieneArchivo(true);
                opcion.setNombreArchivo(archivosOpcionesExistentes.get(i));
            }

            opciones.add(opcion);
        }
        return opciones;
    }

    // Mostrar detalles (archivo, texto limpio, correcta) cuando se selecciona una opción en la lista
    private void mostrarDetallesOpcionSeleccionada() {
        int selectedIndex = listaOpciones.getSelectionModel().getSelectedIndex();
        if (selectedIndex < 0) return;

        String textoOpcion = opcionesList.get(selectedIndex);

        // Detectar indicador de archivo en el texto
        boolean tieneArchivo = textoOpcion.contains("📎") || archivosOpciones.containsKey(selectedIndex) || archivosOpcionesExistentes.containsKey(selectedIndex);

        if (tieneArchivo) {
            // Obtener nombre del archivo desde los mapas si está disponible
            if (archivosOpciones.containsKey(selectedIndex)) {
                lblArchivoOpcion.setText(archivosOpciones.get(selectedIndex).getName());
            } else if (archivosOpcionesExistentes.containsKey(selectedIndex)) {
                lblArchivoOpcion.setText(archivosOpcionesExistentes.get(selectedIndex));
            } else {
                // Extraer de la cadena si no está en mapas
                int inicio = textoOpcion.indexOf("[") + 1;
                int fin = textoOpcion.indexOf("]", inicio);
                if (inicio > 0 && fin > inicio) {
                    lblArchivoOpcion.setText(textoOpcion.substring(inicio, fin).replace("📎", "").trim());
                } else {
                    lblArchivoOpcion.setText("Ningún archivo seleccionado");
                }
            }
            panelArchivoOpcion.setVisible(true);
        } else {
            panelArchivoOpcion.setVisible(false);
            lblArchivoOpcion.setText("Ningún archivo seleccionado");
        }

        // Marcar si es correcta y cargar texto limpio en el campo
        checkCorrecta.setSelected(textoOpcion.contains("Correcta") || textoOpcion.contains("✓") );
        String textoLimpio = textoOpcion.replaceAll(" \\[[^\"]*\\]", "").replace(" (Correcta)", "").replace(" ✓", "");
        campoNuevaOpcion.setText(textoLimpio.trim());
    }

    private void actualizarTextoOpcionEnLista() {
        int selectedIndex = listaOpciones.getSelectionModel().getSelectedIndex();
        if (selectedIndex < 0) return;

        String textoLimpio = campoNuevaOpcion.getText().trim();
        StringBuilder nuevoTexto = new StringBuilder(textoLimpio);

        if (archivosOpciones.containsKey(selectedIndex)) {
            nuevoTexto.append(" 📎 [").append(archivosOpciones.get(selectedIndex).getName()).append("]");
        } else if (archivosOpcionesExistentes.containsKey(selectedIndex)) {
            nuevoTexto.append(" 📎 [").append(archivosOpcionesExistentes.get(selectedIndex)).append("]");
        }

        if (checkCorrecta.isSelected()) {
            nuevoTexto.append(" (Correcta)");
        }

        opcionesList.set(selectedIndex, nuevoTexto.toString());
    }

    private void reindexarArchivosOpciones(int indiceEliminado) {
        Map<Integer, File> nuevoMapa = new HashMap<>();
        for (Map.Entry<Integer, File> entry : archivosOpciones.entrySet()) {
            int oldIndex = entry.getKey();
            if (oldIndex > indiceEliminado) {
                nuevoMapa.put(oldIndex - 1, entry.getValue());
            } else if (oldIndex < indiceEliminado) {
                nuevoMapa.put(oldIndex, entry.getValue());
            }
        }
        archivosOpciones = nuevoMapa;

        Map<Integer, String> nuevoExistentes = new HashMap<>();
        for (Map.Entry<Integer, String> entry : archivosOpcionesExistentes.entrySet()) {
            int oldIndex = entry.getKey();
            if (oldIndex > indiceEliminado) {
                nuevoExistentes.put(oldIndex - 1, entry.getValue());
            } else if (oldIndex < indiceEliminado) {
                nuevoExistentes.put(oldIndex, entry.getValue());
            }
        }
        archivosOpcionesExistentes = nuevoExistentes;
    }

    private void cargarOpcionesExistentes(List<Opcion> opciones) {
        opcionesList.clear();
        archivosOpciones.clear();
        archivosOpcionesExistentes.clear();

        for (int i = 0; i < opciones.size(); i++) {
            Opcion opcion = opciones.get(i);
            StringBuilder textoOpcion = new StringBuilder(opcion.getTexto());
            if (opcion.getTieneArchivo() != null && opcion.getTieneArchivo() && opcion.getNombreArchivo() != null) {
                textoOpcion.append(" 📎 [").append(opcion.getNombreArchivo()).append("]");
                archivosOpcionesExistentes.put(i, opcion.getNombreArchivo());
            }
            if (opcion.getEsCorrecta()) {
                textoOpcion.append(" (Correcta)");
            }
            opcionesList.add(textoOpcion.toString());
        }
    }

    private void configurarComboboxTipo() {
        tipoComboBox.setItems(FXCollections.observableArrayList(TipoPregunta.values()));
        tipoComboBox.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> actualizarInterfazSegunTipo());
    }

    private void actualizarInterfazSegunTipo() {
        TipoPregunta tipo = tipoComboBox.getValue();
        if (tipo == null)
            return;

        boolean esOpcionMultiple = tipo == TipoPregunta.OPCION_MULTIPLE;
        boolean esRespuestaAbierta = tipo == TipoPregunta.RESPUESTA_ABIERTA;

        // Mostrar/ocultar componentes de opciones múltiples
        panelOpciones.setVisible(esOpcionMultiple);
        panelOpciones.setManaged(esOpcionMultiple);

        // Mostrar/ocultar spinner de longitud solo para respuesta abierta
        if (spinnerLongitud != null) {
            spinnerLongitud.setVisible(esRespuestaAbierta);
            spinnerLongitud.setManaged(esRespuestaAbierta);
        }

        // Configurar mensajes específicos por tipo
        switch (tipo) {
            case OPCION_MULTIPLE:
                statusLabel.setText("Pregunta de opción múltiple - agregue las opciones posibles");
                break;
            case RESPUESTA_ABIERTA:
                statusLabel.setText("Pregunta de respuesta abierta - el estudiante escribirá su respuesta");
                break;
            case VERDADERO_FALSO:
                statusLabel.setText("Pregunta de verdadero/falso - el estudiante seleccionará entre las dos opciones");
                break;
        }

        // Limpiar opciones si no es de tipo múltiple
        if (!esOpcionMultiple) {
            opcionesList.clear();
        }
    }

    @FXML
    private void agregarOpcion() {
        String opcionTexto = campoNuevaOpcion.getText().trim();
        if (opcionTexto.isEmpty()) {
            mostrarEstado("La opción no puede estar vacía", Color.RED);
            return;
        }

        // Preparar texto con indicador de archivo si se seleccionó desde el botón
        if (archivoOpcionActual != null) {
            opcionTexto += " 📎 [" + archivoOpcionActual.getName() + "]";
        }

        if (checkCorrecta.isSelected()) {
            opcionTexto += " (Correcta)";
        }

        // Agregar a la lista
        int newIndex = opcionesList.size();
        opcionesList.add(opcionTexto);

        // Asociar archivo si existía
        if (archivoOpcionActual != null) {
            archivosOpciones.put(newIndex, archivoOpcionActual);
        }

        // Limpiar campos y estado de archivo temporal
        campoNuevaOpcion.clear();
        checkCorrecta.setSelected(false);
        eliminarArchivoOpcion();
        mostrarEstado("Opción agregada", Color.GREEN);
    }

    @FXML
    private void eliminarOpcion() {
        int selectedIndex = listaOpciones.getSelectionModel().getSelectedIndex();
        if (selectedIndex >= 0) {
            // Eliminar archivos asociados y reajustar índices
            archivosOpciones.remove(selectedIndex);
            archivosOpcionesExistentes.remove(selectedIndex);
            opcionesList.remove(selectedIndex);
            reindexarArchivosOpciones(selectedIndex);
            mostrarEstado("Opción eliminada", Color.GREEN);
        } else {
            mostrarEstado("Seleccione una opción para eliminar", Color.RED);
        }
    }

    @FXML
    private void adjuntarArchivoAOpcion() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar archivo para la opción");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg", "*.gif"),
                new FileChooser.ExtensionFilter("Documentos", "*.pdf", "*.doc", "*.docx", "*.txt"),
                new FileChooser.ExtensionFilter("Todos los archivos", "*.*")
        );

        archivoOpcionActual = fileChooser.showOpenDialog(btnAdjuntarArchivoOpcion.getScene().getWindow());
        if (archivoOpcionActual != null) {
            lblArchivoOpcion.setText(archivoOpcionActual.getName());
            panelArchivoOpcion.setVisible(true);
        }
    }

    @FXML
    private void eliminarArchivoOpcion() {
        archivoOpcionActual = null;
        lblArchivoOpcion.setText("Ningún archivo seleccionado");
        if (panelArchivoOpcion != null) panelArchivoOpcion.setVisible(false);
    }

    private void mostrarEstado(String mensaje, Color color) {
        if (statusLabel != null) {
            statusLabel.setTextFill(color);
            statusLabel.setText(mensaje);
        }
    }

    private void configurarListeners() {
        // Configurar listener para la lista de opciones
        listaOpciones.setItems(opcionesList);
        listaOpciones.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> mostrarDetallesOpcionSeleccionada());

        // Configurar acciones de los botones
        btnAgregarOpcion.setOnAction(e -> agregarOpcion());
        btnEliminarOpcion.setOnAction(e -> eliminarOpcion());

        // Configurar listener para cambios en el tipo de pregunta
        tipoComboBox.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> actualizarInterfazSegunTipo());

        // Configurar listener para cambios en la selección de la tabla
        preguntasTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> cargarPreguntaSeleccionada(newSelection));
    }
}