// EstudianteApiServiceImpl.java
package com.rep.service.impl;

import com.rep.dto.actividad.ActividadDTO;
import com.rep.dto.actividad.MateriaDTO;
import com.rep.dto.tokens.JwtTokenHolder;
import com.rep.service.funciones.EstudianteApiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpClientErrorException;
import java.util.Collections;
import java.util.List;

@Service
public class EstudianteApiServiceImpl implements EstudianteApiService {
    private static final Logger logger = LoggerFactory.getLogger(EstudianteApiServiceImpl.class);

    private final RestTemplate restTemplate;
    private final String apiBaseUrl;
    private final JwtTokenHolder jwtTokenHolder;

    public EstudianteApiServiceImpl(
            RestTemplate restTemplate,
            @Value("${estudiante.service.url}") String apiBaseUrl,
            JwtTokenHolder jwtTokenHolder) {
        this.restTemplate = restTemplate;
        this.apiBaseUrl = apiBaseUrl;
        this.jwtTokenHolder = jwtTokenHolder;
        logger.info("EstudianteApiServiceImpl inicializado con URL base: {}", apiBaseUrl);
    }

    @Override
    public List<MateriaDTO> getMateriasByEstudiante(Long id) {
        final String endpoint = "/materias";
        String url = buildUrl(id, endpoint);
        logger.debug("Solicitando materias para estudiante {} en URL: {}", id, url);

        try {
            return makeAuthenticatedRequest(
                    url,
                    new ParameterizedTypeReference<List<MateriaDTO>>() {}
            );
        } catch (Exception e) {
            logger.error("Error al obtener materias para estudiante {}: {}", id, e.getMessage());
            return handleException(e, Collections.emptyList());
        }
    }

    @Override
    public List<ActividadDTO> getActividadesByEstudiante(Long id) {
        final String endpoint = "/actividades";
        String url = buildUrl(id, endpoint);
        logger.debug("Solicitando actividades para estudiante {} en URL: {}", id, url);

        try {
            return makeAuthenticatedRequest(
                    url,
                    new ParameterizedTypeReference<List<ActividadDTO>>() {}
            );
        } catch (Exception e) {
            logger.error("Error al obtener actividades para estudiante {}: {}", id, e.getMessage());
            return handleException(e, Collections.emptyList());
        }
    }

    @Override
    public List<ActividadDTO> getActividadesByMateria(Long id, Long materiaId) {
        final String endpoint = "/materias/" + materiaId + "/actividades";
        String url = buildUrl(id, endpoint);
        logger.debug("Solicitando actividades para estudiante {} y materia {} en URL: {}", id, materiaId, url);

        try {
            return makeAuthenticatedRequest(
                    url,
                    new ParameterizedTypeReference<List<ActividadDTO>>() {}
            );
        } catch (Exception e) {
            logger.error("Error al obtener actividades para estudiante {} y materia {}: {}",
                    id, materiaId, e.getMessage());
            return handleException(e, Collections.emptyList());
        }
    }

    private String buildUrl(Long estudianteId, String endpoint) {
        return apiBaseUrl + "/api/estudiante/" + estudianteId + endpoint;
    }

    private <T> T makeAuthenticatedRequest(String url, ParameterizedTypeReference<T> responseType) {
        try {
            HttpHeaders headers = createHeadersWithToken();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            logger.debug("Realizando request a {} con token {}", url, getTokenPreview());

            ResponseEntity<T> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    responseType
            );

            validateResponse(response);
            logSuccessResponse(response);

            return response.getBody();
        } catch (HttpClientErrorException e) {
            logger.error("Error HTTP al realizar request: {}", e.getStatusCode());
            throw new RuntimeException("Error en la solicitud: " + e.getStatusCode(), e);
        } catch (Exception e) {
            logger.error("Error inesperado al realizar request", e);
            throw new RuntimeException("Error inesperado al realizar la solicitud", e);
        }
    }

    private HttpHeaders createHeadersWithToken() {
        if (jwtTokenHolder == null || jwtTokenHolder.getToken() == null || jwtTokenHolder.getToken().isEmpty()) {
            throw new IllegalArgumentException("Token no puede ser nulo o vacío");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("Authorization", formatToken(jwtTokenHolder.getToken()));
        return headers;
    }

    private String formatToken(String token) {
        String cleanedToken = token.startsWith("Bearer ") ? token.substring(7) : token;
        return "Bearer " + cleanedToken;
    }

    private String getTokenPreview() {
        String token = jwtTokenHolder != null ? jwtTokenHolder.getToken() : null;
        return token != null && token.length() > 10 ?
                token.substring(0, 10) + "..." : "[token no válido]";
    }

    private <T> void validateResponse(ResponseEntity<T> response) {
        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new RuntimeException("Respuesta no exitosa: " + response.getStatusCode());
        }
    }

    private <T> void logSuccessResponse(ResponseEntity<T> response) {
        if (response.getBody() instanceof List) {
            logger.info("Respuesta exitosa con {} elementos", ((List<?>) response.getBody()).size());
        } else {
            logger.info("Respuesta exitosa");
        }
    }

    private <T> T handleException(Exception e, T defaultValue) {
        if (e instanceof HttpClientErrorException.Unauthorized) {
            logger.error("Error de autenticación - Token inválido o expirado");
            throw new RuntimeException("Token inválido o expirado", e);
        }
        return defaultValue;
    }
}