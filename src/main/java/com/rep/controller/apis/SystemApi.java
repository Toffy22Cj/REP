package com.rep.controller.apis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/system")
@Slf4j
public class SystemApi {

    @Autowired
    private ApplicationContext context;

    @PostMapping("/shutdown")
    public ResponseEntity<?> shutdown() {
        log.warn("Solicitud de apagado del sistema recibida.");

        // Ejecutar en un nuevo hilo para permitir que la respuesta HTTP se envíe
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                log.warn("Apagando servidor...");
                SpringApplication.exit(context, () -> 0);
                System.exit(0);
            } catch (Exception e) {
                log.error("Error durante el apagado: ", e);
            }
        }).start();

        return ResponseEntity.ok(Map.of(
                "message", "El servidor se apagará en un momento.",
                "status", "SHUTDOWN_INITIATED"));
    }

    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        return ResponseEntity.ok(Map.of(
                "uptime", System.currentTimeMillis(), // Simulado
                "memory", Runtime.getRuntime().totalMemory(),
                "status", "RUNNING"));
    }
}
