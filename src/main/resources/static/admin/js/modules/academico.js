import { API } from "./api.js";
import { UI } from "./ui.js";

export const Academico = {
  async init(containerId) {
    const container = document.getElementById(containerId);
    container.innerHTML = `
            <div class="card">
                <h3>Gestión Académica</h3>
                <div style="margin-top: 1rem;">
                    <div style="display: flex; justify-content: space-between; align-items: center;">
                        <h4>Cursos</h4>
                        <button id="btnNewCurso" class="btn-small btn-edit">+ Nuevo Curso</button>
                    </div>
                    <div id="cursosList"></div>
                    
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 2rem;">
                        <h4>Materias</h4>
                        <button id="btnNewMateria" class="btn-small btn-edit">+ Nueva Materia</button>
                    </div>
                    <div id="materiasList"></div>

                    <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 2rem;">
                        <h4>Asignaciones Profesor-Materia</h4>
                        <button id="btnNewAsignacion" class="btn-small btn-edit">+ Nueva Asignación</button>
                    </div>
                    <div id="asignacionesList"></div>
                </div>
            </div>
        `;

    document.getElementById("btnNewCurso").onclick = () =>
      this.showCursoModal();
    document.getElementById("btnNewMateria").onclick = () =>
      this.showMateriaModal();
    document.getElementById("btnNewAsignacion").onclick = () =>
      this.showAsignacionModal();

    await this.loadCursos();
    await this.loadMaterias();
    await this.loadAsignaciones();
  },

  async showCursoModal(cursoId = null) {
    let cursoData = null;
    if (cursoId) {
      try {
        cursoData = await API.get(`/api/admin/cursos/${cursoId}`);
      } catch (e) {
        UI.showAlert("Error al cargar curso: " + e.message, "error");
        return;
      }
    }

    const content = `
            <form>
                <div class="form-group">
                    <label>Grado (1-11)</label>
                    <input type="number" id="c_grado" min="1" max="11" value="${cursoData?.grado || ""}" required>
                </div>
                <div class="form-group">
                    <label>Grupo (A, B, C...)</label>
                    <input type="text" id="c_grupo" value="${cursoData?.grupo || ""}" required>
                </div>
            </form>
        `;
    UI.showModal(
      cursoId ? "Editar Curso" : "Nuevo Curso",
      content,
      async () => {
        const data = {
          grado: parseInt(document.getElementById("c_grado").value),
          grupo: document.getElementById("c_grupo").value,
        };
        try {
          if (cursoId) {
            await API.put(`/api/admin/cursos/${cursoId}`, data);
            UI.showAlert("Curso actualizado exitosamente");
          } else {
            await API.post("/api/admin/cursos", data);
            UI.showAlert("Curso creado exitosamente");
          }
          UI.hideModal();
          await this.loadCursos();
        } catch (e) {
          UI.showAlert(e.message, "error");
        }
      },
    );
  },

  async showMateriaModal(materiaId = null) {
    let materiaData = null;
    if (materiaId) {
      try {
        materiaData = await API.get(`/api/admin/materias/${materiaId}`);
      } catch (e) {
        UI.showAlert("Error al cargar materia: " + e.message, "error");
        return;
      }
    }

    const content = `
            <form>
                <div class="form-group">
                    <label>Nombre de la Materia</label>
                    <input type="text" id="m_nombre_mat" value="${materiaData?.nombre || ""}" required>
                </div>
            </form>
        `;
    UI.showModal(
      materiaId ? "Editar Materia" : "Nueva Materia",
      content,
      async () => {
        const data = { nombre: document.getElementById("m_nombre_mat").value };
        try {
          if (materiaId) {
            await API.put(`/api/admin/materias/${materiaId}`, data);
            UI.showAlert("Materia actualizada exitosamente");
          } else {
            await API.post("/api/admin/materias", data);
            UI.showAlert("Materia creada exitosamente");
          }
          UI.hideModal();
          await this.loadMaterias();
        } catch (e) {
          UI.showAlert(e.message, "error");
        }
      },
    );
  },

  async showAsignacionModal(asignacionId = null) {
    try {
      const [profesores, cursos, materias] = await Promise.all([
        API.get("/api/admin/usuarios?rol=PROFESOR"),
        API.get("/api/admin/cursos"),
        API.get("/api/admin/materias"),
      ]);

      let asignacionData = null;
      if (asignacionId) {
        asignacionData = await API.get(
          `/api/admin/asignaciones/${asignacionId}`,
        );
      }

      const content = `
                <form>
                    <div class="form-group">
                        <label>Profesor</label>
                        <select id="a_profesor" required>
                            <option value="">Seleccionar profesor...</option>
                            ${profesores
                              .map(
                                (p) => `
                                <option value="${p.id}" ${asignacionData?.profesor?.id === p.id ? "selected" : ""}>
                                    ${p.nombre} ${p.apellido}
                                </option>
                            `,
                              )
                              .join("")}
                        </select>
                    </div>
                    <div class="form-group">
                        <label>Curso</label>
                        <select id="a_curso" required>
                            <option value="">Seleccionar curso...</option>
                            ${cursos
                              .map(
                                (c) => `
                                <option value="${c.id}" ${asignacionData?.curso?.id === c.id ? "selected" : ""}>
                                    Grado ${c.grado} - Grupo ${c.grupo}
                                </option>
                            `,
                              )
                              .join("")}
                        </select>
                    </div>
                    <div class="form-group">
                        <label>Materia</label>
                        <select id="a_materia" required>
                            <option value="">Seleccionar materia...</option>
                            ${materias
                              .map(
                                (m) => `
                                <option value="${m.id}" ${asignacionData?.materia?.id === m.id ? "selected" : ""}>
                                    ${m.nombre}
                                </option>
                            `,
                              )
                              .join("")}
                        </select>
                    </div>
                </form>
            `;
      UI.showModal(
        asignacionId ? "Editar Asignación" : "Nueva Asignación",
        content,
        async () => {
          const data = {
            profesor: { id: document.getElementById("a_profesor").value },
            curso: { id: document.getElementById("a_curso").value },
            materia: { id: document.getElementById("a_materia").value },
          };
          try {
            await API.post("/api/admin/asignaciones", data);
            UI.showAlert("Asignación creada exitosamente");
            UI.hideModal();
            await this.loadAsignaciones();
          } catch (e) {
            UI.showAlert(e.message, "error");
          }
        },
      );
    } catch (e) {
      UI.showAlert("Error al cargar datos: " + e.message, "error");
    }
  },

  async loadCursos() {
    UI.showLoading("cursosList");
    try {
      const cursos = await API.get("/api/admin/cursos");
      const columns = [
        { label: "Id", key: "id" },
        { label: "Grado", key: "grado" },
        { label: "Grupo", key: "grupo" },
      ];
      UI.renderTable(
        "cursosList",
        columns,
        cursos,
        (curso) => `
                <button class="btn-small btn-edit" onclick="window.academicoModule.loadEstudiantesDeCurso(${curso.id})">Estudiantes</button>
                <button class="btn-small btn-edit" onclick="window.academicoModule.showCursoModal(${curso.id})">Editar</button>
                <button class="btn-small btn-delete" onclick="window.academicoModule.deleteCurso(${curso.id})">Eliminar</button>
            `,
      );
    } catch (error) {
      UI.showError("cursosList", error.message);
    }
  },

  async loadEstudiantesDeCurso(cursoId) {
    try {
      UI.showLoading("mainContent");
      const estudiantes = await API.get(
        `/api/admin/cursos/${cursoId}/estudiantes`,
      );
      const columns = [
        { label: "Id", key: "id" },
        { label: "Nombre", key: "nombre" },
        { label: "Apellido", key: "apellido" },
        { label: "Identificación", key: "identificacion" },
        { label: "Correo", key: "correo" },
      ];
      const html = `
                <div class="card">
                    <h3>Estudiantes del Curso</h3>
                    <div id="tempEstudiantesList"></div>
                    <button class="btn" onclick="window.location.hash='#academico'" style="margin-top: 1rem;">Volver</button>
                </div>
            `;
      document.getElementById("mainContent").innerHTML = html;
      UI.renderTable(
        "tempEstudiantesList",
        columns,
        estudiantes,
        (e) => '<span class="text-muted">Solo lectura</span>',
      );
    } catch (error) {
      UI.showAlert("Error: " + error.message, "error");
    }
  },

  async deleteCurso(cursoId) {
    if (!confirm("¿Estás seguro de que deseas eliminar este curso?")) {
      return;
    }
    try {
      await API.delete(`/api/admin/cursos/${cursoId}`);
      UI.showAlert("Curso eliminado exitosamente");
      await this.loadCursos();
    } catch (error) {
      UI.showAlert("Error: " + error.message, "error");
    }
  },

  async loadMaterias() {
    UI.showLoading("materiasList");
    try {
      const materias = await API.get("/api/admin/materias");
      const columns = [
        { label: "Id", key: "id" },
        { label: "Nombre", key: "nombre" },
      ];
      UI.renderTable(
        "materiasList",
        columns,
        materias,
        (materia) => `
                <button class="btn-small btn-edit" onclick="window.academicoModule.showMateriaModal(${materia.id})">Editar</button>
                <button class="btn-small btn-delete" onclick="window.academicoModule.deleteMateria(${materia.id})">Eliminar</button>
            `,
      );
    } catch (error) {
      UI.showError("materiasList", error.message);
    }
  },

  async deleteMateria(materiaId) {
    if (!confirm("¿Estás seguro de que deseas eliminar esta materia?")) {
      return;
    }
    try {
      await API.delete(`/api/admin/materias/${materiaId}`);
      UI.showAlert("Materia eliminada exitosamente");
      await this.loadMaterias();
    } catch (error) {
      UI.showAlert("Error: " + error.message, "error");
    }
  },

  async loadAsignaciones() {
    UI.showLoading("asignacionesList");
    try {
      const asignaciones = await API.get("/api/admin/asignaciones");
      const columns = [
        { label: "Id", key: "id" },
        { label: "Profesor", key: "profesor.nombre" },
        { label: "Curso", key: "curso.nombre" },
        { label: "Materia", key: "materia.nombre" },
      ];
      UI.renderTable(
        "asignacionesList",
        columns,
        asignaciones,
        (a) => `
                <button class="btn-small btn-delete" onclick="window.academicoModule.deleteAsignacion(${a.id})">Eliminar</button>
            `,
      );
    } catch (error) {
      UI.showError("asignacionesList", error.message);
    }
  },

  async deleteAsignacion(asignacionId) {
    if (!confirm("¿Estás seguro de que deseas eliminar esta asignación?")) {
      return;
    }
    try {
      await API.delete(`/api/admin/asignaciones/${asignacionId}`);
      UI.showAlert("Asignación eliminada exitosamente");
      await this.loadAsignaciones();
    } catch (error) {
      UI.showAlert("Error: " + error.message, "error");
    }
  },
};

// Make module accessible globally for onclick handlers
window.academicoModule = Academico;
