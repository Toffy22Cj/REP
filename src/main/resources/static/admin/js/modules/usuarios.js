import { API } from './api.js';
import { UI } from './ui.js';

export const Usuarios = {
    async init(containerId) {
        const container = document.getElementById(containerId);
        container.innerHTML = `
            <div class="card">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem;">
                    <h3>Gestión de Usuarios</h3>
                    <button id="btnNewUser" class="btn" style="width: auto; padding: 0.5rem 1rem;">+ Nuevo Usuario</button>
                </div>
                <div id="usersFilters"></div>
                <div id="usersList"></div>
            </div>
        `;

        UI.renderFilters('usersFilters', {
            onSearch: (val) => this.filterUsers(val),
            filters: [
                { id: 'rol', label: 'Filtrar por Rol', options: [
                    { value: 'ADMIN', label: 'Administrador' },
                    { value: 'PROFESOR', label: 'Profesor' },
                    { value: 'ESTUDIANTE', label: 'Estudiante' }
                ]},
                { id: 'estado', label: 'Filtrar por Estado', options: [
                    { value: 'true', label: 'Activo' },
                    { value: 'false', label: 'Inactivo' }
                ]}
            ]
        });

        document.getElementById('btnNewUser').onclick = () => this.showCreateUserModal();
        
        await this.loadUsers();
    },

    private_users: [], // Cache for filtering

    async loadUsers() {
        UI.showLoading('usersList');
        try {
            this.private_users = await API.get('/api/admin/usuarios');
            this.renderUsersTable(this.private_users);
        } catch (error) {
            UI.showError('usersList', error.message);
        }
    },

    filterUsers() {
        const searchTerm = document.getElementById('globalSearch').value.toLowerCase();
        const roleFilter = document.getElementById('filter-rol').value;
        const statusFilter = document.getElementById('filter-estado').value;

        const filtered = this.private_users.filter(u => {
            const matchesSearch = !searchTerm || 
                u.nombre.toLowerCase().includes(searchTerm) || 
                u.apellido.toLowerCase().includes(searchTerm) || 
                u.identificacion.includes(searchTerm) ||
                u.correo.toLowerCase().includes(searchTerm);
            
            const matchesRole = !roleFilter || u.rol === roleFilter;
            const matchesStatus = !statusFilter || String(u.activo) === statusFilter;

            return matchesSearch && matchesRole && matchesStatus;
        });

        this.renderUsersTable(filtered);
    },

    renderUsersTable(users) {
        const columns = [
            { label: 'Id', key: 'id' },
            { label: 'Identificacion', key: 'identificacion' },
            { label: 'Nombre', key: 'nombre' },
            { label: 'Apellido', key: 'apellido' },
            { label: 'Rol', key: 'rol' },
            { label: 'Activo', key: 'activo' }
        ];

        UI.renderTable('usersList', columns, users, (user) => `
            <button class="btn-small btn-edit" onclick="alert('Detalles de ${user.nombre} en v2.1')">Detalles</button>
            <button class="btn-small ${user.activo ? 'btn-delete' : 'btn-edit'}" 
                onclick="alert('Cambio de estado para ${user.identificacion} en v2.1')">
                ${user.activo ? 'Desactivar' : 'Activar'}
            </button>
        `);
    },

    showCreateUserModal() {
        const content = `
            <form id="userForm">
                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem;">
                    <div class="form-group">
                        <label>Nombres</label>
                        <input type="text" id="m_nombre" required>
                    </div>
                    <div class="form-group">
                        <label>Apellidos</label>
                        <input type="text" id="m_apellido" required>
                    </div>
                </div>
                <div class="form-group">
                    <label>Correo Electrónico</label>
                    <input type="email" id="m_correo" required>
                </div>
                <div style="display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 1rem;">
                    <div class="form-group">
                        <label>Tipo Id</label>
                        <select id="m_tipoIdentificacion" class="form-control">
                            <option value="CC">Cédula Ciudadanía</option>
                            <option value="TI">Tarjeta Identidad</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label>Identificación</label>
                        <input type="text" id="m_identificacion" required>
                    </div>
                    <div class="form-group">
                        <label>Edad</label>
                        <input type="number" id="m_edad" min="5" max="120">
                    </div>
                </div>
                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem;">
                    <div class="form-group">
                        <label>Sexo</label>
                        <select id="m_sexo" class="form-control">
                            <option value="MASCULINO">Masculino</option>
                            <option value="FEMENINO">Femenino</option>
                            <option value="OTRO">Otro</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label>Rol</label>
                        <select id="m_rol" class="form-control">
                            <option value="ADMIN">Administrador</option>
                            <option value="PROFESOR">Profesor</option>
                            <option value="ESTUDIANTE">Estudiante</option>
                        </select>
                    </div>
                </div>
                <div class="form-group">
                    <label>Contraseña</label>
                    <input type="password" id="m_password" required>
                </div>
            </form>
        `;

        UI.showModal('Registrar Nuevo Usuario', content, async () => {
            const data = {
                nombre: document.getElementById('m_nombre').value,
                apellido: document.getElementById('m_apellido').value,
                identificacion: document.getElementById('m_identificacion').value,
                tipoIdentificacion: document.getElementById('m_tipoIdentificacion').value,
                correo: document.getElementById('m_correo').value,
                contraseña: document.getElementById('m_password').value,
                rol: document.getElementById('m_rol').value,
                edad: document.getElementById('m_edad').value,
                sexo: document.getElementById('m_sexo').value,
                activo: true
            };

            try {
                await API.post('/api/admin/usuarios', data);
                UI.showAlert('Usuario creado exitosamente');
                UI.hideModal();
                await this.loadUsers();
            } catch (error) {
                UI.showAlert(error.message, 'error');
            }
        });
    },
};
