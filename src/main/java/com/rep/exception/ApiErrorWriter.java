package com.rep.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;

/**
 * Escribe un {@link ApiError} directamente en la respuesta. Lo usan los filtros y
 * manejadores de Spring Security, que actúan antes de llegar a los controladores y
 * por tanto no pasan por {@link GlobalExceptionHandler}.
 */
public final class ApiErrorWriter {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ApiErrorWriter() {
    }

    public static void write(HttpServletRequest request, HttpServletResponse response, ErrorCode code) throws IOException {
        write(request, response, code, null);
    }

    public static void write(HttpServletRequest request, HttpServletResponse response, ErrorCode code, String message)
            throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(code.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        MAPPER.writeValue(response.getWriter(), ApiError.of(code, message, request.getRequestURI(), null, null));
    }
}
