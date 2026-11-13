package com.rep.service.logica;

import com.rep.model.AuditLog;
import com.rep.repositories.AuditLogRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

/** Registro de acciones administrativas (quién hizo qué). */
@Service
public class AuditoriaService {
    private final AuditLogRepository auditLogRepository;

    public AuditoriaService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void registrar(String accion, String detalles) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String usuario = auth != null ? auth.getName() : "sistema";
        auditLogRepository.save(new AuditLog(usuario, accion, detalles));
    }

    /** Últimos registros (el historial completo puede crecer sin límite). */
    public List<AuditLog> listar(int limite) {
        int tamano = Math.max(1, Math.min(limite, 5000));
        return auditLogRepository.findAllByOrderByTimestampDesc(org.springframework.data.domain.PageRequest.of(0, tamano));
    }
}
