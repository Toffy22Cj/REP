package com.rep.service.funciones;

import com.rep.dto.actividad.PreguntaPlantillaDTO;
import com.rep.model.Pregunta;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PreguntaApiClient {
    private final RestTemplate restTemplate;
    private final String baseUrl = "http://localhost:8080/api/preguntas";

    public PreguntaApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public CompletableFuture<List<Pregunta>> crearPreguntasDesdePlantilla(
            List<PreguntaPlantillaDTO> plantillas,
            Long actividadId,
            String token) {
        return CompletableFuture.supplyAsync(() -> {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);

            String url = baseUrl + "/lote?actividadId=" + actividadId;
            ResponseEntity<List<Pregunta>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(plantillas, headers),
                    new ParameterizedTypeReference<List<Pregunta>>() {}
            );

            return response.getBody();
        });
    }
}