import { API } from './api.js';
import { UI } from './ui.js';

export const Auditoria = {
    async init(containerId) {
        const container = document.getElementById(containerId);
        container.innerHTML = `
            <div class="card">
                <h3>Auditoría de Sistema</h3>
                <p class="text-muted">Registro histórico de acciones administrativas.</p>
                <div id="auditFilters" style="margin-top: 1rem;"></div>
                <div id="auditList"></div>
            </div>
        `;

        UI.renderFilters('auditFilters', {
            onSearch: (val) => this.filterLogs(val)
        });
        
        await this.loadLogs();
    },

    private_logs: [],

    async loadLogs() {
        UI.showLoading('auditList');
        try {
            this.private_logs = await API.get('/api/admin/auditoria');
            this.renderLogsTable(this.private_logs);
        } catch (error) {
            UI.showError('auditList', error.message);
        }
    },

    filterLogs(searchTerm) {
        searchTerm = searchTerm.toLowerCase();
        const filtered = this.private_logs.filter(log => 
            log.username.toLowerCase().includes(searchTerm) ||
            log.action.toLowerCase().includes(searchTerm) ||
            log.details.toLowerCase().includes(searchTerm)
        );
        this.renderLogsTable(filtered);
    },

    renderLogsTable(logs) {
        const columns = [
            { label: 'Tiempo', key: 'timestamp' },
            { label: 'Usuario', key: 'username' },
            { label: 'Acción', key: 'action' },
            { label: 'Detalles', key: 'details' }
        ];
        UI.renderTable('auditList', columns, logs, (log) => `
            <span class="text-muted">Solo lectura</span>
        `);
    }
};
