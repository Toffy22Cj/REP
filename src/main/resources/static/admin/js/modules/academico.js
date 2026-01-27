import { API } from './api.js';
import { UI } from './ui.js';

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
                </div>
            </div>
        `;

        document.getElementById('btnNewCurso').onclick = () => this.showCursoModal();
        document.getElementById('btnNewMateria').onclick = () => this.showMateriaModal();
        
        await this.loadCursos();
        await this.loadMaterias();
    },

    showCursoModal() {
        const content = `
            <form>
                <div class="form-group">
                    <label>Grado (1-11)</label>
                    <input type="number" id="c_grado" min="1" max="11" required>
                </div>
                <div class="form-group">
                    <label>Grupo (A, B, C...)</label>
                    <input type="text" id="c_grupo" required>
                </div>
            </form>
        `;
        UI.showModal('Nuevo Curso', content, async () => {
            const data = {
                grado: parseInt(document.getElementById('c_grado').value),
                grupo: document.getElementById('c_grupo').value
            };
            try {
                await API.post('/api/admin/cursos', data);
                UI.showAlert('Curso creado exitosamente');
                UI.hideModal();
                await this.loadCursos();
            } catch (e) { UI.showAlert(e.message, 'error'); }
        });
    },

    showMateriaModal() {
        const content = `
            <form>
                <div class="form-group">
                    <label>Nombre de la Materia</label>
                    <input type="text" id="m_nombre_mat" required>
                </div>
            </form>
        `;
        UI.showModal('Nueva Materia', content, async () => {
            const data = { nombre: document.getElementById('m_nombre_mat').value };
            try {
                await API.post('/api/admin/materias', data);
                UI.showAlert('Materia creada exitosamente');
                UI.hideModal();
                await this.loadMaterias();
            } catch (e) { UI.showAlert(e.message, 'error'); }
        });
    },

    async loadCursos() {
        UI.showLoading('cursosList');
        try {
            const cursos = await API.get('/api/admin/cursos');
            const columns = [
                { label: 'Id', key: 'id' },
                { label: 'Grado', key: 'grado' },
                { label: 'Grupo', key: 'grupo' }
            ];
            UI.renderTable('cursosList', columns, cursos, (curso) => `
                <button class="btn-small btn-edit" onclick="alert('Ver estudiantes en v2.1')">Estudiantes</button>
            `);
        } catch (error) {
            UI.showError('cursosList', error.message);
        }
    },

    async loadMaterias() {
        UI.showLoading('materiasList');
        try {
            const materias = await API.get('/api/admin/materias');
            const columns = [
                { label: 'Id', key: 'id' },
                { label: 'Nombre', key: 'nombre' }
            ];
            UI.renderTable('materiasList', columns, materias, (materia) => `
                <button class="btn-small btn-delete" onclick="alert('Eliminar en v2.1')">Eliminar</button>
            `);
        } catch (error) {
            UI.showError('materiasList', error.message);
        }
    }
};
