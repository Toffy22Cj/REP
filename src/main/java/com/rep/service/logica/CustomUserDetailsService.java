package com.rep.service.logica;

import com.rep.model.Usuario;
import com.rep.repositories.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public CustomUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String identificacion) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByIdentificacion(identificacion)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        // Devuelve el Usuario directamente (que implementa UserDetails)
        return usuario;
    }
}