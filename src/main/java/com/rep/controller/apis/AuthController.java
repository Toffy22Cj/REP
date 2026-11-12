package com.rep.controller.apis;

import com.rep.dto.auth.LoginRequest;
import com.rep.dto.auth.LoginResponse;
import com.rep.exception.AccountInactiveException;
import com.rep.exception.AuthenticationException;
import com.rep.service.logica.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Autenticación por identificación y contraseña.
     *
     * <p>Antes se capturaba la {@code AuthenticationException} de Spring Security, pero
     * {@link AuthService} lanza la excepción propia de {@code com.rep.exception}; las
     * credenciales incorrectas terminaban en un 500. Ahora: 401 credenciales inválidas,
     * 403 cuenta inactiva.</p>
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            return ResponseEntity.ok(authService.authenticateUser(request));
        } catch (AccountInactiveException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(LoginResponse.error(e.getMessage()));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(LoginResponse.error(e.getMessage()));
        }
    }
}
