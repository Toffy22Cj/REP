/**
 * Error de la API con el contrato del backend (ApiError):
 * { status, code, message, details, path, traceId }.
 * Ver docs/errores.md para el catálogo de códigos.
 */
export class ApiError extends Error {
    constructor({ status, code, message, details, traceId }) {
        super(message || `Error ${status}`);
        this.name = 'ApiError';
        this.status = status;
        this.code = code || 'DESCONOCIDO';
        this.details = details || {};
        this.traceId = traceId;
    }

    /** Mensaje listo para mostrar al usuario. */
    get userMessage() {
        return this.traceId ? `${this.message} (traceId: ${this.traceId})` : this.message;
    }
}

const MENSAJES_POR_ESTADO = {
    0: 'No se pudo conectar con el servidor',
    400: 'Los datos enviados no son válidos',
    403: 'No tiene permisos para realizar esta acción',
    404: 'El recurso solicitado no existe',
    409: 'La operación entra en conflicto con datos existentes',
    429: 'Demasiadas solicitudes. Intente más tarde',
    500: 'Error interno del servidor',
};

async function toApiError(response) {
    const text = await response.text().catch(() => '');
    let body = {};
    try {
        body = text ? JSON.parse(text) : {};
    } catch {
        // Respuesta en texto plano (servidores antiguos o proxies)
        body = { message: text.length <= 300 ? text : '' };
    }
    return new ApiError({
        status: response.status,
        code: body.code,
        message: body.message || body.mensaje || MENSAJES_POR_ESTADO[response.status] || `Error ${response.status}`,
        details: body.details,
        traceId: body.traceId,
    });
}

export const API = {
    get(url) { return this.request(url, 'GET'); },
    post(url, data) { return this.request(url, 'POST', data); },
    put(url, data) { return this.request(url, 'PUT', data); },
    delete(url) { return this.request(url, 'DELETE'); },

    async request(url, method, data = null) {
        const options = {
            method,
            credentials: 'same-origin',
            headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
        };
        if (data !== null && data !== undefined) {
            options.body = JSON.stringify(data);
        }

        let response;
        try {
            response = await fetch(url, options);
        } catch (networkError) {
            throw new ApiError({ status: 0, code: 'SIN_CONEXION', message: MENSAJES_POR_ESTADO[0] });
        }

        // Solo la sesión caducada lleva al login; un 403 es un error de negocio que se muestra
        if (response.status === 401) {
            if (!window.location.pathname.endsWith('login.html')) {
                window.location.href = '/admin/login.html';
            }
            throw await toApiError(response);
        }

        if (!response.ok) {
            const error = await toApiError(response);
            console.warn(`API ${method} ${url} -> ${error.status} ${error.code}: ${error.message}`, error.details);
            throw error;
        }

        if (response.status === 204) return null;
        const text = await response.text();
        return text ? JSON.parse(text) : null;
    },
};
