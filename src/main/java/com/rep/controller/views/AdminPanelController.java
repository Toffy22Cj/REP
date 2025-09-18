package com.rep.controller.views;


import com.rep.dto.auth.RegistroUsuarioDTO;

import com.rep.dto.profesor.ProfesorMateriaRequest;
import com.rep.dto.tokens.JwtTokenHolder;
import com.rep.model.*;

import com.rep.service.funciones.AdminApiService;
import com.rep.service.logica.UsuarioRegistrationService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import java.util.*;

@Controller
public class AdminPanelController {
    // ----------- UI Components -----------
    // Materias section
    @FXML private TextField materiaTextField;
    @FXML private TableView<Materia> materiasTable;
    @FXML private TableColumn<Materia, Long> colMateriaId;
    @FXML private TableColumn<Materia, String> colMateriaNombre;
    @FXML private Label statusMateriaLabel;

    // Cursos section
    @FXML private TextField gradoCursoField;
    @FXML private TextField grupoCursoField;
    @FXML private Button crearCursoButton;
    @FXML private Label statusCursoLabel;
    @FXML private TableView<Curso> cursosTable;
    @FXML private TableColumn<Curso, String> colCursoNombre;
    @FXML private TableColumn<Curso, Integer> colCursoGrado;
    @FXML private TableColumn<Curso, String> colCursoGrupo;

    // Profesores section
    @FXML private TextField nombreProfesorField;
    @FXML private TextField correoProfesorField;
    @FXML private PasswordField claveProfesorField;
    @FXML private ComboBox<String> materiaAsignarCombo;
    @FXML private ComboBox<String> cursoAsignarCombo;
    @FXML private ComboBox<String> profesorAsignarCombo;
    @FXML private TableView<Usuario> profesoresTable;
    @FXML private TableColumn<Usuario, Long> colProfId;
    @FXML private TableColumn<Usuario, String> colProfNombre;
    @FXML private TableColumn<Usuario, String> colProfCorreo;

    // Estudiantes section
    @FXML private TextField nombreEstudianteField;
    @FXML private TextField correoEstudianteField;
    @FXML private PasswordField claveEstudianteField;
    @FXML private ComboBox<String> cursoEstudianteCombo;
    @FXML private TableView<Usuario> estudiantesTable;
    @FXML private TableColumn<Usuario, Long> colEstId;
    @FXML private TableColumn<Usuario, String> colEstNombre;
    @FXML private TableColumn<Usuario, String> colEstCorreo;
    @FXML private TableColumn<Usuario, String> colEstCurso;

    // Asignaciones section
    @FXML private TableView<ProfesorMateria> asignacionesTable;
    @FXML private TableColumn<ProfesorMateria, String> colAsignacionMateria;
    @FXML private TableColumn<ProfesorMateria, String> colAsignacionCurso;
    @FXML
    private TabPane tabPane;
    // ----------- Services -----------
    private final AdminApiService adminApiService;
    private final UsuarioRegistrationService registrationService;
    private final JwtTokenHolder jwtTokenHolder; // Si necesitas este campo

