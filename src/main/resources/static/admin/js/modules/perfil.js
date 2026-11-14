import { API } from './api.js';
import { UI, escapeHtml } from './ui.js';

/**
 * Mi perfil del administrador: datos personales y cambio de contraseña.
 * Usa /api/admin/perfil (misma lógica que /api/perfil de los clientes de escritorio).
 */
export const Perfil = {
    async init(containerId, opciones = {}) {
        const container = document.getElementById(containerId);
        UI.showLoading(containerId);
        let perfil;
        try {
            perfil = await API.get('/api/admin/perfil');
        } catch (error) {
            UI.showError(containerId, error.message);
            return;
        }

        const banner = opciones.obligatorio ? `
            <div class="obligatorio-banner">
                <strong>⚠ Cambie la contraseña provisional</strong><br>
                Por seguridad, el panel se habilita cuando defina una contraseña propia.
                Guárdela en un lugar seguro: no se puede recuperar desde otro equipo.
            </div>` : '';
        container.innerHTML = banner + `
            <div class="card">
                <h3>Datos personales</h3>
                <p class="text-muted">${escapeHtml(perfil.tipoIdentificacion || '')} ${escapeHtml(perfil.identificacion)} · Administrador</p>
                <form id="formPerfil" style="margin-top: 1rem; max-width: 560px;">
                    <div class="form-group"><label for="p_nombre">Nombre</label>
                        <input id="p_nombre" required minlength="2" maxlength="50" value="${escapeHtml(perfil.nombre)}"></div>
                    <div class="form-group"><label for="p_apellido">Apellido</label>
                        <input id="p_apellido" required minlength="2" maxlength="50" value="${escapeHtml(perfil.apellido)}"></div>
                    <div class="form-group"><label for="p_correo">Correo</label>
                        <input id="p_correo" type="email" required value="${escapeHtml(perfil.correo)}"></div>
                    <button class="btn" type="submit" style="width: auto; padding: 0.5rem 1.25rem;">Guardar cambios</button>
                </form>
            </div>
            <div class="card" style="margin-top: 1.5rem;">
                <h3>Cambiar contraseña</h3>
                <p class="text-muted">Mínimo 8 caracteres con letras y números, distinta de la identificación y de la actual.
                   Las sesiones abiertas con la contraseña anterior en los clientes de escritorio se cierran.</p>
                <form id="formClave" style="margin-top: 1rem; max-width: 560px;" autocomplete="off">
                    <div class="form-group"><label for="c_actual">Contraseña actual</label>
                        <input id="c_actual" type="password" required autocomplete="current-password"></div>
                    <div class="form-group"><label for="c_nueva">Nueva contraseña</label>
                        <input id="c_nueva" type="password" required minlength="8" autocomplete="new-password"></div>
                    <div class="form-group"><label for="c_confirmar">Confirmar nueva contraseña</label>
                        <input id="c_confirmar" type="password" required minlength="8" autocomplete="new-password"></div>
                    <button class="btn" type="submit" style="width: auto; padding: 0.5rem 1.25rem;">Actualizar contraseña</button>
                </form>
            </div>
        `;

        document.getElementById('formPerfil').addEventListener('submit', async (e) => {
            e.preventDefault();
            try {
                const actualizado = await API.put('/api/admin/perfil', {
                    nombre: document.getElementById('p_nombre').value.trim(),
                    apellido: document.getElementById('p_apellido').value.trim(),
                    correo: document.getElementById('p_correo').value.trim(),
                    sexo: perfil.sexo || null,
                });
                Perfil.mostrarUsuario(actualizado);
                UI.showAlert('Datos actualizados');
            } catch (error) {
                UI.showAlert(error.message, 'error');
            }
        });

        document.getElementById('formClave').addEventListener('submit', async (e) => {
            e.preventDefault();
            const nueva = document.getElementById('c_nueva').value;
            if (nueva !== document.getElementById('c_confirmar').value) {
                UI.showAlert('La confirmación no coincide', 'error');
                return;
            }
            if (!/[A-Za-z]/.test(nueva) || !/\d/.test(nueva)) {
                UI.showAlert('La contraseña debe combinar letras y números', 'error');
                return;
            }
            try {
                await API.put('/api/admin/perfil/contrasena', {
                    actual: document.getElementById('c_actual').value,
                    nueva,
                });
                e.target.reset();
                UI.showAlert('Contraseña actualizada');
                if (opciones.obligatorio) {
                    setTimeout(() => window.location.reload(), 900); // habilita el panel completo
                }
            } catch (error) {
                UI.showAlert(error.message, 'error');
            }
        });
    },

    /** Muestra el nombre del administrador en la barra superior. */
    mostrarUsuario(perfil) {
        const destino = document.querySelector('.top-bar .user-info');
        if (destino && perfil) destino.textContent = `${perfil.nombre} ${perfil.apellido}`;
    },

    async cargarUsuarioActual() {
        try {
            const perfil = await API.get('/api/admin/perfil');
            Perfil.mostrarUsuario(perfil);
            return perfil;
        } catch (_) {
            // La barra superior es informativa; si falla se deja vacía
            return null;
        }
    },
};
