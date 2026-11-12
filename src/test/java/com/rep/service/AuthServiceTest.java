package com.rep.service;

import com.rep.config.JwtConfig;
import com.rep.dto.auth.LoginRequest;
import com.rep.dto.auth.LoginResponse;
import com.rep.exception.AccountInactiveException;
import com.rep.exception.AuthenticationException;
import com.rep.model.Usuario;
import com.rep.model.Usuario.Rol;
import com.rep.repositories.UsuarioRepository;
import com.rep.service.logica.AuthService;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtConfig jwtConfig;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        String testSecret = "01234567890123456789012345678901"; // 32 bytes
        SecretKey secretKey = Keys.hmacShaKeyFor(testSecret.getBytes(StandardCharsets.UTF_8));
        lenient().when(jwtConfig.getExpiration()).thenReturn(86400000L);

        authService = new AuthService(usuarioRepository, passwordEncoder, secretKey, jwtConfig);
    }

    @Test
    @DisplayName("authenticateUser retorna token y respuesta exitosa cuando las credenciales son válidas")
    void testAuthenticateSuccess() {
        Usuario usuario = new Usuario();
        usuario.setId(10L);
        usuario.setIdentificacion("12345678");
        usuario.setNombre("Carlos");
        usuario.setApellido("Pérez");
        usuario.setContraseña("encoded_pass");
        usuario.setRol(Rol.PROFESOR);
        usuario.setActivo(true);

        when(usuarioRepository.findByIdentificacion("12345678")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("secret123", "encoded_pass")).thenReturn(true);

        LoginRequest request = new LoginRequest("12345678", "secret123");
        LoginResponse response = authService.authenticateUser(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertNotNull(response.getToken());
        assertNotNull(response.getUsuario());
        assertEquals("12345678", response.getUsuario().getIdentificacion());
    }

    @Test
    @DisplayName("authenticateUser lanza AuthenticationException cuando el usuario no existe")
    void testAuthenticateUserNotFound() {
        when(usuarioRepository.findByIdentificacion("unknown")).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest("unknown", "pass");
        assertThrows(AuthenticationException.class, () -> authService.authenticateUser(request));
    }

    @Test
    @DisplayName("authenticateUser lanza AuthenticationException cuando la contraseña no coincide")
    void testAuthenticateInvalidPassword() {
        Usuario usuario = new Usuario();
        usuario.setIdentificacion("12345678");
        usuario.setContraseña("encoded_pass");
        usuario.setActivo(true);

        when(usuarioRepository.findByIdentificacion("12345678")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("wrong_pass", "encoded_pass")).thenReturn(false);

        LoginRequest request = new LoginRequest("12345678", "wrong_pass");
        assertThrows(AuthenticationException.class, () -> authService.authenticateUser(request));
    }

    @Test
    @DisplayName("authenticateUser lanza AccountInactiveException cuando la cuenta está desactivada")
    void testAuthenticateInactiveAccount() {
        Usuario usuario = new Usuario();
        usuario.setIdentificacion("12345678");
        usuario.setContraseña("encoded_pass");
        usuario.setActivo(false);

        when(usuarioRepository.findByIdentificacion("12345678")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("secret123", "encoded_pass")).thenReturn(true);

        LoginRequest request = new LoginRequest("12345678", "secret123");
        assertThrows(AccountInactiveException.class, () -> authService.authenticateUser(request));
    }
}
