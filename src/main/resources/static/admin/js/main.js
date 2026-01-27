document.addEventListener('DOMContentLoaded', () => {
    const navLinks = document.querySelectorAll('.sidebar nav a');
    const pageTitle = document.getElementById('pageTitle');
    const btnLogout = document.getElementById('btnLogout');

    // Navigation logic
    navLinks.forEach(link => {
        link.addEventListener('click', (e) => {
            if (link.id === 'btnLogout') return;
            
            e.preventDefault();
            
            // Update active state
            navLinks.forEach(l => l.classList.remove('active'));
            link.classList.add('active');
            
            // Update title
            pageTitle.textContent = link.textContent;
            
            // Load content (placeholder for now)
            loadModule(link.getAttribute('href').replace('#', ''));
        });
    });

    // Logout logic
    btnLogout.addEventListener('click', (e) => {
        e.preventDefault();
        if (confirm('¿Cerrar sesión?')) {
            window.location.href = '/admin/logout';
        }
    });

    function loadModule(moduleName) {
        console.log('Cargando módulo:', moduleName);
        const contentArea = document.getElementById('mainContent');
        
        // Basic routing switch
        switch(moduleName) {
            case 'usuarios':
                contentArea.innerHTML = '<div class="card"><h3>Gestión de Usuarios</h3><p>Cargando lista de profesores y estudiantes...</p></div>';
                break;
            case 'academico':
                contentArea.innerHTML = '<div class="card"><h3>Gestión Académica</h3><p>Cargando cursos y materias...</p></div>';
                break;
            default:
                contentArea.innerHTML = '<div class="card"><h3>Dashboard</h3><p>Bienvenido al sistema.</p></div>';
        }
    }
});
