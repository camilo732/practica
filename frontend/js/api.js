/*
 * Cliente de la API de LibroLog.
 *
 * El token de acceso vive en una variable de este módulo (memoria de la
 * pestaña). El refresh token viaja en una cookie HttpOnly que JavaScript no
 * puede leer; al cargar cada página se renueva la sesión con esa cookie.
 */
import { CONFIG } from './config.js';
import { getLanguage, t } from './i18n.js';

const AUTH_BASE = '/api/auth';
const ACCESS_TOKEN_MARGIN_MS = 30_000;

let accessToken = null;
let tokenExpiresAt = 0;
let currentUser = null;
let refreshPromise = null;

/**
 * Error normalizado de la API.
 */
export class ApiError extends Error {
    /**
     * Crea el error.
     *
     * @param {number} status código HTTP, 0 si no hubo respuesta
     * @param {{title?: string, detail?: string, errors?: Record<string, string>}} problem problema
     * @param {string|null} retryAfter segundos de Retry-After
     * @param {unknown} [cause] causa original
     */
    constructor(status, problem = {}, retryAfter = null, cause) {
        super(problem.detail || problem.title || `HTTP ${status}`);
        this.name = 'ApiError';
        this.status = status;
        this.problem = problem;
        this.fieldErrors = problem.errors || {};
        this.retryAfter = retryAfter ? Number(retryAfter) : null;
        this.cause = cause;
    }
}

/**
 * Guarda la sesión devuelta por autenticación.
 *
 * @param {{accessToken: string, expiresInSeconds?: number, user?: object}} payload respuesta
 */
function storeSession(payload) {
    accessToken = payload.accessToken || null;
    tokenExpiresAt = Date.now() + Math.max(0, Number(payload.expiresInSeconds) || 0) * 1000;
    currentUser = payload.user || null;
}

/**
 * Borra la sesión en memoria.
 */
export function clearSession() {
    accessToken = null;
    tokenExpiresAt = 0;
    currentUser = null;
}

/**
 * Devuelve el usuario actual en memoria.
 *
 * @returns {object|null} usuario o null
 */
export function getCurrentUser() {
    return currentUser;
}

/**
 * Indica si hay token en memoria.
 *
 * @returns {boolean} verdadero si existe token
 */
export function hasAccessToken() {
    return Boolean(accessToken);
}

/**
 * Renueva el token de acceso con la cookie HttpOnly.
 *
 * Varias llamadas simultáneas comparten la misma promesa para no rotar la
 * cookie más de una vez.
 *
 * @returns {Promise<boolean>} verdadero si la sesión sigue siendo válida
 */
export function refreshSession() {
    if (refreshPromise) {
        return refreshPromise;
    }

    refreshPromise = (async () => {
        try {
            const response = await fetch(`${CONFIG.apiBase}${AUTH_BASE}/refresh`, {
                method: 'POST',
                credentials: 'include',
                cache: 'no-store',
                headers: {
                    'Accept': 'application/json',
                    'Accept-Language': getLanguage(),
                    [CONFIG.clientHeader]: CONFIG.clientValue
                }
            });

            if (!response.ok) {
                clearSession();
                return false;
            }

            const payload = await response.json();
            storeSession(payload);
            return Boolean(accessToken);
        } catch (error) {
            // Si ya había un token, un fallo de red no demuestra que la sesión
            // haya caducado: se conserva para reintentar la petición.
            return Boolean(accessToken);
        }
    })().finally(() => {
        refreshPromise = null;
    });

    return refreshPromise;
}

/**
 * Garantiza que hay una sesión en memoria.
 *
 * @returns {Promise<object|null>} usuario o null
 */
export async function ensureSession() {
    if (accessToken && currentUser) {
        return currentUser;
    }

    const renewed = await refreshSession();
    return renewed ? currentUser : null;
}

/**
 * Inicia sesión con usuario o correo y contraseña.
 *
 * @param {{usernameOrEmail: string, password: string}} credentials credenciales
 * @returns {Promise<object>} usuario autenticado
 */
export async function login(credentials) {
    const response = await request(`${AUTH_BASE}/login`, {
        method: 'POST',
        body: credentials,
        auth: false
    });

    if (!response.ok) {
        throw await toApiError(response);
    }

    const payload = await response.json();
    storeSession(payload);
    return currentUser;
}

/**
 * Registra una cuenta y abre la sesión.
 *
 * @param {object} data datos de registro
 * @returns {Promise<object>} usuario autenticado
 */
export async function register(data) {
    const response = await request(`${AUTH_BASE}/register`, {
        method: 'POST',
        body: data,
        auth: false
    });

    if (!response.ok) {
        throw await toApiError(response);
    }

    const payload = await response.json();
    storeSession(payload);
    return currentUser;
}

/**
 * Consulta los proveedores de autenticación disponibles.
 *
 * @returns {Promise<{local: boolean, google: boolean}>} proveedores
 */
export async function authProviders() {
    const response = await request(`${AUTH_BASE}/providers`, { auth: false });
    if (!response.ok) {
        throw await toApiError(response);
    }
    return response.json();
}

/**
 * Cierra la sesión y borra la cookie de refresco.
 *
 * @returns {Promise<void>} promesa resuelta siempre
 */
