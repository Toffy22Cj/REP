// JavaScript para manejo de errores en login UI
document.addEventListener('DOMContentLoaded', () => {
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.has('error')) {
        const alert = document.getElementById('alert');
        alert.textContent = 'Credenciales inválidas. Por favor intente de nuevo.';
        alert.style.display = 'block';
    }
});

// Primer acceso: el servidor entrega la contraseña provisional una sola vez y solo si
// esta página se abrió en el propio equipo servidor.
document.addEventListener('DOMContentLoaded', async () => {
    let datos;
    try {
        const r = await fetch('/admin/primer-acceso', { credentials: 'same-origin' });
        if (!r.ok) return;
        datos = await r.json();
    } catch (_) {
        return;
    }
    if (datos.mostrar) {
        document.getElementById('paIdent').textContent = datos.identificacion;
        document.getElementById('paClave').textContent = datos.contrasena;
        document.getElementById('primerAcceso').hidden = false;
        document.getElementById('paUsar').addEventListener('click', () => {
            document.getElementById('identificacion').value = datos.identificacion;
            document.getElementById('password').value = datos.contrasena;
            document.getElementById('loginForm').submit();
        });
        document.getElementById('paCopiar').addEventListener('click', async () => {
            try {
                await navigator.clipboard.writeText(datos.contrasena);
                document.getElementById('paCopiar').textContent = 'Copiada';
            } catch (_) {
                // Sin portapapeles (http): el usuario la copia a mano
            }
        });
    } else if (datos.pendiente && datos.motivo) {
        const aviso = document.getElementById('primerAccesoAviso');
        aviso.textContent = datos.motivo;
        aviso.hidden = false;
    }
});
