package com.rep.controller.views;

import com.rep.dto.tokens.JwtTokenHolder;
import com.rep.model.RespuestaPregunta;
import com.rep.dto.actividad.RespuestaEstudianteDetalleDTO;
import com.rep.dto.actividad.RespuestaPreguntaDetalleDTO;
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
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TableCell;
import java.awt.Desktop;
import java.io.File;
import java.nio.file.Paths;

@Controller
public class DetalleRespuestaController {

    @FXML
    private Label estudianteLabel;
    @FXML
    private TextField notaField;
    @FXML
    private TableView<RespuestaPreguntaDetalleDTO> respuestasTable;
    @FXML
    private TableColumn<RespuestaPreguntaDetalleDTO, String> colPregunta;
    @FXML
    private TableColumn<RespuestaPreguntaDetalleDTO, String> colRespuesta;
    @FXML
    private TableColumn<RespuestaPreguntaDetalleDTO, String> colCorrecta;
    @FXML
    private TableColumn<RespuestaPreguntaDetalleDTO, String> colArchivo;
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
            cellData -> new SimpleStringProperty(cellData.getValue().getEnunciado()));

        colRespuesta.setCellValueFactory(cellData -> {
            if (cellData.getValue().getOpcionTexto() != null) {
                return new SimpleStringProperty(cellData.getValue().getOpcionTexto());
            } else if (cellData.getValue().getRespuestaAbierta() != null) {
                return new SimpleStringProperty(cellData.getValue().getRespuestaAbierta());
            }
            return new SimpleStringProperty("Sin responder");
        });

        colCorrecta.setCellValueFactory(cellData -> {
            return new SimpleStringProperty(
                cellData.getValue().getEsCorrecta() != null && cellData.getValue().getEsCorrecta() ? "Sí" : "No");
        });

        colArchivo.setCellValueFactory(cellData -> {
            if (cellData.getValue().getNombreArchivo() != null && !cellData.getValue().getNombreArchivo().isEmpty()) {
                return new SimpleStringProperty(cellData.getValue().getNombreArchivo());
            }
            return new SimpleStringProperty("Sin archivo");
        });

        // Hacer la columna de archivos clickeable para descargar
        colArchivo.setCellFactory(column -> new TableCell<RespuestaPreguntaDetalleDTO, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.equals("Sin archivo")) {
                    setText(item);
                    setGraphic(null);
                    setStyle("");
                } else {
                    Hyperlink link = new Hyperlink(item);
                    link.setOnAction(e -> {
                        RespuestaPreguntaDetalleDTO respuesta = getTableView().getItems().get(getIndex());
                        DetalleRespuestaController.this.descargarArchivo(respuesta.getArchivoAdjunto(),
                                respuesta.getNombreArchivo());
                    });
                    setGraphic(link);
                    setText(null);
                }
            }
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

            ResponseEntity<RespuestaEstudianteDetalleDTO> response = restTemplate.exchange(
                    API_URL + "/" + actividadId + "/respuestas/" + estudianteId,
                    HttpMethod.GET,
                    entity,
                    RespuestaEstudianteDetalleDTO.class
            );

            if (response.getBody() != null) {
                RespuestaEstudianteDetalleDTO re = response.getBody();
                estudianteLabel.setText("Estudiante: " + (re.getNombreEstudiante() != null ? re.getNombreEstudiante() : "-"));
                notaField.setText(re.getNota() != null ? String.valueOf(re.getNota()) : "");
                observacionesArea.setText(re.getObservaciones() != null ? re.getObservaciones() : "");
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
                    Void.class);

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

    private void descargarArchivo(String rutaArchivo, String nombreArchivo) {
        if (rutaArchivo == null || rutaArchivo.isEmpty()) {
            mostrarEstado("No hay archivo adjunto", Color.RED);
            return;
        }

        try {
            File archivo = new File(rutaArchivo);
            if (archivo.exists()) {
                // Abrir el archivo con la aplicación predeterminada del sistema
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(archivo);
                    mostrarEstado("Archivo abierto: " + nombreArchivo, Color.GREEN);
                } else {
                    mostrarEstado("No se puede abrir el archivo en este sistema", Color.RED);
                }
            } else {
                mostrarEstado("Archivo no encontrado: " + nombreArchivo, Color.RED);
            }
        } catch (Exception e) {
            e.printStackTrace();
            mostrarEstado("Error al abrir archivo: " + e.getMessage(), Color.RED);
        }
    }
}
