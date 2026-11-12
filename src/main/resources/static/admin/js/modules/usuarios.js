import { API } from './api.js';
import { UI } from './ui.js';
import { validarUsuario, mostrarErrores, limpiarErrores } from './validacion.js';

// Campo del backend -> id del input del formulario
const CAMPOS = {
    nombre: 'm_nombre', apellido: 'm_apellido', correo: 'm_correo', identificacion: 'm_identificacion',
    tipoIdentificacion: 'm_tipoIdentificacion', edad: 'm_edad', rol: 'm_rol', cursoId: 'm_curso',
    'contraseña': 'm_password',
};

// Códigos de error del backend que se asocian a un campo concreto
const CODIGO_A_CAMPO = {
    USUARIO_IDENTIFICACION_DUPLICADA: 'identificacion',
    USUARIO_CORREO_DUPLICADO: 'correo',
    DOCUMENTO_LONGITUD_INVALIDA: 'identificacion',
    DOCUMENTO_FORMATO_INVALIDO: 'identificacion',
    DOCUMENTO_CC_MENOR_DE_EDAD: 'tipoIdentificacion',
    DOCUMENTO_TI_MAYOR_DE_EDAD: 'tipoIdentificacion',
    ESTUDIANTE_SIN_CURSO: 'cursoId',
    ESTUDIANTE_SIN_EDAD: 'edad',
};

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
            <button class="btn-small btn-edit" data-action="detalles" data-id="${user.id}">Detalles</button>
            <button class="btn-small ${user.activo ? 'btn-delete' : 'btn-edit'}" data-action="estado"
                data-id="${user.id}" data-activo="${user.activo}">
                ${user.activo ? 'Desactivar' : 'Activar'}
            </button>
        `);

        // Delegación de eventos (sin JavaScript en línea)
        document.getElementById('usersList').onclick = (event) => {
            const button = event.target.closest('button[data-action]');
            if (!button) return;
            const user = this.private_users.find(u => String(u.id) === button.dataset.id);
            if (!user) return;
            if (button.dataset.action === 'detalles') this.showDetails(user);
            if (button.dataset.action === 'estado') this.toggleEstado(user);
        };
    },

    showDetails(user) {
        const fila = (k, v) => `<dt>${UI.escapeHtml(k)}</dt><dd>${UI.escapeHtml(v ?? '-')}</dd>`;
        UI.showModal(`${UI.escapeHtml(user.nombre)} ${UI.escapeHtml(user.apellido)}`, `
            <dl class="detail-list">
                ${fila('Identificación', `${user.tipoIdentificacion || ''} ${user.identificacion}`)}
                ${fila('Correo', user.correo)}
                ${fila('Rol', user.rol)}
                ${fila('Edad', user.edad)}
                ${fila('Curso', user.cursoNombre)}
                ${fila('Estado', user.activo ? 'Activo' : 'Inactivo')}
                ${fila('Creado', user.creadoEn)}
            </dl>`, () => UI.hideModal());
    },

    async toggleEstado(user) {
        const activar = !user.activo;
        if (!activar && !confirm(`¿Desactivar a ${user.nombre} ${user.apellido}? No podrá iniciar sesión.`)) return;
        try {
            await API.put(`/api/admin/usuarios/${user.id}/estado?activo=${activar}`);
            UI.showAlert(activar ? 'Usuario activado' : 'Usuario desactivado');
            await this.loadUsers();
        } catch (error) {
            UI.showAlert(error.userMessage || error.message, 'error');
        }
    },

    async showCreateUserModal() {
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
                            <option value="CC">Cédula (≥ 18 años)</option>
                            <option value="TI">Tarjeta Identidad (&lt; 18 años)</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label>Identificación</label>
                        <input type="text" id="m_identificacion" inputmode="numeric" required>
                        <small class="form-hint" id="m_identificacion_hint">CC: 6–10 dígitos</small>
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
                <div class="form-group" id="m_curso_group" style="display: none">
                    <label>Curso (obligatorio para estudiantes)</label>
                    <select id="m_curso" class="form-control"></select>
                </div>
                <div class="form-group">
                    <label>Contraseña (mínimo 8 caracteres)</label>
                    <input type="password" id="m_password" minlength="8" required>
                </div>
            </form>
        `;

        UI.showModal('Registrar Nuevo Usuario', content, async () => {
            const form = document.getElementById('userForm');
            const data = {
                nombre: document.getElementById('m_nombre').value.trim(),
                apellido: document.getElementById('m_apellido').value.trim(),
                identificacion: document.getElementById('m_identificacion').value.trim(),
                tipoIdentificacion: document.getElementById('m_tipoIdentificacion').value,
                correo: document.getElementById('m_correo').value.trim(),
                contraseña: document.getElementById('m_password').value,
                rol: document.getElementById('m_rol').value,
                edad: document.getElementById('m_edad').value || null,
                sexo: document.getElementById('m_sexo').value,
                cursoId: document.getElementById('m_rol').value === 'ESTUDIANTE'
                    ? (document.getElementById('m_curso').value || null) : null,
                activo: true
            };

            // 1. Validación local: errores inmediatos y junto a cada campo
            const errores = validarUsuario(data);
            if (Object.keys(errores).length > 0) {
                mostrarErrores(form, errores, CAMPOS);
                return;
            }
            limpiarErrores(form);

            // 2. Envío: los errores del servidor se muestran en el campo correspondiente
            try {
                await API.post('/api/admin/usuarios', data);
                UI.showAlert('Usuario creado exitosamente');
                UI.hideModal();
                await this.loadUsers();
            } catch (error) {
                const porCampo = { ...(error.code === 'CAMPOS_INVALIDOS' ? error.details : {}) };
                const campo = CODIGO_A_CAMPO[error.code];
                if (campo) porCampo[campo] = error.message;
                if (Object.keys(porCampo).length > 0) {
                    mostrarErrores(form, porCampo, CAMPOS);
                }
                UI.showAlert(error.userMessage || error.message, 'error');
            }
        });

        // Pista dinámica de longitud según el tipo de documento
        const tipo = document.getElementById('m_tipoIdentificacion');
        const hint = document.getElementById('m_identificacion_hint');
        tipo.addEventListener('change', () => {
            hint.textContent = tipo.value === 'TI' ? 'TI: exactamente 10 dígitos' : 'CC: 6–10 dígitos';
        });

        await this.setupCursoSelect();
    },

    /** Muestra y rellena el selector de curso solo cuando el rol es ESTUDIANTE. */
    async setupCursoSelect() {
        const rolSelect = document.getElementById('m_rol');
        const group = document.getElementById('m_curso_group');
        const select = document.getElementById('m_curso');
        const toggle = () => { group.style.display = rolSelect.value === 'ESTUDIANTE' ? 'block' : 'none'; };
        rolSelect.addEventListener('change', toggle);
        toggle();
        try {
            const cursos = await API.get('/api/admin/cursos') || [];
            select.replaceChildren(...cursos.map(c => {
                const option = document.createElement('option');
                option.value = c.id;
                option.textContent = c.nombre || `${c.grado}°${c.grupo}`;
                return option;
            }));
        } catch (error) {
            UI.showAlert('No se pudieron cargar los cursos: ' + error.message, 'error');
        }
    },
};
