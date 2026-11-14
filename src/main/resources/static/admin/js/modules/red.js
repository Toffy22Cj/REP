import { API } from './api.js';
import { UI, escapeHtml } from './ui.js';

const TIPOS = {
    ETHERNET: ['🔌', 'Cable (LAN)'],
    WIFI: ['📶', 'Wi-Fi'],
    HOTSPOT: ['📡', 'Red propia (hotspot)'],
    BLUETOOTH: ['🔵', 'Bluetooth (PAN)'],
    VIRTUAL: ['🧩', 'Virtual / VPN'],
    OTRA: ['🌐', 'Otra'],
};

/**
 * Red del servidor: cómo lo encuentran los clientes, su nombre, el descubrimiento
 * automático y la red Wi-Fi propia (hotspot).
 */
export const Red = {
    async init(containerId) {
        UI.showLoading(containerId);
        try {
            this.render(containerId, await API.get('/api/admin/red'));
        } catch (error) {
            UI.showError(containerId, error.message);
        }
    },

    render(containerId, red) {
        const container = document.getElementById(containerId);
        const utiles = red.interfaces.filter(i => i.tipo !== 'VIRTUAL');
        const virtuales = red.interfaces.filter(i => i.tipo === 'VIRTUAL');
        const filaInterfaz = i => {
            const [icono, tipo] = TIPOS[i.tipo] || TIPOS.OTRA;
            return `<tr>
                <td>${icono} ${escapeHtml(tipo)}</td>
                <td><code>${escapeHtml(i.url)}</code></td>
                <td class="text-muted">${escapeHtml(i.nombre)}${i.descripcion && i.descripcion !== i.nombre ? ' · ' + escapeHtml(i.descripcion) : ''}</td>
            </tr>`;
        };
        const d = red.descubrimiento;
        const h = red.hotspot;

        container.innerHTML = `
            <div class="card">
                <h3>Identidad del servidor</h3>
                <p class="text-muted">Los clientes de escritorio encuentran este servidor solos en la red (cable, Wi-Fi,
                   red propia o Bluetooth PAN), con o sin internet, y lo reconocen aunque cambie de dirección IP.</p>
                <form id="formRed" style="margin-top:1rem; max-width:620px">
                    <div class="form-group"><label for="r_nombre">Nombre visible para los clientes</label>
                        <input id="r_nombre" maxlength="60" required value="${escapeHtml(red.nombre)}"></div>
                    <div class="form-group" style="display:flex; gap:.6rem; align-items:center">
                        <input type="checkbox" id="r_desc" ${d.activo ? 'checked' : ''} style="width:auto">
                        <label for="r_desc" style="margin:0">Anunciar el servidor en la red (descubrimiento automático)</label>
                    </div>
                    <p class="text-muted" style="font-size:.85rem">
                        Estado: ${d.escuchando ? '🟢 activo' : (d.activo ? '🟠 sin puerto (otro programa lo usa)' : '⚪ desactivado')}
                        · UDP ${d.puerto} (búsquedas) y ${d.puertoAnuncio} (anuncios) · HTTP ${red.puertoHttp}
                        · mDNS ${d.mdns ? '🟢 _rep._tcp' : '⚪'} · Versión ${escapeHtml(red.version)} · ID <code>${escapeHtml(red.id.substring(0, 8))}</code></p>
                    <button class="btn" type="submit" style="width:auto; padding:.5rem 1.25rem">Guardar</button>
                </form>
            </div>

            <div class="card" style="margin-top:1.5rem">
                <h3>Direcciones para conectarse</h3>
                <p class="text-muted">Si un equipo no encuentra el servidor solo, escriba una de estas direcciones en
                   «Servidor › Escribir dirección» del cliente. Use la de la misma red que ese equipo.</p>
                ${utiles.length === 0 ? '<p class="alert alert-error" style="display:block">Este equipo no está conectado a ninguna red. Conéctelo por cable, a un Wi-Fi o cree una red propia abajo.</p>' : `
                <table class="admin-table"><thead><tr><th>Red</th><th>Dirección</th><th>Interfaz</th></tr></thead>
                <tbody>${utiles.map(filaInterfaz).join('')}</tbody></table>`}
                ${virtuales.length ? `<details style="margin-top:.75rem"><summary class="text-muted">Interfaces virtuales (${virtuales.length})</summary>
                    <table class="admin-table"><tbody>${virtuales.map(filaInterfaz).join('')}</tbody></table></details>` : ''}
                <p class="text-muted" style="font-size:.85rem; margin-top:.75rem">
                    El mismo equipo servidor puede usar el cliente: lo encuentra en <code>http://localhost:${red.puertoHttp}</code>.</p>
            </div>

            <div class="card" style="margin-top:1.5rem">
                <h3>Red Wi-Fi propia (sin router)</h3>
                ${h.soportado ? `
                    <p class="text-muted">El servidor crea una red Wi-Fi a la que se conectan los demás equipos. No necesita internet.
                       ${h.activo ? '<br><strong>🟢 La red propia está activa.</strong>' : ''}</p>
                    <form id="formHotspot" style="margin-top:1rem; max-width:620px" autocomplete="off">
                        <div class="form-group"><label for="h_ssid">Nombre de la red</label>
                            <input id="h_ssid" maxlength="32" value="REP-Colegio" required></div>
                        <div class="form-group"><label for="h_clave">Clave (8 a 63 caracteres)</label>
                            <input id="h_clave" type="text" minlength="8" maxlength="63" required></div>
                        <button class="btn" type="submit" style="width:auto; padding:.5rem 1.25rem">Crear red</button>
                        ${h.activo ? '<button class="btn btn-secondary" type="button" id="h_detener" style="width:auto; padding:.5rem 1.25rem; margin-left:.5rem">Detener red</button>' : ''}
                    </form>
                    <p class="text-muted" style="font-size:.85rem; margin-top:.5rem">Al crearla, este equipo deja la red Wi-Fi a la que estaba conectado.</p>`
                : `<p class="text-muted">${escapeHtml(h.instrucciones)}</p>
                   ${!h.permitido ? '<p class="text-muted">La creación desde REP está desactivada (REP_HOTSPOT=false).</p>' : ''}`}
            </div>

            <div class="card" style="margin-top:1.5rem">
                <h3>Equipos que buscaron el servidor</h3>
                ${red.clientesRecientes.length === 0 ? '<p class="text-muted">Aún no hay búsquedas desde otros equipos.</p>' : `
                <table class="admin-table"><thead><tr><th>Dirección</th><th>Última búsqueda</th></tr></thead><tbody>
                ${red.clientesRecientes.map(c => `<tr><td><code>${escapeHtml(c.ip)}</code></td>
                    <td>${escapeHtml(new Date(c.cuando).toLocaleString('es-CO'))}</td></tr>`).join('')}</tbody></table>`}
            </div>

            <div class="card" style="margin-top:1.5rem">
                <h3>Si un equipo no encuentra el servidor</h3>
                <ul class="text-muted" style="line-height:1.7; padding-left:1.2rem">
                    <li>Ambos equipos deben estar en la misma red (mismo Wi-Fi, cable al mismo router, red propia o Bluetooth PAN).</li>
                    <li>Permita en el cortafuegos del servidor: TCP ${red.puertoHttp} y UDP ${d.puerto} y ${d.puertoAnuncio}
                        (el instalador de Windows crea estas reglas).</li>
                    <li>Algunas redes de invitados aíslan a los equipos entre sí: use la red propia del servidor.</li>
                    <li>Bluetooth: empareje los equipos y active «Compartir red por Bluetooth» (PAN); aparecerá arriba como Bluetooth.</li>
                </ul>
            </div>`;

        document.getElementById('formRed').addEventListener('submit', async e => {
            e.preventDefault();
            try {
                const nuevo = await API.put('/api/admin/red', {
                    nombre: document.getElementById('r_nombre').value.trim(),
                    descubrimiento: document.getElementById('r_desc').checked,
                });
                UI.showAlert('Configuración de red guardada');
                this.render(containerId, nuevo);
            } catch (error) {
                UI.showAlert(error.message, 'error');
            }
        });
        const formHotspot = document.getElementById('formHotspot');
        if (formHotspot) {
            formHotspot.addEventListener('submit', async e => {
                e.preventDefault();
                try {
                    const nuevo = await API.post('/api/admin/red/hotspot', {
                        ssid: document.getElementById('h_ssid').value.trim(),
                        clave: document.getElementById('h_clave').value,
                    });
                    UI.showAlert('Red Wi-Fi creada');
                    this.render(containerId, nuevo);
                } catch (error) {
                    UI.showAlert(error.message, 'error');
                }
            });
        }
        const detener = document.getElementById('h_detener');
        if (detener) {
            detener.addEventListener('click', async () => {
                try {
                    this.render(containerId, await API.delete('/api/admin/red/hotspot'));
                    UI.showAlert('Red Wi-Fi detenida');
                } catch (error) {
                    UI.showAlert(error.message, 'error');
                }
            });
        }
    },
};
