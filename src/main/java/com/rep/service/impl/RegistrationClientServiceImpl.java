package com.rep.service.impl;


import com.rep.dto.auth.RegistroUsuarioDTO;
import com.rep.service.ApiClientService;
import com.rep.service.funciones.RegistrationServiceClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class RegistrationClientServiceImpl implements RegistrationServiceClient {
    private final ApiClientService apiClient;

    public RegistrationClientServiceImpl(ApiClientService apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public ResponseEntity<?> registerUser(RegistroUsuarioDTO usuarioDTO) {
        return apiClient.post(
                "/registro",
                usuarioDTO,
                String.class // O el tipo de respuesta esperado
        );
    }
}