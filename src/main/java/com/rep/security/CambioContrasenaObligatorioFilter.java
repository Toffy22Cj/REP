package com.rep.security;

import com.rep.exception.ApiErrorWriter;
import com.rep.exception.ErrorCode;
import com.rep.model.Usuario;
import com.rep.repositories.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Mientras un usuario tenga una contraseña provisional, la API solo le permite ver su
 * perfil y cambiar la contraseña ({@code CAMBIO_CONTRASENA_OBLIGATORIO}, 403). Las
 * páginas estáticas del panel web siguen cargando para poder mostrar el formulario.
 */
public class CambioContrasenaObligatorioFilter extends OncePerRequestFilter {
    private final UsuarioRepository usuarios;

    public CambioContrasenaObligatorioFilter(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Usuario u && u.requiereCambioDeContrasena()
                && !permitida(request.getRequestURI()) && sigueObligado(u)) {
            ApiErrorWriter.write(request, response, ErrorCode.CAMBIO_CONTRASENA_OBLIGATORIO);
            return;
        }
        chain.doFilter(request, response);
    }

    /**
     * En el panel web el usuario vive en la sesión y no se actualiza al cambiar la
     * contraseña: se confirma contra la base y se refresca la copia de la sesión.
     */
    private boolean sigueObligado(Usuario u) {
        boolean obligado = usuarios.findById(u.getId()).map(Usuario::requiereCambioDeContrasena).orElse(false);
        if (!obligado) {
            u.setDebeCambiarContrasena(false);
        }
        return obligado;
    }

    static boolean permitida(String uri) {
        if (!uri.startsWith("/api/")) {
            return true; // páginas, estilos y scripts del panel; logout
        }
        return uri.startsWith("/api/perfil") || uri.startsWith("/api/admin/perfil")
                || uri.startsWith("/api/auth/") || uri.startsWith("/api/public/");
    }
}
