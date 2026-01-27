export const API = {
    async get(url) {
        return this.request(url, 'GET');
    },

    async post(url, data) {
        return this.request(url, 'POST', data);
    },

    async put(url, data) {
        return this.request(url, 'PUT', data);
    },

    async delete(url) {
        return this.request(url, 'DELETE');
    },

    async request(url, method, data = null) {
        const options = {
            method,
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            }
        };

        if (data) {
            options.body = JSON.stringify(data);
        }

        try {
            const response = await fetch(url, options);
            
            if (response.status === 401 || response.status === 403) {
                if (!window.location.pathname.endsWith('login.html')) {
                    window.location.href = '/admin/login.html';
                }
                return null;
            }

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                throw new Error(errorData.message || `Error ${response.status}: ${response.statusText}`);
            }

            if (response.status === 204) return null;
            return await response.json();
        } catch (error) {
            console.error(`API Error (${method} ${url}):`, error);
            throw error;
        }
    }
};
