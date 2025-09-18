package com.rep.service.funciones;

import com.rep.dto.auth.LoginResponse;
import java.util.Optional;

public interface AuthServiceClient {
    Optional<LoginResponse> authenticate(String identificacion, String password);
}