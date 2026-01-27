import { Usuarios } from './modules/usuarios.js';
import { Academico } from './modules/academico.js';
import { Auditoria } from './modules/auditoria.js';
import { API } from './modules/api.js';

document.addEventListener('DOMContentLoaded', () => {
    const navLinks = document.querySelectorAll('.sidebar nav a');
    const pageTitle = document.getElementById('pageTitle');
    const btnLogout = document.getElementById('btnLogout');
    const btnShutdown = document.getElementById('btnShutdown');
    const containerId = 'mainContent';

    // Navigation logic
    navLinks.forEach(link => {
        link.addEventListener('click', (e) => {
            if (link.id === 'btnLogout' || link.id === 'btnShutdown') return;
            
            e.preventDefault();
            
            // Update active state
            navLinks.forEach(l => l.classList.remove('active'));
            link.classList.add('active');
            
            // Update title
            pageTitle.textContent = link.textContent;
            
            // Load module
            const moduleName = link.getAttribute('href').replace('#', '');
            loadModule(moduleName);
        });
    });

    // Logout logic
    btnLogout.addEventListener('click', (e) => {
        e.preventDefault();
        if (confirm('¿Cerrar sesión?')) {
            window.location.href = '/admin/logout';
        }
    });

    // Shutdown logic
    btnShutdown.addEventListener('click', async (e) => {
        e.preventDefault();
        if (confirm('⚠️ ¿Estás seguro de que deseas APAGAR el servidor?\nPerderás acceso a la interfaz hasta que se reinicie manualmente.')) {
            try {
                const response = await fetch('/api/admin/system/shutdown', { method: 'POST' });
                if (response.ok) {
                    document.body.innerHTML = `
                        <div style="height: 100vh; display: flex; align-items: center; justify-content: center; background: #0f172a; color: white; text-align: center; font-family: sans-serif;">
                            <div>
                                <h1>Sistema Apagado</h1>
                                <p>El servidor se ha detenido satisfactoriamente.</p>
                                <p style="color: #94a3b8">Ya puedes cerrar esta ventana.</p>
                            </div>
                        </div>
                    `;
                }
            } catch (error) {
                alert('Error al intentar apagar el sistema.');
            }
        }
    });

    async function loadModule(moduleName) {
        console.log('Cargando módulo:', moduleName);
        const contentArea = document.getElementById(containerId);
        
        switch(moduleName) {
            case 'usuarios':
                await Usuarios.init(containerId);
                break;
            case 'academico':
                await Academico.init(containerId);
                break;
            case 'auditoria':
                await Auditoria.init(containerId);
                break;
            case 'archivos':
                contentArea.innerHTML = '<div class="card"><h3>Gestión de Archivos</h3><p>Módulo de archivos en construcción (v2.1).</p></div>';
                break;
            default:
                await loadDashboard(contentArea);
        }
    }

    async function loadDashboard(container) {
        container.innerHTML = '<div class="card"><h3>Cargando Dashboard...</h3></div>';
        try {
            const stats = await API.get('/api/admin/stats');
            container.innerHTML = `
                <div class="card">
                    <h3>Dashboard Global</h3>
                    <p>Resumen del estado actual del sistema educativo.</p>
                    <div style="margin-top: 1.5rem; display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 1rem;">
                        <div class="card" style="border-left: 4px solid var(--primary)">
                            <small>Usuarios Totales</small>
                            <h2>${stats.usuarios}</h2>
                        </div>
                        <div class="card" style="border-left: 4px solid var(--success)">
                            <small>Estudiantes</small>
                            <h2>${stats.estudiantes}</h2>
                        </div>
                        <div class="card" style="border-left: 4px solid #f59e0b">
                            <small>Profesores</small>
                            <h2>${stats.profesores}</h2>
                        </div>
                        <div class="card" style="border-left: 4px solid #8b5cf6">
                            <small>Cursos</small>
                            <h2>${stats.cursos}</h2>
                        </div>
                    </div>
                    
                    <div class="card" style="margin-top: 1.5rem; border-top: 3px solid var(--primary)">
                        <h4>Estado del Servidor</h4>
                        <p>Plataforma: <strong>REST API / PWA</strong></p>
                        <p>Seguridad: <strong>SSL-ready / JWT + Session</strong></p>
                    </div>
                </div>
            `;
        } catch (error) {
            container.innerHTML = '<div class="card"><h3>Error al cargar Dashboard</h3><p>' + error.message + '</p></div>';
        }
    }

    // Default module
    loadModule('dashboard');
});
