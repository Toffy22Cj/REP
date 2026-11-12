import { API } from './api.js';
import { UI } from './ui.js';

const errorMsg = (e) => e.userMessage || e.message;

/**
 * Gestión académica: cursos, materias y asignaciones profesor-materia-curso.
 * Sin asignaciones un profesor no ve materias ni cursos en su panel.
 */
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
                        <h4>Asignaciones profesor · materia · curso</h4>
                        <button id="btnNewAsignacion" class="btn-small btn-edit">+ Nueva Asignación</button>
                    </div>
                    <p class="form-hint">Un profesor solo ve las materias y cursos que tiene asignados.</p>
                    <div id="asignacionesList"></div>
                </div>
            </div>
        `;

        document.getElementById('btnNewCurso').onclick = () => this.showCursoModal();
        document.getElementById('btnNewMateria').onclick = () => this.showMateriaModal();
        document.getElementById('btnNewAsignacion').onclick = () => this.showAsignacionModal();

        await Promise.all([this.loadCursos(), this.loadMaterias(), this.loadAsignaciones()]);
    },

    // ------------------------------------------------------------------ cursos

    showCursoModal() {
        UI.showModal('Nuevo Curso', `
            <form>
                <div class="form-group">
                    <label>Grado (1-11)</label>
                    <input type="number" id="c_grado" min="1" max="11" required>
                </div>
                <div class="form-group">
                    <label>Grupo (A, B, C...)</label>
                    <input type="text" id="c_grupo" maxlength="5" required>
                </div>
            </form>
        `, async () => {
            const grado = parseInt(document.getElementById('c_grado').value, 10);
            const grupo = document.getElementById('c_grupo').value.trim();
            if (Number.isNaN(grado) || grado < 1 || grado > 11 || !grupo) {
                UI.showAlert('Indique un grado entre 1 y 11 y un grupo', 'error');
                return;
            }
            try {
                await API.post('/api/admin/cursos', { grado, grupo });
                UI.showAlert('Curso creado exitosamente');
                UI.hideModal();
                await this.loadCursos();
            } catch (e) { UI.showAlert(errorMsg(e), 'error'); }
        });
    },

    async loadCursos() {
        UI.showLoading('cursosList');
        try {
            this.cursos = await API.get('/api/admin/cursos') || [];
            UI.renderTable('cursosList', [
                { label: 'Id', key: 'id' },
                { label: 'Curso', key: 'nombre' },
            ], this.cursos, (c) => `
                <button class="btn-small btn-edit" data-action="estudiantes" data-id="${c.id}">Estudiantes</button>
                <button class="btn-small btn-delete" data-action="eliminar" data-id="${c.id}">Eliminar</button>
            `);
            document.getElementById('cursosList').onclick = (ev) => {
                const b = ev.target.closest('button[data-action]');
                if (!b) return;
                const curso = this.cursos.find(c => String(c.id) === b.dataset.id);
                if (b.dataset.action === 'estudiantes') this.showEstudiantes(curso);
                if (b.dataset.action === 'eliminar') this.eliminar(`/api/admin/cursos/${curso.id}`,
                    `¿Eliminar el curso ${curso.nombre}?`, () => this.loadCursos());
            };
        } catch (error) {
            UI.showError('cursosList', errorMsg(error));
        }
    },

    async showEstudiantes(curso) {
        try {
            const estudiantes = await API.get(`/api/admin/cursos/${curso.id}/estudiantes`) || [];
            const lista = estudiantes.length
                ? `<ul>${estudiantes.map(e => `<li>${UI.escapeHtml(e.nombre)} ${UI.escapeHtml(e.apellido)} — ${UI.escapeHtml(e.identificacion)}</li>`).join('')}</ul>`
                : '<p class="text-muted">Este curso no tiene estudiantes.</p>';
            UI.showModal(`Estudiantes de ${UI.escapeHtml(curso.nombre)}`, lista, () => UI.hideModal());
        } catch (e) { UI.showAlert(errorMsg(e), 'error'); }
    },

    // ------------------------------------------------------------------ materias

    showMateriaModal() {
        UI.showModal('Nueva Materia', `
            <form>
                <div class="form-group">
                    <label>Nombre de la Materia</label>
                    <input type="text" id="m_nombre_mat" minlength="3" maxlength="100" required>
                </div>
            </form>
        `, async () => {
            const nombre = document.getElementById('m_nombre_mat').value.trim();
            if (nombre.length < 3) {
                UI.showAlert('El nombre debe tener al menos 3 caracteres', 'error');
                return;
            }
            try {
                await API.post('/api/admin/materias', { nombre });
                UI.showAlert('Materia creada exitosamente');
                UI.hideModal();
                await this.loadMaterias();
            } catch (e) { UI.showAlert(errorMsg(e), 'error'); }
        });
    },

    async loadMaterias() {
        UI.showLoading('materiasList');
        try {
            this.materias = await API.get('/api/admin/materias') || [];
            UI.renderTable('materiasList', [
                { label: 'Id', key: 'id' },
                { label: 'Nombre', key: 'nombre' },
            ], this.materias, (m) => `
                <button class="btn-small btn-delete" data-id="${m.id}">Eliminar</button>
            `);
            document.getElementById('materiasList').onclick = (ev) => {
                const b = ev.target.closest('button[data-id]');
                if (!b) return;
                const materia = this.materias.find(m => String(m.id) === b.dataset.id);
                this.eliminar(`/api/admin/materias/${materia.id}`, `¿Eliminar la materia ${materia.nombre}?`,
                    () => this.loadMaterias());
            };
        } catch (error) {
            UI.showError('materiasList', errorMsg(error));
        }
    },

    // ------------------------------------------------------------------ asignaciones

    async loadAsignaciones() {
        UI.showLoading('asignacionesList');
        try {
            const asignaciones = await API.get('/api/admin/asignaciones') || [];
            this.asignaciones = asignaciones.map(a => ({
                id: a.id,
                profesor: a.profesor ? `${a.profesor.nombre} ${a.profesor.apellido}` : '-',
                materia: a.materia ? a.materia.nombre : '-',
                curso: a.curso ? a.curso.nombre : '-',
            }));
            UI.renderTable('asignacionesList', [
                { label: 'Profesor', key: 'profesor' },
                { label: 'Materia', key: 'materia' },
                { label: 'Curso', key: 'curso' },
            ], this.asignaciones, (a) => `
                <button class="btn-small btn-delete" data-id="${a.id}">Quitar</button>
            `);
            document.getElementById('asignacionesList').onclick = (ev) => {
                const b = ev.target.closest('button[data-id]');
                if (!b) return;
                const a = this.asignaciones.find(x => String(x.id) === b.dataset.id);
                this.eliminar(`/api/admin/asignaciones/${a.id}`,
                    `¿Quitar ${a.materia} (${a.curso}) a ${a.profesor}?`, () => this.loadAsignaciones());
            };
        } catch (error) {
            UI.showError('asignacionesList', errorMsg(error));
        }
    },

    async showAsignacionModal() {
        let profesores = [];
        try {
            profesores = await API.get('/api/admin/usuarios?rol=PROFESOR') || [];
        } catch (e) {
            UI.showAlert(errorMsg(e), 'error');
            return;
        }
        const cursos = this.cursos || [];
        const materias = this.materias || [];
        if (!profesores.length || !cursos.length || !materias.length) {
            UI.showAlert('Necesita al menos un profesor, una materia y un curso para crear una asignación', 'error');
            return;
        }
        const opciones = (lista, texto) => lista
            .map(x => `<option value="${x.id}">${UI.escapeHtml(texto(x))}</option>`).join('');

        UI.showModal('Nueva Asignación', `
            <form>
                <div class="form-group">
                    <label>Profesor</label>
                    <select id="a_profesor" class="form-control">${opciones(profesores, p => `${p.nombre} ${p.apellido} (${p.identificacion})`)}</select>
                </div>
                <div class="form-group">
                    <label>Materia</label>
                    <select id="a_materia" class="form-control">${opciones(materias, m => m.nombre)}</select>
                </div>
                <div class="form-group">
                    <label>Curso</label>
                    <select id="a_curso" class="form-control">${opciones(cursos, c => c.nombre)}</select>
                </div>
            </form>
        `, async () => {
            const data = {
                profesorId: Number(document.getElementById('a_profesor').value),
                materiaId: Number(document.getElementById('a_materia').value),
                cursoId: Number(document.getElementById('a_curso').value),
            };
            try {
                await API.post('/api/admin/asignaciones', data);
                UI.showAlert('Asignación creada');
                UI.hideModal();
                await this.loadAsignaciones();
            } catch (e) { UI.showAlert(errorMsg(e), 'error'); }
        });
    },

    // ------------------------------------------------------------------ comunes

    async eliminar(url, pregunta, recargar) {
        if (!confirm(pregunta)) return;
        try {
            await API.delete(url);
            UI.showAlert('Eliminado correctamente');
            await recargar();
        } catch (e) {
            // p. ej. CURSO_CON_ESTUDIANTES o MATERIA_CON_ASIGNACIONES: el mensaje explica el motivo
            UI.showAlert(errorMsg(e), 'error');
        }
    },
};
