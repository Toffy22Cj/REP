import { API } from "./api.js";
import { UI } from "./ui.js";

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

    UI.renderFilters("usersFilters", {
      onSearch: (val) => this.filterUsers(val),
      filters: [
        {
          id: "rol",
          label: "Filtrar por Rol",
          options: [
            { value: "ADMIN", label: "Administrador" },
            { value: "PROFESOR", label: "Profesor" },
            { value: "ESTUDIANTE", label: "Estudiante" },
          ],
        },
        {
          id: "estado",
          label: "Filtrar por Estado",
          options: [
            { value: "true", label: "Activo" },
            { value: "false", label: "Inactivo" },
          ],
        },
      ],
    });

    document.getElementById("btnNewUser").onclick = () =>
      this.showCreateUserModal();

    await this.loadUsers();
  },

  private_users: [], // Cache for filtering

  async loadUsers() {
    UI.showLoading("usersList");
    try {
      this.private_users = await API.get("/api/admin/usuarios");
      this.renderUsersTable(this.private_users);
    } catch (error) {
      UI.showError("usersList", error.message);
    }
  },

  filterUsers() {
    const searchTerm = document
      .getElementById("globalSearch")
      .value.toLowerCase();
    const roleFilter = document.getElementById("filter-rol").value;
    const statusFilter = document.getElementById("filter-estado").value;

    const filtered = this.private_users.filter((u) => {
      const matchesSearch =
        !searchTerm ||
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
      { label: "Id", key: "id" },
      { label: "Identificacion", key: "identificacion" },
      { label: "Nombre", key: "nombre" },
      { label: "Apellido", key: "apellido" },
      { label: "Correo", key: "correo" },
      { label: "Rol", key: "rol" },
      { label: "Activo", key: "activo" },
    ];

    const self = this;
    UI.renderTable(
      "usersList",
      columns,
      users,
      (user) => `
            <button class="btn-small btn-edit" onclick="window.usuariosModule.showUserDetail(${user.id})">Detalles</button>
            <button class="btn-small btn-edit" onclick="window.usuariosModule.showEditUserModal(${user.id})">Editar</button>
            <button class="btn-small ${user.activo ? "btn-delete" : "btn-edit"}" 
                onclick="window.usuariosModule.toggleUserStatus(${user.id}, ${!user.activo})">
                ${user.activo ? "Desactivar" : "Activar"}
            </button>
            <button class="btn-small btn-delete" onclick="window.usuariosModule.deleteUser(${user.id})">Eliminar</button>
        `,
    );
  },

  async showUserDetail(userId) {
    try {
      UI.showLoading("mainContent");
      const usuario = await API.get(`/api/admin/usuarios/${userId}`);

      const detail = `
                <div style="background: #f8f9fa; padding: 1.5rem; border-radius: 0.5rem; margin-top: 1rem;">
                    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; margin-bottom: 1rem;">
                        <div><strong>ID:</strong> ${usuario.id}</div>
                        <div><strong>Rol:</strong> ${usuario.rol}</div>
                        <div><strong>Nombre:</strong> ${usuario.nombre}</div>
                        <div><strong>Apellido:</strong> ${usuario.apellido}</div>
                        <div><strong>Correo:</strong> ${usuario.correo}</div>
                        <div><strong>Identificación:</strong> ${usuario.identificacion}</div>
                    </div>
                </div>
            `;

      UI.showAlert("Detalles del Usuario cargados");
      const container = document.getElementById("mainContent");
      const detailHtml = container.innerHTML;
      container.innerHTML = `
                <div class="card">
                    <h3>Detalles del Usuario</h3>
                    ${detail}
                    <button class="btn" onclick="window.location.hash='#usuarios'" style="margin-top: 1rem;">Volver</button>
                </div>
            `;
    } catch (error) {
      UI.showAlert(error.message, "error");
    }
  },

  async showEditUserModal(userId) {
    try {
      const usuario = await API.get(`/api/admin/usuarios/${userId}`);

      const content = `
                <form id="userForm">
                    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem;">
                        <div class="form-group">
                            <label>Nombres</label>
                            <input type="text" id="m_nombre" value="${usuario.nombre}" required>
                        </div>
                        <div class="form-group">
                            <label>Apellidos</label>
                            <input type="text" id="m_apellido" value="${usuario.apellido}" required>
                        </div>
                    </div>
                    <div class="form-group">
                        <label>Correo Electrónico</label>
                        <input type="email" id="m_correo" value="${usuario.correo}" required>
                    </div>
                    <div class="form-group">
                        <label>Rol</label>
                        <select id="m_rol" class="form-control">
                            <option value="ADMIN" ${usuario.rol === "ADMIN" ? "selected" : ""}>Administrador</option>
                            <option value="PROFESOR" ${usuario.rol === "PROFESOR" ? "selected" : ""}>Profesor</option>
                            <option value="ESTUDIANTE" ${usuario.rol === "ESTUDIANTE" ? "selected" : ""}>Estudiante</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label>Nueva Contraseña (dejar en blanco para no cambiar)</label>
                        <input type="password" id="m_password">
                    </div>
                    <div class="form-group">
                        <label>
                            <input type="checkbox" id="m_activo" ${usuario.activo ? "checked" : ""}>
                            Usuario Activo
                        </label>
                    </div>
                </form>
            `;

      UI.showModal("Editar Usuario", content, async () => {
        const data = {
          nombre: document.getElementById("m_nombre").value,
          apellido: document.getElementById("m_apellido").value,
          correo: document.getElementById("m_correo").value,
          rol: document.getElementById("m_rol").value,
          activo: document.getElementById("m_activo").checked,
        };

        const password = document.getElementById("m_password").value;
        if (password) {
          data.contraseña = password;
        }

        try {
          await API.put(`/api/admin/usuarios/${userId}`, data);
          UI.showAlert("Usuario actualizado exitosamente");
          UI.hideModal();
          await this.loadUsers();
        } catch (error) {
          UI.showAlert(error.message, "error");
        }
      });
    } catch (error) {
      UI.showAlert("Error al cargar usuario: " + error.message, "error");
    }
  },

  async toggleUserStatus(userId, newStatus) {
    if (
      !confirm(
        `¿Estás seguro de que deseas ${newStatus ? "activar" : "desactivar"} este usuario?`,
      )
    ) {
      return;
    }

    try {
      await API.put(
        `/api/admin/usuarios/${userId}/estado?activo=${newStatus}`,
        {},
      );
      UI.showAlert(
        `Usuario ${newStatus ? "activado" : "desactivado"} exitosamente`,
      );
      await this.loadUsers();
    } catch (error) {
      UI.showAlert(error.message, "error");
    }
  },

  async deleteUser(userId) {
    if (
      !confirm(
        "⚠️ ¿Estás seguro de que deseas ELIMINAR este usuario? Esta acción no se puede deshacer.",
      )
    ) {
      return;
    }

    try {
      await API.delete(`/api/admin/usuarios/${userId}`);
      UI.showAlert("Usuario eliminado exitosamente");
      await this.loadUsers();
    } catch (error) {
      UI.showAlert(error.message, "error");
    }
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

    UI.showModal("Registrar Nuevo Usuario", content, async () => {
      const data = {
        nombre: document.getElementById("m_nombre").value,
        apellido: document.getElementById("m_apellido").value,
        identificacion: document.getElementById("m_identificacion").value,
        tipoIdentificacion: document.getElementById("m_tipoIdentificacion")
          .value,
        correo: document.getElementById("m_correo").value,
        contraseña: document.getElementById("m_password").value,
        rol: document.getElementById("m_rol").value,
        edad: document.getElementById("m_edad").value,
        sexo: document.getElementById("m_sexo").value,
        activo: true,
      };

      try {
        await API.post("/api/admin/usuarios", data);
        UI.showAlert("Usuario creado exitosamente");
        UI.hideModal();
        await this.loadUsers();
      } catch (error) {
        UI.showAlert(error.message, "error");
      }
    });
  },
};

// Make module accessible globally for onclick handlers
window.usuariosModule = Usuarios;