export async function logout() {
    try {
        await fetch(`${CONFIG.apiBase}${AUTH_BASE}/logout`, {
            method: 'POST',
            credentials: 'include',
            cache: 'no-store',
            headers: {
                'Accept': 'application/json',
                'Accept-Language': getLanguage(),
                [CONFIG.clientHeader]: CONFIG.clientValue,
                ...(accessToken ? { 'Authorization': `Bearer ${accessToken}` } : {})
            }
        });
    } catch (error) {
        // El cierre local debe ocurrir aunque falle la red.
    } finally {
        clearSession();
    }
}

/**
 * Recarga los datos públicos del usuario desde el servidor.
 *
 * @returns {Promise<object|null>} usuario o null
 */
export async function refreshCurrentUser() {
    try {
        const user = await api(`${AUTH_BASE}/me`);
        currentUser = user;
        return user;
    } catch (error) {
        if (error instanceof ApiError && error.status === 401) {
            clearSession();
        }
        return null;
    }
}

/**
 * Ejecuta una petición autenticada y devuelve el cuerpo interpretado.
 *
 * @param {string} path ruta relativa a la API
 * @param {{method?: string, body?: unknown, headers?: Record<string, string>}} [options] opciones
 * @returns {Promise<any>} cuerpo JSON, texto o null
 */
export async function api(path, options = {}) {
    const response = await request(path, { ...options, auth: true });

    if (!response.ok) {
        throw await toApiError(response);
    }
    if (response.status === 204) {
        return null;
    }

    const contentType = response.headers.get('content-type') || '';
    if (contentType.includes('json')) {
        return response.json();
    }
    return response.text();
}

/**
 * Sube un formulario multipart autenticado.
 *
 * @param {string} path ruta
 * @param {FormData} formData datos
 * @returns {Promise<any>} respuesta JSON
 */
export async function upload(path, formData) {
    return api(path, { method: 'POST', body: formData });
}

/**
 * Descarga un archivo autenticado y fuerza la descarga en el navegador.
 *
 * @param {string} path ruta
 * @param {string} fallbackName nombre alternativo si falta Content-Disposition
 * @returns {Promise<string>} nombre usado
 */
export async function download(path, fallbackName) {
    const response = await request(path, {
        method: 'GET',
        auth: true,
        headers: { 'Accept': '*/*' }
    });

    if (!response.ok) {
        throw await toApiError(response);
    }

    const blob = await response.blob();
    const filename = filenameFromHeader(response.headers.get('content-disposition'))
        || fallbackName;

    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    link.rel = 'noopener';
    document.body.appendChild(link);
    link.click();
    link.remove();

    // Dar tiempo al navegador a iniciar la descarga antes de liberar el blob.
    setTimeout(() => URL.revokeObjectURL(url), 2000);
    return filename;
}

/**
 * Realiza la petición HTTP con token, idioma y reintento tras renovar sesión.
 *
 * @param {string} path ruta
 * @param {{method?: string, body?: unknown, headers?: Record<string, string>, auth?: boolean, retry?: boolean}} options opciones
 * @returns {Promise<Response>} respuesta HTTP
 */
async function request(path, options = {}) {
    const {
        method = 'GET',
        body,
        headers = {},
        auth = true,
        retry = true
    } = options;

    if (auth && accessToken && tokenExpiresAt > 0 && Date.now() >= tokenExpiresAt - ACCESS_TOKEN_MARGIN_MS) {
        await refreshSession();
    }

    const finalHeaders = new Headers(headers);
    if (!finalHeaders.has('Accept')) {
        finalHeaders.set('Accept', 'application/json');
    }
    finalHeaders.set('Accept-Language', getLanguage());

    let payload;
    if (body instanceof FormData) {
        payload = body;
    } else if (body !== undefined) {
        finalHeaders.set('Content-Type', 'application/json');
        payload = JSON.stringify(body);
    }

    if (auth && accessToken) {
        finalHeaders.set('Authorization', `Bearer ${accessToken}`);
    }

    let response;
    try {
        response = await fetch(`${CONFIG.apiBase}${path}`, {
            method,
            headers: finalHeaders,
            body: payload,
            credentials: 'include',
            cache: 'no-store'
        });
    } catch (error) {
        throw new ApiError(0, {
            title: t('errors.network'),
            detail: t('errors.networkDetail')
        }, null, error);
    }

    if (response.status === 401 && auth && retry && accessToken) {
        const renewed = await refreshSession();
        if (renewed) {
            return request(path, { ...options, retry: false });
        }
        clearSession();
        document.dispatchEvent(new CustomEvent('librolog:session-expired'));
    }

    return response;
}

/**
 * Convierte una respuesta de error en ApiError.
 *
 * @param {Response} response respuesta
 * @returns {Promise<ApiError>} error normalizado
 */
async function toApiError(response) {
    let problem = {};
    const contentType = response.headers.get('content-type') || '';

    try {
        if (contentType.includes('json')) {
            problem = await response.json();
        } else {
            const text = await response.text();
            if (text) {
                problem = { detail: text };
            }
        }
    } catch (error) {
        problem = {};
    }

    return new ApiError(
        response.status,
        problem,
        response.headers.get('retry-after')
    );
}

/**
 * Extrae el nombre de archivo de Content-Disposition.
 *
 * @param {string|null} header cabecera
 * @returns {string|null} nombre o null
 */
function filenameFromHeader(header) {
    if (!header) {
        return null;
    }

    const encoded = /filename\*=UTF-8''([^;]+)/i.exec(header);
    if (encoded) {
        try {
            return decodeURIComponent(encoded[1]);
        } catch (error) {
            return encoded[1];
        }
    }

    const plain = /filename="?([^";]+)"?/i.exec(header);
    return plain ? plain[1] : null;
}
