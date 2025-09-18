package com.rep.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import com.rep.dto.tokens.JwtTokenHolder;

import java.util.List;

@Service
public class ApiClientService {
    private final RestTemplate restTemplate;
    private final JwtTokenHolder jwtTokenHolder;
    private final String API_BASE_URL = "http://localhost:8080/api";

    @Autowired
    public ApiClientService(RestTemplate restTemplate, JwtTokenHolder jwtTokenHolder) {
        this.restTemplate = restTemplate;
        this.jwtTokenHolder = jwtTokenHolder;
    }

    // Método genérico para GET
    public <T> ResponseEntity<T> get(String endpoint, Class<T> responseType) {
        return executeRequest(endpoint, HttpMethod.GET, null, responseType);
    }

    public <T> ResponseEntity<T> get(String endpoint, ParameterizedTypeReference<T> responseType) {
        return executeRequest(endpoint, HttpMethod.GET, null, responseType);
    }

    // Método genérico para POST
    public <T> ResponseEntity<T> post(String endpoint, Object requestBody, Class<T> responseType) {
        return executeRequest(endpoint, HttpMethod.POST, requestBody, responseType);
    }

    // Método genérico para PUT
    public <T> ResponseEntity<T> put(String endpoint, Object requestBody, Class<T> responseType) {
        return executeRequest(endpoint, HttpMethod.PUT, requestBody, responseType);
    }

    // Método genérico para DELETE
    public ResponseEntity<Void> delete(String endpoint) {
        return executeRequest(endpoint, HttpMethod.DELETE, null, Void.class);
    }

    // Métodos privados para ejecutar las solicitudes
    private <T> ResponseEntity<T> executeRequest(String endpoint, HttpMethod method,
                                                 Object requestBody, Class<T> responseType) {
        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<?> entity = requestBody != null ?
                    new HttpEntity<>(requestBody, headers) :
                    new HttpEntity<>(headers);

            return restTemplate.exchange(
                    API_BASE_URL + endpoint,
                    method,
                    entity,
                    responseType
            );
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private <T> ResponseEntity<T> executeRequest(String endpoint, HttpMethod method,
                                                 Object requestBody, ParameterizedTypeReference<T> responseType) {
        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<?> entity = requestBody != null ?
                    new HttpEntity<>(requestBody, headers) :
                    new HttpEntity<>(headers);

            return restTemplate.exchange(
                    API_BASE_URL + endpoint,
                    method,
                    entity,
                    responseType
            );
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

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