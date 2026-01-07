package com.rep.controller.views;

import com.rep.dto.tokens.JwtTokenHolder;
import com.rep.model.RespuestaEstudiante;
import com.rep.model.RespuestaPregunta;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import javafx.scene.paint.Color;

@Controller
public class DetalleRespuestaController {

    @FXML
    private Label estudianteLabel;
    @FXML
    private TextField notaField;
    @FXML
    private TableView<RespuestaPregunta> respuestasTable;
    @FXML
    private TableColumn<RespuestaPregunta, String> colPregunta;
    @FXML
    private TableColumn<RespuestaPregunta, String> colRespuesta;
    @FXML
    private TableColumn<RespuestaPregunta, String> colCorrecta;
    @FXML
    private TextArea observacionesArea;
    @FXML
    private Label statusLabel;

    private Long actividadId;
    private Long estudianteId;
    private JwtTokenHolder jwtTokenHolder;
    private final RestTemplate restTemplate = new RestTemplate();
    private final String API_URL = "http://localhost:8080/api/actividades";

    @FXML
    public void initialize() {
        colPregunta.setCellValueFactory(
                cellData -> new SimpleStringProperty(cellData.getValue().getPregunta().getEnunciado()));

        colRespuesta.setCellValueFactory(cellData -> {
            if (cellData.getValue().getOpcion() != null) {
                return new SimpleStringProperty(cellData.getValue().getOpcion().getTexto());
            } else if (cellData.getValue().getRespuestaAbierta() != null) {
                return new SimpleStringProperty(cellData.getValue().getRespuestaAbierta());
            }
            return new SimpleStringProperty("Sin responder");
        });

        colCorrecta.setCellValueFactory(cellData -> {
            return new SimpleStringProperty(
                    cellData.getValue().getPregunta().getOpciones().stream()
                            .filter(op -> op.getEsCorrecta())
                            .findFirst()
                            .map(op -> op.getTexto())
                            .orElse("N/A"));
        });
    }

    public void setDatos(Long actividadId, Long estudianteId, JwtTokenHolder jwtTokenHolder) {
        this.actividadId = actividadId;
        this.estudianteId = estudianteId;
        this.jwtTokenHolder = jwtTokenHolder;
        cargarDetalle();
    }

    private void cargarDetalle() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + jwtTokenHolder.getToken());
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<RespuestaEstudiante> response = restTemplate.exchange(
                    API_URL + "/" + actividadId + "/respuestas/" + estudianteId,
                    HttpMethod.GET,
                    entity,
                    RespuestaEstudiante.class // Backend returns detailed entity with answers
            );

            if (response.getBody() != null) {
                RespuestaEstudiante re = response.getBody();
                estudianteLabel.setText("Estudiante: " + re.getEstudiante().getNombre());
                notaField.setText(String.valueOf(re.getNota()));
                observacionesArea.setText(re.getObservaciones());
                if (re.getRespuestasPreguntas() != null) {
                    respuestasTable.setItems(FXCollections.observableArrayList(re.getRespuestasPreguntas()));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void guardarCambios() {
        try {
            float nota = Float.parseFloat(notaField.getText());
            if (nota < 0 || nota > 5.0) { // Assuming scale 0-5
                mostrarEstado("La nota debe estar entre 0.0 y 5.0", Color.RED);
                return;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + jwtTokenHolder.getToken());
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
            map.add("nota", nota);
            map.add("observaciones", observacionesArea.getText());

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(map, headers);

            restTemplate.exchange(
                    API_URL + "/" + actividadId + "/notas/" + estudianteId,
                    HttpMethod.PUT,
                    entity,
                    RespuestaEstudiante.class);

            mostrarEstado("Calificación guardada correctamente", Color.GREEN);

        } catch (NumberFormatException e) {
            mostrarEstado("La nota debe ser un número válido", Color.RED);
        } catch (Exception e) {
            mostrarEstado("Error al guardar: " + e.getMessage(), Color.RED);
            e.printStackTrace();
        }
    }

    private void mostrarEstado(String mensaje, Color color) {
        if (statusLabel != null) {
            statusLabel.setTextFill(color);
            statusLabel.setText(mensaje);
        }
    }
}
