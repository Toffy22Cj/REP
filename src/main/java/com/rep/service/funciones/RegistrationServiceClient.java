package com.rep.service.funciones;

import com.rep.dto.auth.RegistroUsuarioDTO;
import org.springframework.http.ResponseEntity;

public interface RegistrationServiceClient {
    ResponseEntity<?> registerUser(RegistroUsuarioDTO usuarioDTO);
}