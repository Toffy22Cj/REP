package com.rep.service.impl;

import com.rep.dto.profesor.ProfesorMateriaRequest;
import com.rep.dto.tokens.JwtTokenHolder;
import com.rep.model.*;

import com.rep.service.funciones.AdminApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class AdminApiServiceImpl implements AdminApiService {

    private final RestTemplate restTemplate;
    private final String baseUrl = "http://localhost:8080/api/admin"; // Ajusta la URL según tu configuración

    @Autowired
    private JwtTokenHolder jwtTokenHolder;

    public AdminApiServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private HttpHeaders createHeadersWithToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("Authorization", "Bearer " + jwtTokenHolder.getToken());
        return headers;
    }

    // -------------------- Gestión de Cursos --------------------
    @Override
    public List<Curso> listarCursos() {
        HttpEntity<String> entity = new HttpEntity<>(createHeadersWithToken());
        ResponseEntity<List<Curso>> response = restTemplate.exchange(
                baseUrl + "/cursos",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<Curso>>() {}
        );
        return response.getBody();
    }

    @Override
    public Curso obtenerCursoPorId(Long id) {
        HttpEntity<String> entity = new HttpEntity<>(createHeadersWithToken());
        ResponseEntity<Curso> response = restTemplate.exchange(
                baseUrl + "/cursos/" + id,
                HttpMethod.GET,
                entity,
                Curso.class
        );
        return response.getBody();
    }

    @Override
    public Curso registrarCurso(Curso curso) {
        HttpEntity<Curso> entity = new HttpEntity<>(curso, createHeadersWithToken());
        ResponseEntity<Curso> response = restTemplate.exchange(
                baseUrl + "/cursos",
                HttpMethod.POST,
                entity,
                Curso.class
        );
        return response.getBody();
    }

    @Override
    public Curso actualizarCurso(Long id, Curso curso) {
        HttpEntity<Curso> entity = new HttpEntity<>(curso, createHeadersWithToken());
        ResponseEntity<Curso> response = restTemplate.exchange(
                baseUrl + "/cursos/" + id,
                HttpMethod.PUT,
                entity,
                Curso.class
        );
        return response.getBody();
    }

    @Override
    public void eliminarCurso(Long id) {
        HttpEntity<String> entity = new HttpEntity<>(createHeadersWithToken());
        restTemplate.exchange(
                baseUrl + "/cursos/" + id,
                HttpMethod.DELETE,
                entity,
                Void.class
        );
    }

    @Override
    public List<Estudiante> obtenerEstudiantesPorCurso(Long cursoId) {
        HttpEntity<String> entity = new HttpEntity<>(createHeadersWithToken());
        ResponseEntity<List<Estudiante>> response = restTemplate.exchange(
                baseUrl + "/cursos/" + cursoId + "/estudiantes",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<Estudiante>>() {}
        );
        return response.getBody();
    }

    // -------------------- Gestión de Materias --------------------
    @Override
    public List<Materia> listarMaterias() {
        HttpEntity<String> entity = new HttpEntity<>(createHeadersWithToken());
        ResponseEntity<List<Materia>> response = restTemplate.exchange(
                baseUrl + "/materias",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<Materia>>() {}
        );
        return response.getBody();
    }

    @Override
    public Materia obtenerMateriaPorId(Long id) {
        HttpEntity<String> entity = new HttpEntity<>(createHeadersWithToken());
        ResponseEntity<Materia> response = restTemplate.exchange(
                baseUrl + "/materias/" + id,
                HttpMethod.GET,
                entity,
                Materia.class
        );
        return response.getBody();
    }

    @Override
    public Materia crearMateria(Materia materia) {
        HttpEntity<Materia> entity = new HttpEntity<>(materia, createHeadersWithToken());
        ResponseEntity<Materia> response = restTemplate.exchange(
                baseUrl + "/materias",
                HttpMethod.POST,
                entity,
                Materia.class
        );
        return response.getBody();
    }

    @Override
    public Materia actualizarMateria(Long id, Materia materia) {
        HttpEntity<Materia> entity = new HttpEntity<>(materia, createHeadersWithToken());
        ResponseEntity<Materia> response = restTemplate.exchange(
                baseUrl + "/materias/" + id,
                HttpMethod.PUT,
                entity,
                Materia.class
        );
        return response.getBody();
    }

    @Override
    public void eliminarMateria(Long id) {
        HttpEntity<String> entity = new HttpEntity<>(createHeadersWithToken());
        restTemplate.exchange(
                baseUrl + "/materias/" + id,
                HttpMethod.DELETE,
                entity,
                Void.class
        );
    }

    // -------------------- Gestión de Usuarios --------------------
    @Override
    public List<Usuario> listarUsuariosPorRol(String rol) {
        HttpEntity<String> entity = new HttpEntity<>(createHeadersWithToken());
        String url = baseUrl + "/usuarios" + (rol != null ? "?rol=" + rol : "");
        ResponseEntity<List<Usuario>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<Usuario>>() {}
        );
        return response.getBody();
    }
    @Override
    public Usuario obtenerUsuarioPorId(Long id) {
        HttpEntity<String> entity = new HttpEntity<>(createHeadersWithToken());
        ResponseEntity<Usuario> response = restTemplate.exchange(
                baseUrl + "/usuarios/" + id,
                HttpMethod.GET,
                entity,
                Usuario.class
        );
        return response.getBody();
    }

    @Override
    public Usuario actualizarUsuario(Long id, Usuario usuario) {
        HttpEntity<Usuario> entity = new HttpEntity<>(usuario, createHeadersWithToken());
        ResponseEntity<Usuario> response = restTemplate.exchange(
                baseUrl + "/usuarios/" + id,
                HttpMethod.PUT,
                entity,
                Usuario.class
        );
        return response.getBody();
    }

    @Override
    public void eliminarUsuario(Long id) {
        HttpEntity<String> entity = new HttpEntity<>(createHeadersWithToken());
        restTemplate.exchange(
                baseUrl + "/usuarios/" + id,
                HttpMethod.DELETE,
                entity,
                Void.class
        );
    }

    @Override
    public void asignarCursoAEstudiante(Long estudianteId, Long cursoId) {
        HttpEntity<String> entity = new HttpEntity<>(createHeadersWithToken());
        restTemplate.exchange(
                baseUrl + "/estudiantes/" + estudianteId + "/curso?cursoId=" + cursoId,
                HttpMethod.PUT,
                entity,
                Void.class
        );
    }

    @Override
    public Profesor obtenerProfesorPorId(Long id) {
        HttpEntity<String> entity = new HttpEntity<>(createHeadersWithToken());
        ResponseEntity<Profesor> response = restTemplate.exchange(
                baseUrl + "/profesores/" + id,
                HttpMethod.GET,
                entity,
                Profesor.class
        );
        return response.getBody();
    }

    @Override
    public Profesor actualizarEstadoProfesor(Long id, String estado) {
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(
                Collections.singletonMap("estado", estado),
                createHeadersWithToken()
        );
        ResponseEntity<Profesor> response = restTemplate.exchange(
                baseUrl + "/profesores/" + id + "/estado",
                HttpMethod.PUT,
                entity,
                Profesor.class
        );
        return response.getBody();
    }

    @Override
    public List<ProfesorMateria> listarAsignaciones(Long cursoId, Long materiaId) {
        HttpEntity<String> entity = new HttpEntity<>(createHeadersWithToken());
        String url = baseUrl + "/asignaciones?";
        if (cursoId != null) url += "cursoId=" + cursoId;
        if (materiaId != null) url += (cursoId != null ? "&" : "") + "materiaId=" + materiaId;

        ResponseEntity<List<ProfesorMateria>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<ProfesorMateria>>() {}
        );
        return response.getBody();
    }

    @Override
    public List<ProfesorMateria> getAsignacionesPorProfesor(Long profesorId) {
        HttpEntity<String> entity = new HttpEntity<>(createHeadersWithToken());
        ResponseEntity<List<ProfesorMateria>> response = restTemplate.exchange(
                baseUrl + "/profesores/" + profesorId + "/asignaciones",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<ProfesorMateria>>() {}
        );
        return response.getBody();
    }

    @Override
    public List<Materia> getMateriasPorProfesor(Long profesorId) {
        HttpEntity<String> entity = new HttpEntity<>(createHeadersWithToken());
        ResponseEntity<List<Materia>> response = restTemplate.exchange(
                baseUrl + "/profesores/" + profesorId + "/materias",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<Materia>>() {}
        );
        return response.getBody();
    }

    @Override
    public ProfesorMateria crearAsignacion(ProfesorMateriaRequest request) {
        HttpEntity<ProfesorMateriaRequest> entity = new HttpEntity<>(request, createHeadersWithToken());
        ResponseEntity<ProfesorMateria> response = restTemplate.exchange(
                baseUrl + "/asignaciones",
                HttpMethod.POST,
                entity,
                ProfesorMateria.class
        );
        return response.getBody();
    }

    @Override
    public void eliminarAsignacion(Long id) {
        HttpEntity<String> entity = new HttpEntity<>(createHeadersWithToken());
        restTemplate.exchange(
                baseUrl + "/asignaciones/" + id,
                HttpMethod.DELETE,
                entity,
                Void.class
        );
    }
}