    @Autowired
    public AdminPanelController(AdminApiService adminApiService,
                                UsuarioRegistrationService registrationService,
                                JwtTokenHolder jwtTokenHolder) { // Opcional si lo necesitas
        this.adminApiService = adminApiService;
        this.registrationService = registrationService;
        this.jwtTokenHolder = jwtTokenHolder; // Opcional
    }
    // ----------- Initialization Methods -----------
    @FXML
    public void initialize() {
        configurarColumnasTablas();
        configurarTablaAsignaciones();

        cargarCursos();
        cargarMaterias();
        cargarProfesores();
        cargarEstudiantes();
        cargarDatosParaAsignacion();

        configurarListenersParaEdicion();

        profesorAsignarCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                Usuario profesor = profesoresTable.getItems().stream()
                        .filter(p -> p.getNombre().equals(newVal))
                        .findFirst()
                        .orElse(null);
                if (profesor != null) {
                    cargarAsignacionesProfesor(profesor.getId());
                }
            }
        });
    }

    private void configurarColumnasTablas() {
        // Materias
        colMateriaId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colMateriaNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));

        // Cursos
        colCursoNombre.setCellValueFactory(new PropertyValueFactory<>("nombreCompleto"));
        colCursoGrado.setCellValueFactory(new PropertyValueFactory<>("grado"));
        colCursoGrupo.setCellValueFactory(new PropertyValueFactory<>("grupo"));

        // Profesores
        colProfId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colProfNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colProfCorreo.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getIdentificacion()));

        // Estudiantes
        colEstId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colEstNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colEstCorreo.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getIdentificacion()));
        colEstCurso.setCellValueFactory(new PropertyValueFactory<>("cursoNombre"));
    }

    private void configurarTablaAsignaciones() {
        colAsignacionMateria.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getMateria().getNombre()));
        colAsignacionCurso.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getCurso().getNombreCompleto()));
    }

    private void configurarListenersParaEdicion() {
        // Materias
        materiasTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                materiaTextField.setText(newSelection.getNombre());
            }
        });

        // Cursos
        cursosTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                gradoCursoField.setText(String.valueOf(newSelection.getGrado()));
                grupoCursoField.setText(newSelection.getGrupo());
            }
        });

        // Profesores
        profesoresTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                nombreProfesorField.setText(newSelection.getNombre());
                correoProfesorField.setText(newSelection.getCorreo());
                claveProfesorField.clear();
            }
        });

        // Estudiantes
        estudiantesTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                nombreEstudianteField.setText(newSelection.getNombre());
                correoEstudianteField.setText(newSelection.getCorreo());
                claveEstudianteField.clear();
            }
        });
    }

    // ----------- Materias Methods -----------
    private void cargarMaterias() {
        try {
            List<Materia> materias = adminApiService.listarMaterias();
            ObservableList<Materia> materiasObservable = FXCollections.observableArrayList(materias);
            Platform.runLater(() -> {
                materiasTable.setItems(materiasObservable);
                materiasTable.refresh();
            });
        } catch (Exception e) {
            Platform.runLater(() -> {
                statusMateriaLabel.setText("Error al cargar materias: " + e.getMessage());
                e.printStackTrace();
            });
        }
    }

    @FXML
    private void registrarMateria() {
        String nombreMateria = materiaTextField.getText().trim();
        if (nombreMateria.isEmpty()) {
            mostrarAlerta("El nombre de la materia es requerido", true);
            return;
        }

        try {
            Materia nuevaMateria = new Materia();
            nuevaMateria.setNombre(nombreMateria);

            Materia materiaRegistrada = adminApiService.crearMateria(nuevaMateria);
            if (materiaRegistrada != null) {
                materiaTextField.clear();
                cargarMaterias();
                mostrarAlerta("Materia registrada exitosamente", false);
            }
        } catch (Exception e) {
            mostrarAlerta("Error al registrar materia: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    @FXML
    private void actualizarMateria() {
        Materia materiaSeleccionada = materiasTable.getSelectionModel().getSelectedItem();
        if (materiaSeleccionada == null) {
            mostrarAlerta("Por favor seleccione una materia para actualizar", true);
            return;
        }

        String nuevoNombre = materiaTextField.getText().trim();
        if (nuevoNombre.isEmpty()) {
            mostrarAlerta("El nuevo nombre de la materia es requerido", true);
            return;
        }

        try {
            materiaSeleccionada.setNombre(nuevoNombre);
            Materia materiaActualizada = adminApiService.actualizarMateria(materiaSeleccionada.getId(), materiaSeleccionada);
            if (materiaActualizada != null) {
                materiaTextField.clear();
                cargarMaterias();
                mostrarAlerta("Materia actualizada exitosamente", false);
            }
        } catch (Exception e) {
            mostrarAlerta("Error al actualizar materia: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    @FXML
    private void eliminarMateria() {
        Materia materiaSeleccionada = materiasTable.getSelectionModel().getSelectedItem();
        if (materiaSeleccionada == null) {
            mostrarAlerta("Por favor seleccione una materia para eliminar", true);
            return;
        }

        try {
            adminApiService.eliminarMateria(materiaSeleccionada.getId());
            mostrarAlerta("Materia eliminada exitosamente", false);
            cargarMaterias();
        } catch (Exception e) {
            mostrarAlerta("Error al eliminar materia: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    // ----------- Cursos Methods -----------
    private void cargarCursos() {
        try {
            List<Curso> cursos = adminApiService.listarCursos();
            ObservableList<Curso> cursosObservable = FXCollections.observableArrayList(cursos);

            Platform.runLater(() -> {
                cursosTable.setItems(cursosObservable);
                cursosTable.refresh();
                statusCursoLabel.setText("Cursos cargados: " + cursos.size());

                ObservableList<String> nombresCursos = FXCollections.observableArrayList();
                cursos.forEach(curso -> nombresCursos.add(curso.getNombreCompleto()));
                cursoEstudianteCombo.setItems(nombresCursos);
            });
        } catch (Exception e) {
            Platform.runLater(() -> {
                statusCursoLabel.setText("Error al cargar cursos: " + e.getMessage());
                e.printStackTrace();
            });
        }
    }

    @FXML
    private void crearCurso() {
        try {
            if (gradoCursoField.getText().isEmpty() || grupoCursoField.getText().isEmpty()) {
                mostrarMensajeCurso("Todos los campos son requeridos", true);
                return;
            }

            int grado = Integer.parseInt(gradoCursoField.getText());
            if (grado < 1 || grado > 12) {
                mostrarMensajeCurso("El grado debe estar entre 1 y 12", true);
                return;
            }

            Curso nuevoCurso = new Curso();
            nuevoCurso.setGrado(grado);
            nuevoCurso.setGrupo(grupoCursoField.getText().toUpperCase());

            Curso cursoRegistrado = adminApiService.registrarCurso(nuevoCurso);
            if (cursoRegistrado != null) {
                mostrarMensajeCurso("Curso creado exitosamente", false);
                gradoCursoField.clear();
                grupoCursoField.clear();
                cargarCursos();
            }
        } catch (NumberFormatException e) {
            mostrarMensajeCurso("El grado debe ser un número válido", true);
        } catch (Exception e) {
            mostrarMensajeCurso("Error al crear curso: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    @FXML
    private void actualizarCurso() {
        Curso cursoSeleccionado = cursosTable.getSelectionModel().getSelectedItem();
        if (cursoSeleccionado == null) {
            mostrarMensajeCurso("Por favor seleccione un curso para actualizar", true);
            return;
        }

        try {
            int nuevoGrado = Integer.parseInt(gradoCursoField.getText());
            String nuevoGrupo = grupoCursoField.getText().toUpperCase();

            cursoSeleccionado.setGrado(nuevoGrado);
            cursoSeleccionado.setGrupo(nuevoGrupo);

            Curso cursoActualizado = adminApiService.actualizarCurso(cursoSeleccionado.getId(), cursoSeleccionado);
            if (cursoActualizado != null) {
                mostrarMensajeCurso("Curso actualizado exitosamente", false);
                gradoCursoField.clear();
                grupoCursoField.clear();
                cargarCursos();
            }
        } catch (NumberFormatException e) {
            mostrarMensajeCurso("El grado debe ser un número válido", true);
        } catch (Exception e) {
            mostrarMensajeCurso("Error al actualizar curso: " + e.getMessage(), true);
        }
    }

    @FXML
    private void eliminarCurso() {
        Curso cursoSeleccionado = cursosTable.getSelectionModel().getSelectedItem();
        if (cursoSeleccionado == null) {
            mostrarAlerta("Por favor seleccione un curso para eliminar", true);
            return;
        }

        try {
            adminApiService.eliminarCurso(cursoSeleccionado.getId());
            mostrarAlerta("Curso eliminado exitosamente", false);
            cargarCursos();
        } catch (Exception e) {
            mostrarAlerta("Error al eliminar curso: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    // ----------- Profesores Methods -----------
    private void cargarProfesores() {
        try {
            List<Usuario> profesores = adminApiService.listarUsuariosPorRol("PROFESOR");
            ObservableList<Usuario> profesoresObservable = FXCollections.observableArrayList(profesores);

            Platform.runLater(() -> {
                profesoresTable.setItems(profesoresObservable);

                ObservableList<String> nombres = FXCollections.observableArrayList();
                profesores.forEach(p -> nombres.add(p.getNombre()));
                profesorAsignarCombo.setItems(nombres);
            });
        } catch (Exception e) {
            Platform.runLater(() -> {
                mostrarAlerta("Error al cargar profesores: " + e.getMessage());
            });
            e.printStackTrace();
        }
    }

    @FXML
    private void registrarProfesor() {
        Dialog<RegistroUsuarioDTO> dialog = new Dialog<>();
        dialog.setTitle("Registrar Nuevo Profesor");
        dialog.setHeaderText("Complete todos los datos del profesor");

        ButtonType registrarButtonType = new ButtonType("Registrar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(registrarButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nombreField = new TextField();
        nombreField.setPromptText("Nombre completo");
        TextField correoField = new TextField();
        correoField.setPromptText("Correo electrónico");
        PasswordField claveField = new PasswordField();
        claveField.setPromptText("Contraseña");
        PasswordField confirmarClaveField = new PasswordField();
        confirmarClaveField.setPromptText("Confirmar contraseña");
        TextField identificacionField = new TextField();
        identificacionField.setPromptText("Número de identificación");
        ComboBox<String> tipoIdentificacionCombo = new ComboBox<>();
        tipoIdentificacionCombo.getItems().addAll("CC", "TI");
        tipoIdentificacionCombo.setValue("CC");
        CheckBox activoCheck = new CheckBox();
        activoCheck.setSelected(true);

        grid.add(new Label("Nombre:"), 0, 0);
        grid.add(nombreField, 1, 0);
        grid.add(new Label("Correo:"), 0, 1);
        grid.add(correoField, 1, 1);
        grid.add(new Label("Contraseña:"), 0, 2);
        grid.add(claveField, 1, 2);
        grid.add(new Label("Confirmar Contraseña:"), 0, 3);
        grid.add(confirmarClaveField, 1, 3);
        grid.add(new Label("Identificación:"), 0, 4);
        grid.add(identificacionField, 1, 4);
        grid.add(new Label("Tipo ID:"), 0, 5);
        grid.add(tipoIdentificacionCombo, 1, 5);
        grid.add(new Label("Activo:"), 0, 6);
        grid.add(activoCheck, 1, 6);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == registrarButtonType) {
                if (nombreField.getText().isEmpty() || correoField.getText().isEmpty() ||
                        claveField.getText().isEmpty() || identificacionField.getText().isEmpty()) {
                    mostrarAlerta("Todos los campos son obligatorios", true);
                    return null;
                }

                if (!claveField.getText().equals(confirmarClaveField.getText())) {
                    mostrarAlerta("Las contraseñas no coinciden", true);
                    return null;
                }

                RegistroUsuarioDTO dto = new RegistroUsuarioDTO();
                dto.setNombre(nombreField.getText());
                dto.setCorreo(correoField.getText());
                dto.setContraseña(claveField.getText());
                dto.setIdentificacion(identificacionField.getText());
                dto.setTipoIdentificacion(tipoIdentificacionCombo.getValue());
                dto.setActivo(activoCheck.isSelected());
                dto.setRol("PROFESOR");

                return dto;
            }
            return null;
        });

        Optional<RegistroUsuarioDTO> result = dialog.showAndWait();

        result.ifPresent(dto -> {
            ResponseEntity<?> response = registrationService.registrarUsuario(dto);
            if (response.getStatusCode().is2xxSuccessful()) {
                mostrarAlerta("Profesor registrado exitosamente", false);
                cargarProfesores();
            } else {
                mostrarAlerta("Error al registrar profesor: " + response.getBody(), true);
            }
        });
    }

    @FXML
    private void actualizarProfesor() {
        Usuario profesorSeleccionado = profesoresTable.getSelectionModel().getSelectedItem();
        if (profesorSeleccionado == null) {
            mostrarAlerta("Por favor seleccione un profesor para actualizar", true);
            return;
        }

        String nuevoNombre = nombreProfesorField.getText().trim();
        String nuevoCorreo = correoProfesorField.getText().trim();
        String nuevaClave = claveProfesorField.getText().trim();

        if (nuevoNombre.isEmpty() || nuevoCorreo.isEmpty()) {
            mostrarAlerta("Nombre y correo son campos requeridos", true);
            return;
        }

        try {
            profesorSeleccionado.setNombre(nuevoNombre);
            profesorSeleccionado.setCorreo(nuevoCorreo);

            if (!nuevaClave.isEmpty()) {
                profesorSeleccionado.setContraseña(nuevaClave);
            }

            Usuario profesorActualizado = adminApiService.actualizarUsuario(profesorSeleccionado.getId(), profesorSeleccionado);
            if (profesorActualizado != null) {
                mostrarAlerta("Profesor actualizado exitosamente", false);
                limpiarCampos();
                cargarProfesores();
            }
        } catch (Exception e) {
            mostrarAlerta("Error al actualizar profesor: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    @FXML
    private void eliminarProfesor() {
        Usuario profesorSeleccionado = profesoresTable.getSelectionModel().getSelectedItem();
        if (profesorSeleccionado == null) {
            mostrarAlerta("Por favor seleccione un profesor para eliminar", true);
            return;
        }

        try {
            adminApiService.eliminarUsuario(profesorSeleccionado.getId());
            mostrarAlerta("Profesor eliminado exitosamente", false);
            cargarProfesores();
        } catch (Exception e) {
            mostrarAlerta("Error al eliminar profesor: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    // ----------- Estudiantes Methods -----------
    private void cargarEstudiantes() {
        try {
            List<Usuario> estudiantes = adminApiService.listarUsuariosPorRol("ESTUDIANTE");
            ObservableList<Usuario> estudiantesObservable = FXCollections.observableArrayList(estudiantes);

            Platform.runLater(() -> {
                estudiantesTable.setItems(estudiantesObservable);
                estudiantesTable.refresh();
            });
        } catch (Exception e) {
            Platform.runLater(() -> {
                mostrarAlerta("Error al cargar estudiantes: " + e.getMessage());
                e.printStackTrace();
            });
        }
    }

    @FXML
    private void registrarEstudiante() {
        Dialog<RegistroUsuarioDTO> dialog = new Dialog<>();
        dialog.setTitle("Registrar Nuevo Estudiante");
        dialog.setHeaderText("Complete todos los datos del estudiante");

        ButtonType registrarButtonType = new ButtonType("Registrar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(registrarButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nombreField = new TextField();
        nombreField.setPromptText("Nombre completo");
        TextField correoField = new TextField();
        correoField.setPromptText("Correo electrónico");
        PasswordField claveField = new PasswordField();
        claveField.setPromptText("Contraseña");
        PasswordField confirmarClaveField = new PasswordField();
        confirmarClaveField.setPromptText("Confirmar contraseña");
        TextField identificacionField = new TextField();
        identificacionField.setPromptText("Número de identificación");
        ComboBox<String> tipoIdentificacionCombo = new ComboBox<>();
        tipoIdentificacionCombo.getItems().addAll("CC", "TI");
        tipoIdentificacionCombo.setValue("CC");
        ComboBox<String> cursoCombo = new ComboBox<>();
        cursoCombo.setItems(cursoEstudianteCombo.getItems());
        CheckBox activoCheck = new CheckBox();
        activoCheck.setSelected(true);

        grid.add(new Label("Nombre:"), 0, 0);
        grid.add(nombreField, 1, 0);
        grid.add(new Label("Correo:"), 0, 1);
        grid.add(correoField, 1, 1);
        grid.add(new Label("Contraseña:"), 0, 2);
        grid.add(claveField, 1, 2);
        grid.add(new Label("Confirmar Contraseña:"), 0, 3);
        grid.add(confirmarClaveField, 1, 3);
        grid.add(new Label("Identificación:"), 0, 4);
        grid.add(identificacionField, 1, 4);
        grid.add(new Label("Tipo ID:"), 0, 5);
        grid.add(tipoIdentificacionCombo, 1, 5);
        grid.add(new Label("Curso:"), 0, 6);
        grid.add(cursoCombo, 1, 6);
        grid.add(new Label("Activo:"), 0, 7);
        grid.add(activoCheck, 1, 7);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == registrarButtonType) {
                if (nombreField.getText().isEmpty() || correoField.getText().isEmpty() ||
                        claveField.getText().isEmpty() || identificacionField.getText().isEmpty() ||
                        cursoCombo.getValue() == null) {
                    mostrarAlerta("Todos los campos son obligatorios", true);
                    return null;
                }

                if (!claveField.getText().equals(confirmarClaveField.getText())) {
                    mostrarAlerta("Las contraseñas no coinciden", true);
                    return null;
                }

                Curso cursoSeleccionado = cursosTable.getItems().stream()
                        .filter(c -> c.getNombreCompleto().equals(cursoCombo.getValue()))
                        .findFirst()
                        .orElse(null);

                if (cursoSeleccionado == null) {
                    mostrarAlerta("Seleccione un curso válido", true);
                    return null;
                }

                RegistroUsuarioDTO dto = new RegistroUsuarioDTO();
                dto.setNombre(nombreField.getText());
                dto.setCorreo(correoField.getText());
                dto.setContraseña(claveField.getText());
                dto.setIdentificacion(identificacionField.getText());
                dto.setTipoIdentificacion(tipoIdentificacionCombo.getValue());
                dto.setActivo(activoCheck.isSelected());
                dto.setRol("ESTUDIANTE");
                dto.setCursoId(cursoSeleccionado.getId());

                return dto;
            }
            return null;
        });

        Optional<RegistroUsuarioDTO> result = dialog.showAndWait();

        result.ifPresent(dto -> {
            ResponseEntity<?> response = registrationService.registrarUsuario(dto);
            if (response.getStatusCode().is2xxSuccessful()) {
                mostrarAlerta("Estudiante registrado exitosamente", false);
                cargarEstudiantes();
            } else {
                mostrarAlerta("Error al registrar estudiante: " + response.getBody(), true);
            }
        });
    }

    @FXML
    private void actualizarEstudiante() {
        Usuario estudianteSeleccionado = estudiantesTable.getSelectionModel().getSelectedItem();
        if (estudianteSeleccionado == null) {
            mostrarAlerta("Por favor seleccione un estudiante para actualizar", true);
            return;
        }

        String nuevoNombre = nombreEstudianteField.getText().trim();
        String nuevoCorreo = correoEstudianteField.getText().trim();
        String nuevaClave = claveEstudianteField.getText().trim();
        String nuevoCurso = cursoEstudianteCombo.getValue();

        if (nuevoNombre.isEmpty() || nuevoCorreo.isEmpty() || nuevoCurso == null) {
            mostrarAlerta("Nombre, correo y curso son campos requeridos", true);
            return;
        }

        try {
            Curso cursoSeleccionado = cursosTable.getItems().stream()
                    .filter(c -> c.getNombreCompleto().equals(nuevoCurso))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Curso no válido"));

            estudianteSeleccionado.setNombre(nuevoNombre);
            estudianteSeleccionado.setCorreo(nuevoCorreo);

            if (!nuevaClave.isEmpty()) {
                estudianteSeleccionado.setContraseña(nuevaClave);
            }

            Usuario estudianteActualizado = adminApiService.actualizarUsuario(estudianteSeleccionado.getId(), estudianteSeleccionado);
            if (estudianteActualizado != null) {
                adminApiService.asignarCursoAEstudiante(estudianteSeleccionado.getId(), cursoSeleccionado.getId());
                mostrarAlerta("Estudiante actualizado exitosamente", false);
                limpiarCamposEstudiante();
                cargarEstudiantes();
            }
        } catch (Exception e) {
            mostrarAlerta("Error al actualizar estudiante: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    @FXML
    private void eliminarEstudiante() {
        Usuario estudianteSeleccionado = estudiantesTable.getSelectionModel().getSelectedItem();
        if (estudianteSeleccionado == null) {
            mostrarAlerta("Por favor seleccione un estudiante para eliminar", true);
            return;
        }

        try {
            adminApiService.eliminarUsuario(estudianteSeleccionado.getId());
            mostrarAlerta("Estudiante eliminado exitosamente", false);
            cargarEstudiantes();
        } catch (Exception e) {
            mostrarAlerta("Error al eliminar estudiante: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    // ----------- Asignaciones Methods -----------
    private void cargarDatosParaAsignacion() {
        try {
            List<Materia> materias = adminApiService.listarMaterias();
            ObservableList<String> nombresMaterias = FXCollections.observableArrayList();
            materias.forEach(m -> nombresMaterias.add(m.getNombre()));

            List<Curso> cursos = adminApiService.listarCursos();
            ObservableList<String> nombresCursos = FXCollections.observableArrayList();
            cursos.forEach(c -> nombresCursos.add(c.getNombreCompleto()));

            Platform.runLater(() -> {
                materiaAsignarCombo.setItems(nombresMaterias);
                cursoAsignarCombo.setItems(nombresCursos);
            });
        } catch (Exception e) {
            Platform.runLater(() -> {
                mostrarAlerta("Error al cargar datos para asignación: " + e.getMessage());
            });
            e.printStackTrace();
        }
    }

    @FXML
    private void asignarMateriaProfesor() {
        String nombreProfesor = profesorAsignarCombo.getValue();
        String nombreMateria = materiaAsignarCombo.getValue();
        String nombreCurso = cursoAsignarCombo.getValue();

        if (nombreProfesor == null || nombreMateria == null || nombreCurso == null) {
            mostrarAlerta("Todos los campos son requeridos para la asignación", true);
            return;
        }

        try {
            Usuario profesor = profesoresTable.getItems().stream()
                    .filter(p -> p.getNombre().equals(nombreProfesor))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Profesor no encontrado"));

            Materia materia = materiasTable.getItems().stream()
                    .filter(m -> m.getNombre().equals(nombreMateria))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Materia no encontrada"));

            Curso curso = cursosTable.getItems().stream()
                    .filter(c -> c.getNombreCompleto().equals(nombreCurso))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Curso no encontrado"));

            ProfesorMateriaRequest request = new ProfesorMateriaRequest();
            request.setProfesorId(profesor.getId());
            request.setMateriaId(materia.getId());
            request.setCursoId(curso.getId());

            ProfesorMateria asignacion = adminApiService.crearAsignacion(request);
            if (asignacion != null) {
                mostrarAlerta("Asignación creada exitosamente", false);
                cargarAsignacionesProfesor(profesor.getId());
            }
        } catch (Exception e) {
            mostrarAlerta("Error al crear asignación: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    private void cargarAsignacionesProfesor(Long profesorId) {
        try {
            List<ProfesorMateria> asignaciones = adminApiService.getAsignacionesPorProfesor(profesorId);
            ObservableList<ProfesorMateria> asignacionesObservable = FXCollections.observableArrayList(asignaciones);

            Platform.runLater(() -> {
                asignacionesTable.setItems(asignacionesObservable);
                asignacionesTable.refresh();
            });
        } catch (Exception e) {
            Platform.runLater(() -> {
                mostrarAlerta("Error al cargar asignaciones: " + e.getMessage());
            });
            e.printStackTrace();
        }
    }

    @FXML
    private void eliminarAsignacion() {
        ProfesorMateria asignacionSeleccionada = asignacionesTable.getSelectionModel().getSelectedItem();
        if (asignacionSeleccionada == null) {
            mostrarAlerta("Por favor seleccione una asignación para eliminar", true);
            return;
        }

        try {
            adminApiService.eliminarAsignacion(asignacionSeleccionada.getId());
            mostrarAlerta("Asignación eliminada exitosamente", false);

            String profesorSeleccionado = profesorAsignarCombo.getValue();
            if (profesorSeleccionado != null) {
                Usuario profesor = profesoresTable.getItems().stream()
                        .filter(p -> p.getNombre().equals(profesorSeleccionado))
                        .findFirst()
                        .orElse(null);
                if (profesor != null) {
                    cargarAsignacionesProfesor(profesor.getId());
                }
            }
        } catch (Exception e) {
            mostrarAlerta("Error al eliminar asignación: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    // ----------- Utility Methods -----------
    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarAlertaError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void limpiarCampos() {
        nombreProfesorField.clear();
        correoProfesorField.clear();
        claveProfesorField.clear();
    }

    private void limpiarCamposEstudiante() {
        nombreEstudianteField.clear();
        correoEstudianteField.clear();
        claveEstudianteField.clear();
        cursoEstudianteCombo.setValue(null);
    }

    private void mostrarMensajeCurso(String mensaje, boolean esError) {
        statusCursoLabel.setText(mensaje);
        statusCursoLabel.setStyle(esError ? "-fx-text-fill: red;" : "-fx-text-fill: green;");
    }

    private void mostrarAlerta(String mensaje) {
        mostrarAlerta(mensaje, true);
    }

    private void mostrarAlerta(String mensaje, boolean esError) {
        Alert alert = new Alert(esError ? Alert.AlertType.ERROR : Alert.AlertType.INFORMATION);
        alert.setTitle(esError ? "Error" : "Éxito");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();

    }
    // Añade estos métodos a tu controlador existente

    @FXML
    private void cerrarSesion() {
        try {
            // Limpiar el token JWT si es necesario
            if (jwtTokenHolder != null) {
                jwtTokenHolder.clearToken();
            }

            // Cargar la pantalla de login
            Stage stage = (Stage) tabPane.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/com/rep/controller/views/Login.fxml"));
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (Exception e) {
            mostrarAlerta("Error al cerrar sesión: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    @FXML
    private void showMaterias() {
        tabPane.getSelectionModel().select(0);
    }

    @FXML
    private void showCursos() {
        tabPane.getSelectionModel().select(1);
    }

    @FXML
    private void showProfesores() {
        tabPane.getSelectionModel().select(2);
    }

    @FXML
    private void showEstudiantes() {
        tabPane.getSelectionModel().select(3);
    }
}