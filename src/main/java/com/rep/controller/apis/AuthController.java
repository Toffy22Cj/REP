package com.rep.controller.apis;

import com.rep.dto.auth.LoginRequest;
import com.rep.dto.auth.LoginResponse;
import com.rep.service.logica.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Autenticación por identificación y contraseña.
 *
 * <p>Errores (formato {@code ApiError}): {@code CREDENCIALES_INVALIDAS} (401),
 * {@code CUENTA_INACTIVA} (403), {@code CAMPOS_INVALIDOS} (400),
 * {@code DEMASIADAS_SOLICITUDES} (429).</p>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.authenticateUser(request));
    }
}
