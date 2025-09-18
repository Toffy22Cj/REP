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
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URL;
import java.util.*;

@Controller
public class EditorPreguntasController implements Initializable {

    private static final String API_BASE_URL = "http://localhost:8080/api/preguntas";

    // UI Components
    @FXML
    private Spinner<Integer> spinnerLongitud;
    @FXML private VBox panelOpciones;
    @FXML private TableView<Pregunta> preguntasTable;
    @FXML private TableColumn<Pregunta, String> colEnunciado;
    @FXML private TableColumn<Pregunta, String> colTipo;
    @FXML private TextArea campoPregunta;
    @FXML private ComboBox<TipoPregunta> tipoComboBox;
    @FXML private ListView<String> listaOpciones;
    @FXML private TextField campoNuevaOpcion;
    @FXML private CheckBox checkCorrecta;
    @FXML private Button btnAgregarOpcion;
    @FXML private Button btnEliminarOpcion;
    @FXML private Button btnGuardar;
    @FXML private Button btnAgregarPregunta;
    @FXML private Button btnEliminarPregunta;
    @FXML private Label statusLabel;
    @FXML private RadioButton radioVerdadero;
    @FXML private RadioButton radioFalso;
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
                (obs, oldSelection, newSelection) -> cargarPreguntaSeleccionada(newSelection)
        );
    }

    @FXML private Label tituloLabel;

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
                    new ParameterizedTypeReference<List<Pregunta>>() {}
            );

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
                    pregunta.getOpciones().forEach(opcion -> {
                        String textoOpcion = opcion.getTexto();
                        if (opcion.getEsCorrecta()) {
                            textoOpcion += " (Correcta)";
                        }
                        opcionesList.add(textoOpcion);
                    });
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
//                if (pregunta.getLongitudMaxima() != null) {
//                    spinnerLongitud.getValueFactory().setValue(pregunta.getLongitudMaxima());
//                }
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
                        Pregunta.class
                );
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
                        Void.class
                );

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
        if (preguntaActual == null || !validarFormulario()) return;

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
                    // Configurar las opciones para verdadero/falso
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
                    // Configurar la longitud máxima para respuesta abierta
//                    request.setLongitudMaxima(spinnerLongitud.getValue());
                    break;
            }

            ResponseEntity<Pregunta> response = restTemplate.exchange(
                    API_BASE_URL + "/" + preguntaActual.getId(),
                    HttpMethod.PUT,
                    new HttpEntity<>(request, createHeadersWithToken()),
                    Pregunta.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                cargarPreguntas();
                mostrarEstado("Pregunta actualizada correctamente", Color.GREEN);

//                // Si estás usando el controlador de profesor para actualizar una vista
//                if (profesorController != null) {
//                    profesorController.actualizarVistaActividades();
//                }
            }
        } catch (HttpClientErrorException e) {
            mostrarEstado("Error del servidor: " + e.getResponseBodyAsString(), Color.RED);
        } catch (Exception e) {
            mostrarEstado("Error al actualizar: " + e.getMessage(), Color.RED);
        }
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
        for (String opcionTexto : opcionesList) {
            OpcionRequest opcion = new OpcionRequest();
            opcion.setTexto(opcionTexto.replace(" (Correcta)", ""));
            opcion.setEsCorrecta(opcionTexto.endsWith(" (Correcta)"));
            opciones.add(opcion);
        }
        return opciones;
    }

    private void configurarComboboxTipo() {
        tipoComboBox.setItems(FXCollections.observableArrayList(TipoPregunta.values()));
        tipoComboBox.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> actualizarInterfazSegunTipo()
        );
    }

    private void actualizarInterfazSegunTipo() {
        TipoPregunta tipo = tipoComboBox.getValue();
        if (tipo == null) return;

        boolean esOpcionMultiple = tipo == TipoPregunta.OPCION_MULTIPLE;

        // Mostrar/ocultar componentes de opciones múltiples
        panelOpciones.setVisible(esOpcionMultiple);
        panelOpciones.setManaged(esOpcionMultiple);

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

        if (checkCorrecta.isSelected()) {
            opcionTexto += " (Correcta)";
        }

        opcionesList.add(opcionTexto);
        campoNuevaOpcion.clear();
        checkCorrecta.setSelected(false);
        mostrarEstado("Opción agregada", Color.GREEN);
    }

    @FXML
    private void eliminarOpcion() {
        int selectedIndex = listaOpciones.getSelectionModel().getSelectedIndex();
        if (selectedIndex >= 0) {
            opcionesList.remove(selectedIndex);
            mostrarEstado("Opción eliminada", Color.GREEN);
        } else {
            mostrarEstado("Seleccione una opción para eliminar", Color.RED);
        }
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

        // Configurar acciones de los botones
        btnAgregarOpcion.setOnAction(e -> agregarOpcion());
        btnEliminarOpcion.setOnAction(e -> eliminarOpcion());

        // Configurar listener para cambios en el tipo de pregunta
        tipoComboBox.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> actualizarInterfazSegunTipo()
        );

        // Configurar listener para cambios en la selección de la tabla
        preguntasTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> cargarPreguntaSeleccionada(newSelection)
        );
    }
}