package com.rep.controller.apis;

import com.rep.dto.auth.RegistroUsuarioDTO;
import com.rep.service.logica.UsuarioRegistrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost")
@RestController
@RequestMapping("/api/registro")
public class RegistroApi {
    private final UsuarioRegistrationService registrationService;

    public RegistroApi(UsuarioRegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping
    public ResponseEntity<?> registrar(@RequestBody RegistroUsuarioDTO usuarioDTO) {
        return registrationService.registrarUsuario(usuarioDTO);
    }
}