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

    public List<AuditLog> listar() {
        return auditLogRepository.findAllByOrderByTimestampDesc();
    }
}
