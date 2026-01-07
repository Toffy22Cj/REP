package com.rep.controller.views;

import com.rep.dto.actividad.ResultadoActividadDTO;
import com.rep.dto.tokens.JwtTokenHolder;
import com.rep.model.Actividad;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.FloatStringConverter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Controller
public class ResultadosActividadController {

    @FXML
    private Label tituloActividadLabel;
    @FXML
    private TableView<ResultadoActividadDTO> resultadosTable;
    @FXML
    private TableColumn<ResultadoActividadDTO, String> colEstudiante;
    @FXML
    private TableColumn<ResultadoActividadDTO, Float> colNota;
    @FXML
    private TableColumn<ResultadoActividadDTO, String> colObservaciones;
    @FXML
    private TableColumn<ResultadoActividadDTO, Void> colAcciones;

    private Actividad actividad;
    private JwtTokenHolder jwtTokenHolder;
    private final RestTemplate restTemplate = new RestTemplate();
    private final String API_URL = "http://localhost:8080/api/actividades"; // Ajustar si es necesario

    @FXML
    public void initialize() {
        configurarTabla();
    }

    public void setActividad(Actividad actividad) {
        this.actividad = actividad;
        tituloActividadLabel.setText("Resultados: " + actividad.getTitulo());
        cargarResultados();
    }

    public void setJwtTokenHolder(JwtTokenHolder jwtTokenHolder) {
        this.jwtTokenHolder = jwtTokenHolder;
    }

    private void configurarTabla() {
        colEstudiante.setCellValueFactory(new PropertyValueFactory<>("nombreEstudiante"));
        // Nota editable
        colNota.setCellValueFactory(new PropertyValueFactory<>("nota"));
        colNota.setCellFactory(TextFieldTableCell.forTableColumn(new FloatStringConverter()));
        colNota.setOnEditCommit(event -> {
            ResultadoActividadDTO dto = event.getRowValue();
            dto.setNota(event.getNewValue());
            actualizarNota(dto);
        });

        colObservaciones.setCellValueFactory(new PropertyValueFactory<>("observaciones"));
        resultadosTable.setEditable(true);

        // Acciones
        colAcciones.setCellFactory(param -> new TableCell<>() {
            private final Button btnVer = new Button("Ver Respuesta");

            {
                btnVer.setOnAction(event -> {
                    ResultadoActividadDTO dto = getTableView().getItems().get(getIndex());
                    verDetalleRespuesta(dto);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btnVer);
                }
            }
        });
    }

    private void cargarResultados() {
        if (actividad == null || jwtTokenHolder == null)
            return;

        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<List<ResultadoActividadDTO>> response = restTemplate.exchange(
                    API_URL + "/" + actividad.getId() + "/resultados",
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<ResultadoActividadDTO>>() {
                    });

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                resultadosTable.setItems(FXCollections.observableArrayList(response.getBody()));
            }
        } catch (Exception e) {
            mostrarAlerta("Error al cargar resultados: " + e.getMessage());
        }
    }

    private void actualizarNota(ResultadoActividadDTO dto) {
        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // PUT /api/actividades/{id}/notas/{estudianteId}?nota=X&observaciones=Y
            String url = String.format("%s/%d/notas/%d?nota=%.2f",
                    API_URL, actividad.getId(), dto.getEstudianteId(), dto.getNota());

            // Reemplazar coma por punto si es necesario para float en URL
            url = url.replace(",", ".");

            restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);

            // Opcional: Mostrar feedback sutil
        } catch (Exception e) {
            mostrarAlerta("Error al guardar nota: " + e.getMessage());
            // Revertir cambio en UI?
            cargarResultados(); // Recargar para asegurar consistencia
        }
    }

    private void verDetalleRespuesta(ResultadoActividadDTO dto) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/view/DetalleRespuesta.fxml"));
            javafx.scene.Parent root = loader.load();

            DetalleRespuestaController controller = loader.getController();
            controller.setDatos(actividad.getId(), dto.getEstudianteId(), jwtTokenHolder);

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Detalle Respuesta");
            stage.setScene(new javafx.scene.Scene(root, 700, 500));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error al abrir detalle: " + e.getMessage());
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + jwtTokenHolder.getToken());
        return headers;
    }

    private void mostrarAlerta(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(mensaje);
        alert.show();
    }

    @FXML
    private void exportarExcel() {
        if (resultadosTable.getItems().isEmpty()) {
            mostrarAlerta("No hay datos para exportar");
            return;
        }

        try {
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Guardar Resultados");
            fileChooser.setInitialFileName("Resultados_" + actividad.getTitulo().replaceAll("\\s+", "_") + ".csv");
            fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("CSV Files", "*.csv"));
            java.io.File file = fileChooser.showSaveDialog(resultadosTable.getScene().getWindow());

            if (file != null) {
                try (java.io.PrintWriter writer = new java.io.PrintWriter(file)) {
                    // Header
                    writer.println("Estudiante,Fecha Entrega,Nota,Observaciones");

                    // Data
                    for (ResultadoActividadDTO dto : resultadosTable.getItems()) {
                        writer.printf("%s,%s,%.2f,%s%n",
                                dto.getNombreEstudiante() != null ? dto.getNombreEstudiante()
                                        : "Estudiante " + dto.getEstudianteId(),
                                dto.getFechaEntrega() != null ? dto.getFechaEntrega() : "-",
                                dto.getNota(),
                                dto.getObservaciones() != null ? dto.getObservaciones().replace(",", ";") : "");
                    }
                    mostrarAlerta("Exportado exitosamente a " + file.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error al exportar: " + e.getMessage());
        }
    }
}