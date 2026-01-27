package com.rep.controller.views;

import com.rep.dto.auth.RegistroUsuarioDTO;
import com.rep.model.Curso;
import com.rep.model.Usuario;
import com.rep.service.ApiClientService;
import com.rep.service.funciones.RegistrationServiceClient;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class RegistroController {
    @FXML private TextField nombre;
    @FXML private TextField correo;
    @FXML private TextField identificacion;
    @FXML private ComboBox<String> tipoIdentificacion;
    @FXML private PasswordField contraseña;
    @FXML private PasswordField confirmarContrasena;
    @FXML private TextField edad;
    @FXML private ComboBox<String> curso;
    @FXML private HBox cursoBox;
    @FXML private ComboBox<String> sexo;
    private final RegistrationServiceClient registrationService;
    private final ApiClientService apiClient;

    public RegistroController(RegistrationServiceClient registrationService,
                              ApiClientService apiClient) {
        this.registrationService = registrationService;
        this.apiClient = apiClient;
    }

    @FXML
    public void initialize() {
        cargarCursosDisponibles();
        sexo.getItems().addAll(
                Usuario.Sexo.MASCULINO.getDescripcion(),
                Usuario.Sexo.FEMENINO.getDescripcion(),
                Usuario.Sexo.OTRO.getDescripcion());
    }

    @FXML
    private void registrar() {
        if (!validarCampos()) {
            return;
        }

        RegistroUsuarioDTO usuarioDTO = crearUsuarioDesdeFormulario();

        try {
            ResponseEntity<?> response = registrationService.registerUser(usuarioDTO);
            manejarRespuestaRegistro(response);
        } catch (Exception e) {
            mostrarAlertaError("Error de conexión", "No se pudo conectar al servidor: " + e.getMessage());
        }
    }

    private void cargarCursosDisponibles() {
        try {
            curso.setDisable(true);
            curso.setPromptText("Cargando cursos...");
            curso.getItems().clear();

            ResponseEntity<List<Curso>> response = apiClient.get(
                    "/public/cursos",  // Nuevo endpoint público
                    new ParameterizedTypeReference<List<Curso>>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Curso> cursosOrdenados = response.getBody().stream()
                        .sorted((c1, c2) -> {
                            int cmpGrado = Integer.compare(c1.getGrado(), c2.getGrado());
                            return cmpGrado != 0 ? cmpGrado : c1.getGrupo().compareTo(c2.getGrupo());
                        })
                        .collect(Collectors.toList());

                cursosOrdenados.forEach(c -> {
                    String nombreMostrar = String.format("%d° %s", c.getGrado(), c.getGrupo());
                    curso.getItems().add(nombreMostrar);
                });

                if (!curso.getItems().isEmpty()) {
                    curso.getSelectionModel().selectFirst();
                }
            } else {
                curso.setPromptText("No se pudieron cargar los cursos");
            }
        } catch (Exception e) {
            curso.setPromptText("Error al cargar cursos");
            mostrarAlertaError("Error", "No se pudieron cargar los cursos: " + e.getMessage());
        } finally {
            curso.setDisable(false);
        }
    }

    private Long obtenerIdCursoPorNombre(String nombreMostrado) {
        try {
            ResponseEntity<List<Curso>> response = apiClient.get(
                    "/public/cursos",  // Usar el nuevo endpoint público
                    new ParameterizedTypeReference<List<Curso>>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String[] partes = nombreMostrado.split("° ");
                int grado = Integer.parseInt(partes[0]);
                String grupo = partes[1];

                return response.getBody().stream()
                        .filter(c -> c.getGrado() == grado && c.getGrupo().equals(grupo))
                        .findFirst()
                        .map(Curso::getId)
                        .orElse(null);
            }
        } catch (Exception e) {
            mostrarAlertaError("Error", "No se pudo verificar el curso seleccionado");
        }
        return null;
    }

    private boolean validarCampos() {
        if (nombre.getText().isEmpty() || correo.getText().isEmpty() ||
                identificacion.getText().isEmpty() || contraseña.getText().isEmpty()) {
            mostrarAlertaError("Campos vacíos", "Todos los campos obligatorios deben estar completos");
            return false;
        }
        if (sexo.getValue() == null) {
            mostrarAlertaError("Sexo requerido", "Debe seleccionar un sexo");
            return false;
        }


        if (!contraseña.getText().equals(confirmarContrasena.getText())) {
            mostrarAlertaError("Contraseñas no coinciden", "Las contraseñas ingresadas deben ser iguales");
            return false;
        }

        try {
            if (edad.getText().isEmpty()) {
                mostrarAlertaError("Edad requerida", "La edad es obligatoria para estudiantes");
                return false;
            }
            Integer.parseInt(edad.getText());
        } catch (NumberFormatException e) {
            mostrarAlertaError("Edad inválida", "La edad debe ser un número");
            return false;
        }

        if (curso.getValue() == null) {
            mostrarAlertaError("Curso requerido", "Debe seleccionar un curso");
            return false;
        }

        return true;
    }

    private RegistroUsuarioDTO crearUsuarioDesdeFormulario() {
        RegistroUsuarioDTO dto = new RegistroUsuarioDTO();
        dto.setNombre(nombre.getText());
        dto.setCorreo(correo.getText());
        dto.setIdentificacion(identificacion.getText());
        dto.setTipoIdentificacion(tipoIdentificacion.getValue());
        dto.setContraseña(contraseña.getText());
        dto.setRol("ESTUDIANTE"); // Rol fijo como estudiante
        dto.setEdad(Integer.parseInt(edad.getText()));
        dto.setCursoId(obtenerIdCursoPorNombre(curso.getValue()));
        dto.setActivo(true); // Por defecto activo
        Long cursoId = obtenerIdCursoPorNombre(curso.getValue());
        dto.setSexo(Usuario.Sexo.valueOf(sexo.getValue().toUpperCase()));
        if (cursoId != null) {
            dto.setCursoId(cursoId);
        }
        return dto;
    }

    private void manejarRespuestaRegistro(ResponseEntity<?> response) {
        if (response.getStatusCode() == HttpStatus.CREATED) {
            mostrarAlertaExito("Registro exitoso", "Estudiante registrado correctamente");
            limpiarFormulario();
        } else {
            String mensajeError = response.getBody() != null ?
                    response.getBody().toString() :
                    "Error en el servidor (Código: " + response.getStatusCodeValue() + ")";

            if (mensajeError.contains("El correo electrónico ya está registrado")) {
                mensajeError = "El correo electrónico ya está en uso";
            } else if (mensajeError.contains("La identificación ya está registrada")) {
                mensajeError = "La identificación ya está registrada";
            }

            mostrarAlertaError("Error en registro", mensajeError);
        }
    }

    private void mostrarAlertaExito(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarAlertaError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(titulo);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void limpiarFormulario() {
        nombre.clear();
        correo.clear();
        identificacion.clear();
        tipoIdentificacion.getSelectionModel().clearSelection();
        contraseña.clear();
        confirmarContrasena.clear();
        edad.clear();
        curso.getSelectionModel().clearSelection();
        sexo.getSelectionModel().clearSelection();
    }
}