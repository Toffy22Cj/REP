import { API } from "./api.js";
import { UI } from "./ui.js";

export const Auditoria = {
  async init(containerId) {
    const container = document.getElementById(containerId);
    container.innerHTML = `
            <div class="card">
                <h3>Auditoría de Sistema</h3>
                <p class="text-muted">Registro histórico de acciones administrativas.</p>
                <div id="auditFilters" style="margin-top: 1rem; margin-bottom: 1.5rem;"></div>
                <div id="auditStats" style="display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 1rem; margin-bottom: 1.5rem;"></div>
                <div id="auditList"></div>
            </div>
        `;

    UI.renderFilters("auditFilters", {
      onSearch: (val) => this.filterLogs(val),
      filters: [
        {
          id: "action",
          label: "Filtrar por Acción",
          options: [
            { value: "CREAR_USUARIO", label: "Crear Usuario" },
            { value: "ACTUALIZAR_USUARIO", label: "Actualizar Usuario" },
            {
              value: "CAMBIAR_ESTADO_USUARIO",
              label: "Cambiar Estado Usuario",
            },
            { value: "ELIMINAR_USUARIO", label: "Eliminar Usuario" },
            { value: "CREAR_CURSO", label: "Crear Curso" },
            { value: "ACTUALIZAR_CURSO", label: "Actualizar Curso" },
            { value: "ELIMINAR_CURSO", label: "Eliminar Curso" },
            { value: "CREAR_MATERIA", label: "Crear Materia" },
            { value: "ELIMINAR_MATERIA", label: "Eliminar Materia" },
          ],
        },
      ],
    });

    await this.loadStats();
    await this.loadLogs();
  },

  private_logs: [],

  async loadStats() {
    try {
      const stats = await API.get("/api/admin/stats");
      const statsContainer = document.getElementById("auditStats");
      statsContainer.innerHTML = `
                <div style="background: #e0f2fe; padding: 1rem; border-radius: 0.5rem; text-align: center;">
                    <div style="font-size: 1.5rem; font-weight: bold; color: #0369a1;">${stats.usuarios}</div>
                    <div style="color: #0369a1;">Usuarios</div>
                </div>
                <div style="background: #fef3c7; padding: 1rem; border-radius: 0.5rem; text-align: center;">
                    <div style="font-size: 1.5rem; font-weight: bold; color: #b45309;">${stats.estudiantes}</div>
                    <div style="color: #b45309;">Estudiantes</div>
                </div>
                <div style="background: #dbeafe; padding: 1rem; border-radius: 0.5rem; text-align: center;">
                    <div style="font-size: 1.5rem; font-weight: bold; color: #1e40af;">${stats.profesores}</div>
                    <div style="color: #1e40af;">Profesores</div>
                </div>
                <div style="background: #dcfce7; padding: 1rem; border-radius: 0.5rem; text-align: center;">
                    <div style="font-size: 1.5rem; font-weight: bold; color: #15803d;">${stats.cursos}</div>
                    <div style="color: #15803d;">Cursos</div>
                </div>
                <div style="background: #f3e8ff; padding: 1rem; border-radius: 0.5rem; text-align: center;">
                    <div style="font-size: 1.5rem; font-weight: bold; color: #6b21a8;">${stats.materias}</div>
                    <div style="color: #6b21a8;">Materias</div>
                </div>
            `;
    } catch (error) {
      console.error("Error cargando estadísticas:", error);
    }
  },

  async loadLogs() {
    UI.showLoading("auditList");
    try {
      this.private_logs = await API.get("/api/admin/auditoria");
      this.renderLogsTable(this.private_logs);
    } catch (error) {
      UI.showError("auditList", error.message);
    }
  },

  filterLogs(searchTerm) {
    const searchInput = document.getElementById("globalSearch");
    const actionFilter = document.getElementById("filter-action");

    const actualSearchTerm = searchInput ? searchInput.value.toLowerCase() : "";
    const actualActionFilter = actionFilter ? actionFilter.value : "";

    let filtered = this.private_logs;

    if (actualSearchTerm) {
      filtered = filtered.filter(
        (log) =>
          log.username.toLowerCase().includes(actualSearchTerm) ||
          log.action.toLowerCase().includes(actualSearchTerm) ||
          log.details.toLowerCase().includes(actualSearchTerm),
      );
    }

    if (actualActionFilter) {
      filtered = filtered.filter((log) => log.action === actualActionFilter);
    }

    this.renderLogsTable(filtered);
  },

  renderLogsTable(logs) {
    const columns = [
      { label: "Fecha/Hora", key: "timestamp" },
      { label: "Usuario", key: "username" },
      { label: "Acción", key: "action" },
      { label: "Detalles", key: "details" },
    ];
    UI.renderTable(
      "auditList",
      columns,
      logs,
      (log) => `
            <span class="text-muted" style="font-size: 0.85rem;">Solo lectura</span>
        `,
    );
  },
};

// Make module accessible globally
window.auditoriaModule = Auditoria;
