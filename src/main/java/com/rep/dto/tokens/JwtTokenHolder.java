package com.rep.dto.tokens;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Data
@Component
public class JwtTokenHolder {
    private String token;
    private Long userId;

    @Value("${jwt.secret}")
    private String secretKey;

    public void setToken(String token) {
        this.token = token;

        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(secretKey.getBytes())  // importante: usar la clave real
                    .parseClaimsJws(token.replace("Bearer ", ""))
                    .getBody();

            // Asegúrate que el claim `userId` realmente exista en el JWT
            this.userId = Long.parseLong(claims.get("userId").toString());

        } catch (Exception e) {
            this.userId = null;
            throw new RuntimeException("Error al decodificar token JWT: " + e.getMessage());
        }
    }


    public void clearToken() {
        this.token = null;
        this.userId = null;
    }
}
