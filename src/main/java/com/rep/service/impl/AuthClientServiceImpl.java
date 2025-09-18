package com.rep.service.funciones;

import com.rep.dto.auth.LoginRequest;
import com.rep.dto.auth.LoginResponse;
import com.rep.service.ApiClientService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class AuthClientServiceImpl implements AuthServiceClient {
    private final ApiClientService apiClient;

    public AuthClientServiceImpl(ApiClientService apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public Optional<LoginResponse> authenticate(String identificacion, String password) {
        ResponseEntity<LoginResponse> response = apiClient.post(
                "/auth/login",
                new LoginRequest(identificacion, password),
                LoginResponse.class
        );
        return response.getStatusCode().is2xxSuccessful()
                ? Optional.ofNullable(response.getBody())
                : Optional.empty();
    }
}