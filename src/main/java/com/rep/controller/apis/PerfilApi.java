package com.rep.controller.apis;

import com.rep.dto.perfil.ActualizarPerfilDTO;
import com.rep.dto.perfil.CambiarContrasenaDTO;
import com.rep.dto.perfil.PerfilDTO;
import com.rep.model.Usuario;
import com.rep.service.logica.PerfilService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Perfil del usuario autenticado.
 *
 * <p>Disponible en {@code /api/perfil} (cliente de escritorio, JWT) y en
 * {@code /api/admin/perfil} (panel web de administración, sesión).</p>
 */
@RestController
@RequestMapping({ "/api/perfil", "/api/admin/perfil" })
public class PerfilApi {
    private final PerfilService perfilService;

    public PerfilApi(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

    @GetMapping
    public PerfilDTO obtener(@AuthenticationPrincipal Usuario usuario) {
        return perfilService.obtener(usuario.getId());
    }

    @PutMapping
    public PerfilDTO actualizar(@Valid @RequestBody ActualizarPerfilDTO dto, @AuthenticationPrincipal Usuario usuario) {
        return perfilService.actualizar(usuario.getId(), dto);
    }

    /** Devuelve un token nuevo: los emitidos antes del cambio dejan de ser válidos. */
    @PutMapping("/contrasena")
    public Map<String, String> cambiarContrasena(@Valid @RequestBody CambiarContrasenaDTO dto,
            @AuthenticationPrincipal Usuario usuario) {
        String token = perfilService.cambiarContrasena(usuario.getId(), dto.actual(), dto.nueva());
        return Map.of("token", token, "mensaje", "Contraseña actualizada");
    }
}
