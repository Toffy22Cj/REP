package com.rep.service.logica;

import com.rep.config.JwtConfig;
import com.rep.dto.auth.AuthUsuarioDTO;
import com.rep.dto.auth.LoginRequest;
import com.rep.dto.auth.LoginResponse;
import com.rep.exception.AccountInactiveException;
import com.rep.exception.AuthenticationException;
import com.rep.model.Usuario;
import com.rep.repositories.UsuarioRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Collections;
import java.util.Date;

@Service
public class AuthService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecretKey secretKey;
    private final JwtConfig jwtConfig;

    public AuthService(UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder,
                       SecretKey secretKey,
                       JwtConfig jwtConfig) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.secretKey = secretKey;
        this.jwtConfig = jwtConfig;
    }

    public LoginResponse authenticateUser(LoginRequest request) {
        Usuario usuario = authenticate(request.getIdentificacion(), request.getPassword());
        String token = generateToken(usuario);
        return LoginResponse.success(new AuthUsuarioDTO(usuario), token, "Autenticación exitosa");
    }

    private Usuario authenticate(String identificacion, String password) {
        Usuario usuario = usuarioRepository.findByIdentificacion(identificacion)
                .orElseThrow(() -> new AuthenticationException("Credenciales inválidas"));

        if (!passwordEncoder.matches(password, usuario.getContraseña())) {
            throw new AuthenticationException("Credenciales inválidas");
        }

        if (!usuario.isActivo()) {
            throw new AccountInactiveException("La cuenta está inactiva. Si acaba de solicitarla, espere a que la administración la active");
        }

        return usuario;
    }

    /** Emite un JWT para el usuario (login y tras cambiar la contraseña). */
    public String generateToken(Usuario usuario) {
        Claims claims = Jwts.claims().setSubject(usuario.getIdentificacion());
        claims.put("userId", usuario.getId());
        claims.put("authorities", Collections.singletonList(usuario.getRol().name()));

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtConfig.getExpiration()))
                .signWith(secretKey)
                .compact();
    }
}