export const UI = {
    showLoading(containerId) {
        const container = document.getElementById(containerId);
        container.innerHTML = `
            <div class="loading-spinner">
                <p>Cargando datos...</p>
            </div>
        `;
    },

    showError(containerId, message) {
        const container = document.getElementById(containerId);
        container.innerHTML = `
            <div class="alert alert-error" style="display: block">
                <strong>Error:</strong> ${message}
            </div>
        `;
    },

    renderTable(containerId, columns, data, actionsRenderer) {
        const container = document.getElementById(containerId);
        if (!data || data.length === 0) {
            container.innerHTML = '<p class="text-muted">No se encontraron registros.</p>';
            return;
        }

        let html = `
            <table class="admin-table">
                <thead>
                    <tr>
                        ${columns.map(c => `<th>${c.label}</th>`).join('')}
                        <th>Acciones</th>
                    </tr>
                </thead>
                <tbody>
                    ${data.map(row => `
                        <tr>
                            ${columns.map(c => `<td>${this.getNestedValue(row, c.key)}</td>`).join('')}
                            <td>${actionsRenderer(row)}</td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        `;
        container.innerHTML = html;
    },

    getNestedValue(obj, path) {
        if (!path) return '-';
        return path.split('.').reduce((prev, curr) => prev ? prev[curr] : '', obj) || '-';
    },

    showAlert(message, type = 'success') {
        const alertDiv = document.createElement('div');
        alertDiv.className = `ui-alert alert-${type}`;
        alertDiv.innerHTML = `
            <span class="alert-icon">${type === 'success' ? '✅' : '❌'}</span>
            <span class="alert-msg">${message}</span>
        `;
        
        let container = document.getElementById('alert-container');
        if (!container) {
            container = document.createElement('div');
            container.id = 'alert-container';
            container.style.cssText = 'position: fixed; top: 20px; right: 20px; z-index: 2000; width: 300px;';
            document.body.appendChild(container);
        }
        
        container.appendChild(alertDiv);
        setTimeout(() => {
            alertDiv.style.opacity = '0';
            alertDiv.style.transform = 'translateX(20px)';
            alertDiv.style.transition = 'all 0.3s ease';
            setTimeout(() => alertDiv.remove(), 300);
        }, 5000);
    },

    renderFilters(containerId, options = {}) {
        const container = document.getElementById(containerId);
        const { onSearch, filters = [] } = options;
        
        let html = `
            <div class="search-filter-bar">
                <input type="text" class="search-input" placeholder="Buscar..." id="globalSearch">
                ${filters.map(f => `
                    <select class="filter-select" id="filter-${f.id}">
                        <option value="">${f.label}</option>
                        ${f.options.map(opt => `<option value="${opt.value}">${opt.label}</option>`).join('')}
                    </select>
                `).join('')}
            </div>
        `;
        
        container.innerHTML = html;
        
        const searchInput = document.getElementById('globalSearch');
        searchInput.oninput = () => onSearch(searchInput.value);
        
        filters.forEach(f => {
            const select = document.getElementById(`filter-${f.id}`);
            select.onchange = () => onSearch(searchInput.value);
        });
    },

    showModal(title, content, onSave) {
        let modal = document.getElementById('adminModal');
        if (!modal) {
            modal = document.createElement('div');
            modal.id = 'adminModal';
            modal.className = 'modal-overlay';
            document.body.appendChild(modal);
        }

        modal.innerHTML = `
            <div class="modal-card">
                <div class="modal-header">
                    <h3>${title}</h3>
                    <button class="modal-close">&times;</button>
                </div>
                <div class="modal-body">${content}</div>
                <div class="modal-footer">
                    <button class="btn btn-secondary modal-cancel">Cancelar</button>
                    <button class="btn modal-save">Guardar</button>
                </div>
            </div>
        `;

        modal.style.display = 'flex';

        const close = () => modal.style.display = 'none';
        modal.querySelector('.modal-close').onclick = close;
        modal.querySelector('.modal-cancel').onclick = close;
        modal.querySelector('.modal-save').onclick = () => {
            onSave();
            // hideModal is usually called after onSave finishes successfully
        };
    },

    hideModal() {
        const modal = document.getElementById('adminModal');
        if (modal) modal.style.display = 'none';
    }
};
