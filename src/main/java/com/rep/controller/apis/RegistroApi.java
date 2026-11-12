package com.rep.controller.apis;

import com.rep.dto.auth.AuthUsuarioDTO;
import com.rep.dto.auth.RegistroUsuarioDTO;
import com.rep.service.logica.UsuarioRegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Auto-registro público de estudiantes. Errores: ver {@code docs/errores.md}. */
@RestController
@RequestMapping("/api/registro")
public class RegistroApi {
    private final UsuarioRegistrationService registrationService;

    public RegistroApi(UsuarioRegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping
    public ResponseEntity<AuthUsuarioDTO> registrar(@Valid @RequestBody RegistroUsuarioDTO usuarioDTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthUsuarioDTO(registrationService.registrarEstudiante(usuarioDTO)));
    }
}
