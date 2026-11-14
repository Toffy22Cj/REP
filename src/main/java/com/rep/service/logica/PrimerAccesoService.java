package com.rep.service.logica;

import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Contraseña provisional del administrador inicial, solo en memoria.
 *
 * <p>Se puede mostrar <b>una vez</b> en la página de inicio de sesión del panel web
 * abierta en el propio servidor. Nunca se guarda en disco en claro: si el servidor se
 * reinicia antes de que el administrador la cambie, se genera otra (ver DataLoader).</p>
 */
@Service
public class PrimerAccesoService {

    public record Credencial(String identificacion, String contrasena) {
    }

    private Credencial pendiente;
    private boolean mostrada;

    public synchronized void registrar(String identificacion, String contrasena) {
        pendiente = new Credencial(identificacion, contrasena);
        mostrada = false;
    }

    /** Entrega la credencial la primera vez y la marca como mostrada. */
    public synchronized Optional<Credencial> revelarUnaVez() {
        if (pendiente == null || mostrada) {
            return Optional.empty();
        }
        mostrada = true;
        return Optional.of(pendiente);
    }

    public synchronized boolean pendiente() {
        return pendiente != null;
    }

    public synchronized boolean yaMostrada() {
        return mostrada;
    }

    /** Al cambiar la contraseña la provisional deja de existir. */
    public synchronized void olvidar(String identificacion) {
        if (pendiente != null && pendiente.identificacion().equals(identificacion)) {
            pendiente = null;
        }
    }
}
