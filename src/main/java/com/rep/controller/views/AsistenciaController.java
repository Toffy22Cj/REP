package com.rep.controller.views;

import com.rep.dto.asistencia.AsistenciaDTO;
import com.rep.dto.curso.CursoDTO;
import com.rep.dto.tokens.JwtTokenHolder;
import com.rep.model.Asistencia;
import com.rep.model.Estudiante;
import com.rep.model.Materia;
import com.rep.model.Usuario;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AsistenciaController {

    @FXML
    private ComboBox<CursoDTO> cursoComboBox;
    @FXML
    private ComboBox<Materia> materiaComboBox;
    @FXML
    private DatePicker fechaPicker;
    @FXML
    private TableView<AsistenciaDTO> asistenciaTable;
    @FXML
    private TableColumn<AsistenciaDTO, String> estudianteCol;
    @FXML
    private TableColumn<AsistenciaDTO, String> estadoCol;
    @FXML
    private TableColumn<AsistenciaDTO, String> observacionCol;
    @FXML
    private Button guardarButton;
    @FXML
    private Label statusLabel;

    private JwtTokenHolder jwtTokenHolder;
    private final RestTemplate restTemplate = new RestTemplate();
    private final String API_URL = "http://localhost:8080/api/profesor";

    public void setJwtTokenHolder(JwtTokenHolder jwtTokenHolder) {
        this.jwtTokenHolder = jwtTokenHolder;
    }

    @FXML
    private ListView<LocalDate> historialList;

    public void initialize() {
        fechaPicker.setValue(LocalDate.now());

        // Setup columns
        estudianteCol.setCellValueFactory(new PropertyValueFactory<>("estudianteNombre"));
        estadoCol.setCellValueFactory(new PropertyValueFactory<>("estado"));
        observacionCol.setCellValueFactory(new PropertyValueFactory<>("observacion"));

        // Custom cell for Status (ComboBox)
        estadoCol.setCellFactory(col -> new TableCell<AsistenciaDTO, String>() {
            private final ComboBox<String> comboBox = new ComboBox<>(
                    FXCollections.observableArrayList("PRESENTE", "NO_INGRESO", "EXCUSA"));
            {
                comboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                    if (getTableRow() != null && getTableRow().getItem() != null) {
                        getTableRow().getItem().setEstado(newVal);
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    comboBox.setValue(item);
                    setGraphic(comboBox);
                }
            }
        });

        // Custom cell for Observation (TextField)
        observacionCol.setCellFactory(col -> new TableCell<AsistenciaDTO, String>() {
            private final TextField textField = new TextField();
            {
                textField.textProperty().addListener((obs, oldVal, newVal) -> {
                    if (getTableRow() != null && getTableRow().getItem() != null) {
                        getTableRow().getItem().setObservacion(newVal);
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    textField.setText(item);
                    setGraphic(textField);
                }
            }
        });

        // History selection listener
        historialList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                fechaPicker.setValue(newVal);
                cargarLista();
            }
        });

        // Auto-load history when course/materia selected
        cursoComboBox.valueProperty().addListener((obs, o, n) -> cargarHistorial());
        materiaComboBox.valueProperty().addListener((obs, o, n) -> cargarHistorial());
    }

    @FXML
    private void cargarHistorial() {
        CursoDTO curso = cursoComboBox.getValue();
        Materia materia = materiaComboBox.getValue();

        if (curso == null || materia == null) {
            historialList.getItems().clear();
            return;
        }

        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            String url = API_URL + "/asistencia/fechas?cursoId=" + curso.getId() + "&materiaId=" + materia.getId();
            ResponseEntity<List<LocalDate>> resp = restTemplate.exchange(
                    url, HttpMethod.GET, entity, new ParameterizedTypeReference<List<LocalDate>>() {
                    });

            if (resp.getBody() != null) {
                historialList.setItems(FXCollections.observableArrayList(resp.getBody()));
            }
        } catch (Exception e) {
            System.err.println("Error loading history: " + e.getMessage());
        }
    }

    public void setCursos(List<CursoDTO> cursos) {
        cursoComboBox.setItems(FXCollections.observableArrayList(cursos));
    }

    public void setMaterias(List<Materia> materias) {
        materiaComboBox.setItems(FXCollections.observableArrayList(materias));
    }

    @FXML
    private void cargarLista() {
        CursoDTO curso = cursoComboBox.getValue();
        Materia materia = materiaComboBox.getValue();
        LocalDate fecha = fechaPicker.getValue();

        if (curso == null || materia == null || fecha == null) {
            statusLabel.setText("Seleccione curso, materia y fecha.");
            statusLabel.setTextFill(Color.RED);
            return;
        }

        try {
            // Fetch students first
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // 1. Get Students
            String studentsUrl = API_URL + "/estudiantes/curso/" + curso.getId();
            ResponseEntity<List<Estudiante>> studentsResp = restTemplate.exchange(
                    studentsUrl, HttpMethod.GET, entity, new ParameterizedTypeReference<List<Estudiante>>() {
                    });

            List<Estudiante> estudiantes = studentsResp.getBody();
            if (estudiantes == null)
                estudiantes = new ArrayList<>();

            // 2. Get Existing Attendance
            String attendanceUrl = API_URL + "/asistencia?cursoId=" + curso.getId() +
                    "&materiaId=" + materia.getId() + "&fecha=" + fecha.toString();
            ResponseEntity<List<Asistencia>> attResp = restTemplate.exchange(
                    attendanceUrl, HttpMethod.GET, entity, new ParameterizedTypeReference<List<Asistencia>>() {
                    });

            List<Asistencia> asistencias = attResp.getBody();
            if (asistencias == null)
                asistencias = new ArrayList<>();

            // Merge
            List<AsistenciaDTO> rows = new ArrayList<>();
            for (Estudiante est : estudiantes) {
                AsistenciaDTO dto = new AsistenciaDTO();
                dto.setEstudianteId(est.getId());

                dto.setEstudianteNombre(est.getNombre());

                // Find existing
                Asistencia existing = asistencias.stream()
                        .filter(a -> a.getEstudiante() != null && a.getEstudiante().getId().equals(est.getId()))
                        .findFirst().orElse(null);

                if (existing != null) {
                    dto.setId(existing.getId());
                    dto.setEstado(existing.getEstado().name());
                    dto.setObservacion(existing.getObservacion());
                } else {
                    dto.setEstado("PRESENTE"); // Default
                    dto.setObservacion("");
                }
                rows.add(dto);
            }

            asistenciaTable.setItems(FXCollections.observableArrayList(rows));
            statusLabel.setText("Lista cargada.");
            statusLabel.setTextFill(Color.GREEN);

        } catch (Exception e) {
            statusLabel.setText("Error al cargar: " + e.getMessage());
            statusLabel.setTextFill(Color.RED);
            e.printStackTrace();
        }
    }

    @FXML
    private void guardarAsistencia() {
        CursoDTO curso = cursoComboBox.getValue();
        Materia materia = materiaComboBox.getValue();
        LocalDate fecha = fechaPicker.getValue();
        List<AsistenciaDTO> items = asistenciaTable.getItems();

        if (items == null || items.isEmpty()) {
            return;
        }

        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<List<AsistenciaDTO>> entity = new HttpEntity<>(items, headers);

            String url = API_URL + "/asistencia?cursoId=" + curso.getId() +
                    "&materiaId=" + materia.getId() + "&fecha=" + fecha.toString();

            restTemplate.postForEntity(url, entity, List.class);

            statusLabel.setText("Asistencia guardada correctamente.");
            statusLabel.setTextFill(Color.GREEN);
            cargarHistorial(); // Refresh history
        } catch (Exception e) {
            statusLabel.setText("Error al guardar: " + e.getMessage());
            statusLabel.setTextFill(Color.RED);
            e.printStackTrace();
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (jwtTokenHolder != null && jwtTokenHolder.getToken() != null) {
            String token = jwtTokenHolder.getToken();
            if (!token.startsWith("Bearer ")) {
                token = "Bearer " + token;
            }
            headers.set("Authorization", token);
        }
        return headers;
    }
}
