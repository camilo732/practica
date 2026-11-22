/*
 * Utilidades de interfaz compartidas: creación de nodos, avisos, diálogos,
 * estados vacíos, paginación, portadas y descripción de errores.
 */
import { ApiError } from './api.js';
import { formatNumber, getLanguage, getLocale, t } from './i18n.js';

/**
 * Atajo de querySelector.
 *
 * @param {string} selector selector CSS
 * @param {ParentNode} [root] contenedor
 * @returns {Element|null} primer elemento
 */
export function qs(selector, root = document) {
    return root.querySelector(selector);
}

/**
 * Atajo de querySelectorAll como array.
 *
 * @param {string} selector selector CSS
 * @param {ParentNode} [root] contenedor
 * @returns {Element[]} elementos
 */
export function qsa(selector, root = document) {
    return Array.from(root.querySelectorAll(selector));
}

/**
 * Crea un elemento con atributos e hijos.
 *
 * @param {string} tag etiqueta
 * @param {Record<string, any>} [attrs] atributos; text, class, dataset, style y on*
 * @param {...(Node|string|Array)} children hijos
 * @returns {HTMLElement} elemento
 */
export function el(tag, attrs = {}, ...children) {
    const node = document.createElement(tag);

    Object.entries(attrs || {}).forEach(([key, value]) => {
        if (value === undefined || value === null || value === false) {
            return;
        }
        if (key === 'class') {
            node.className = value;
        } else if (key === 'text') {
            node.textContent = value;
        } else if (key === 'dataset') {
            Object.assign(node.dataset, value);
        } else if (key === 'style' && typeof value === 'object') {
            Object.assign(node.style, value);
        } else if (key.startsWith('on') && typeof value === 'function') {
            node.addEventListener(key.slice(2).toLowerCase(), value);
        } else if (value === true) {
            node.setAttribute(key, '');
        } else {
            node.setAttribute(key, String(value));
        }
    });

    append(node, children);
    return node;
}

function append(parent, children) {
    children.flat(Infinity).forEach((child) => {
        if (child === null || child === undefined || child === false) {
            return;
        }
        parent.appendChild(child instanceof Node ? child : document.createTextNode(String(child)));
    });
}

/**
 * Vacía un contenedor.
 *
 * @param {Element} node contenedor
 * @returns {Element} el mismo contenedor
 */
export function clear(node) {
    node.replaceChildren();
    return node;
}

/**
 * Marca un botón como ocupado y evita dobles envíos.
 *
 * @param {HTMLButtonElement} button botón
 * @param {boolean} busy estado
 * @param {string} [busyKey] clave de traducción del texto ocupado
 */
export function setBusy(button, busy, busyKey = 'common.loading') {
    if (!button) {
        return;
    }

    if (busy) {
        if (!button.dataset.originalText) {
            button.dataset.originalText = button.textContent;
        }
        button.disabled = true;
        button.setAttribute('aria-busy', 'true');
        button.textContent = t(busyKey);
    } else {
        button.disabled = false;
        button.removeAttribute('aria-busy');
        if (button.dataset.originalText) {
            button.textContent = button.dataset.originalText;
            delete button.dataset.originalText;
        }
    }
}

/**
 * Muestra un aviso flotante accesible.
 *
 * @param {string} message texto
 * @param {'success'|'danger'|'warning'|'info'} [type] tipo
 * @param {{timeout?: number}} [options] opciones
 * @returns {HTMLElement|null} aviso creado
 */
export function toast(message, type = 'success', options = {}) {
    const stack = document.getElementById('toast-stack');
    if (!stack) {
        return null;
    }

    const { timeout = type === 'danger' ? 9000 : 6000 } = options;
    const close = el('button', {
        type: 'button',
        class: 'toast-close',
        'aria-label': t('toast.dismiss'),
        onclick: () => remove()
    }, '×');

    const node = el('div', {
        class: `app-toast app-toast-${type}`,
        role: type === 'danger' ? 'alert' : 'status',
        'aria-live': type === 'danger' ? 'assertive' : 'polite'
    }, el('span', { class: 'app-toast-message', text: message }), close);

    stack.appendChild(node);

    let timer = null;
    const remove = () => {
        if (timer) {
            clearTimeout(timer);
        }
        node.remove();
    };

    if (timeout > 0) {
        timer = setTimeout(remove, timeout);
        node.addEventListener('mouseenter', () => clearTimeout(timer));
        node.addEventListener('focusin', () => clearTimeout(timer));
    }

    return node;
}

/**
 * Devuelve el detalle del servidor solo cuando el idioma activo es español.
 *
 * <p>La API redacta sus mensajes en español; en inglés se prefiere un texto
 * genérico antes que mezclar idiomas en la interfaz.</p>
 *
 * @param {ApiError} error error de la API
 * @returns {string|null} detalle legible o null
 */
function serverDetail(error) {
    const detail = error.problem?.detail;
    const hasFieldErrors = Object.keys(error.fieldErrors || {}).length > 0;
    return getLanguage() === 'es' && detail && !hasFieldErrors ? detail : null;
}

/**
 * Describe un error de la API en el idioma activo.
 *
 * @param {unknown} error error
 * @param {string} [contextKey] clave específica del contexto
 * @returns {string} mensaje legible
 */
export function describeError(error, contextKey) {
    if (!(error instanceof ApiError)) {
        return t('errors.generic');
    }

    switch (error.status) {
        case 0:
            return t(contextKey || 'errors.network');
        case 400:
            return serverDetail(error) || t(contextKey || 'errors.validation');
        case 401:
            return t(contextKey || 'errors.unauthorized');
        case 403:
            return t('errors.forbidden');
        case 404:
            return t(contextKey || 'errors.notFound');
        case 409:
            return t(contextKey || 'errors.conflict');
        case 413:
            return t('errors.fileTooLarge');
        case 429:
            return error.retryAfter
                ? t('errors.rateLimitDetail', { seconds: error.retryAfter })
                : t('errors.rateLimit');
        case 503:
            return error.retryAfter
                ? t('errors.serviceUnavailableDetail', { seconds: error.retryAfter })
                : t('errors.serviceUnavailable');
        default:
            return t('errors.server');
    }
}

/**
 * Muestra un error como aviso flotante.
 *
 * @param {unknown} error error
 * @param {string} [contextKey] clave específica
 */
export function notifyError(error, contextKey) {
    toast(describeError(error, contextKey), 'danger');
}

/**
 * Limpia los errores de un formulario.
 *
 * @param {HTMLFormElement} form formulario
 */
export function clearFormErrors(form) {
    qsa('.is-invalid', form).forEach((field) => field.classList.remove('is-invalid'));
    qsa('.invalid-feedback', form).forEach((node) => node.remove());
    qsa('[aria-invalid="true"]', form).forEach((field) => field.removeAttribute('aria-invalid'));
    const summary = qs('[data-form-error]', form);
    if (summary) {
        summary.textContent = '';
        summary.hidden = true;
    }
}

/**
 * Marca los campos inválidos con los errores devueltos por la API.
 *
 * En español se muestra el mensaje del servidor; en inglés se usa un mensaje
 * genérico para no mezclar idiomas.
 *
 * @param {HTMLFormElement} form formulario
 * @param {unknown} error error de la API
 */
export function showFormErrors(form, error) {
    clearFormErrors(form);

    const fieldErrors = error instanceof ApiError ? error.fieldErrors : {};
    let firstField = null;

    Object.entries(fieldErrors).forEach(([name, message]) => {
        const field = form.elements.namedItem(name);
        if (!(field instanceof HTMLElement)) {
            return;
        }

        field.classList.add('is-invalid');
        field.setAttribute('aria-invalid', 'true');
        firstField = firstField || field;

        const feedback = el('div', {
            class: 'invalid-feedback',
            text: getLanguage() === 'es' ? message : t('errors.invalidField')
        });

        if (field.parentElement) {
            field.parentElement.appendChild(feedback);
        }
    });

    const summary = qs('[data-form-error]', form);
    if (summary) {
        summary.textContent = describeError(error);
        summary.hidden = false;
    }

    if (firstField) {
        firstField.focus();
    }
}

/**
 * Muestra errores locales por nombre de campo.
 *
 * @param {HTMLFormElement} form formulario
 * @param {Record<string, string>} errors mensajes ya traducidos
 */
export function showLocalFieldErrors(form, errors) {
    clearFormErrors(form);
    let firstField = null;

    Object.entries(errors).forEach(([name, message]) => {
        const field = form.elements.namedItem(name);
        if (!(field instanceof HTMLElement)) {
            return;
        }
        field.classList.add('is-invalid');
        field.setAttribute('aria-invalid', 'true');
        firstField = firstField || field;

        if (field.parentElement) {
            field.parentElement.appendChild(el('div', {
                class: 'invalid-feedback',
                text: message
            }));
        }
    });

    if (firstField) {
        firstField.focus();
    }
}

/**
 * Abre un diálogo de confirmación nativo y accesible.
 *
 * @param {{title: string, message: string, confirmText?: string, cancelText?: string, danger?: boolean}} options opciones
 * @returns {Promise<boolean>} verdadero si se confirma
 */
export function confirmDialog(options) {
    return new Promise((resolve) => {
        const titleId = `confirm-title-${Date.now()}`;
        const messageId = `confirm-message-${Date.now()}`;

        const dialog = el('dialog', {
            class: 'confirm-dialog',
            'aria-labelledby': titleId,
            'aria-describedby': messageId
        });

        const cancel = el('button', {
            type: 'button',
            class: 'btn btn-outline-secondary',
            text: options.cancelText || t('common.cancel')
        });
        const confirm = el('button', {
            type: 'button',
            class: options.danger ? 'btn btn-danger' : 'btn btn-primary',
            text: options.confirmText || t('common.confirm')
        });

        dialog.append(
            el('div', { class: 'confirm-dialog-body' },
                el('h2', { id: titleId, class: 'h5', text: options.title }),
                el('p', { id: messageId, text: options.message })),
            el('div', { class: 'confirm-dialog-actions' }, cancel, confirm)
        );

        const finish = (value) => {
            dialog.close();
            dialog.remove();
            resolve(value);
        };

        cancel.addEventListener('click', () => finish(false));
        confirm.addEventListener('click', () => finish(true));
        dialog.addEventListener('cancel', (event) => {
            event.preventDefault();
            finish(false);
        });
        dialog.addEventListener('click', (event) => {
            if (event.target === dialog) {
                finish(false);
            }
        });

        document.body.appendChild(dialog);
        dialog.showModal();
        cancel.focus();
    });
}

/**
 * Crea un estado vacío con una acción opcional.
 *
 * @param {{title: string, text: string, action?: Node, icon?: string}} options contenido
 * @returns {HTMLElement} bloque
 */
export function emptyState(options) {
    return el('div', { class: 'empty-state' },
        el('p', { class: 'empty-state-icon', 'aria-hidden': 'true', text: options.icon || '📚' }),
        el('h2', { class: 'h4', text: options.title }),
        el('p', { class: 'text-muted', text: options.text }),
        options.action || null
    );
}

/**
 * Crea la paginación de una lista.
 *
 * @param {{page: number, totalPages: number, onChange: (page: number) => void, label?: string}} options datos
 * @returns {HTMLElement|null} navegación o null
 */
export function pagination(options) {
    const { page, totalPages, onChange, label } = options;
    if (!Number.isFinite(totalPages) || totalPages <= 1) {
        return null;
    }

    const previous = el('button', {
        type: 'button',
        class: 'btn btn-sm btn-outline-primary',
        text: `← ${t('common.previous')}`,
        disabled: page <= 0,
        onclick: () => onChange(page - 1)
    });
    const next = el('button', {
        type: 'button',
        class: 'btn btn-sm btn-outline-primary',
        text: `${t('common.next')} →`,
        disabled: page >= totalPages - 1,
        onclick: () => onChange(page + 1)
    });

    return el('nav', {
        class: 'pagination-bar',
        'aria-label': label || t('common.page')
    },
    previous,
    el('span', {
        class: 'pagination-status',
        text: t('common.pageOf', { page: page + 1, total: totalPages })
    }),
    next);
}

/**
 * Crea una insignia de estado de lectura.
 *
 * @param {string} status WANT_TO_READ, READING o READ
 * @returns {HTMLElement} insignia
 */
export function statusBadge(status) {
    return el('span', {
        class: `badge status-badge status-${String(status).toLowerCase()}`,
        text: t(`status.${status}`)
    });
}

/**
 * Crea la representación visual de una calificación.
 *
 * @param {number} rating 1-5
 * @param {string} [label] texto accesible alternativo
 * @returns {HTMLElement} estrellas
 */
export function starsElement(rating, label) {
    const value = Math.max(0, Math.min(5, Number(rating) || 0));
    const accessible = label || t('reviews.starsAria', { rating: value });

    return el('span', { class: 'stars', 'aria-label': accessible, role: 'img' },
        el('span', { 'aria-hidden': 'true', text: '★'.repeat(value) + '☆'.repeat(5 - value) }),
        el('span', { class: 'visually-hidden', text: accessible })
    );
}

/**
 * Crea una portada con marcador de respaldo si la imagen falla.
 *
 * @param {{title?: string, coverUrl?: string|null}} book libro
 * @param {'card'|'thumb'} [variant] variante visual
 * @returns {HTMLElement} contenedor de portada
 */
export function coverElement(book, variant = 'card') {
    const title = book && book.title ? book.title : t('common.unknown');
    const wrapper = el('div', { class: `book-cover book-cover-${variant}` });
    const fallback = () => {
        wrapper.replaceChildren(el('span', {
            class: 'cover-initials',
            'aria-hidden': 'true',
            text: initials(title)
        }));
        wrapper.classList.add('book-cover-placeholder');
    };

    if (book && book.coverUrl) {
        const image = el('img', {
            src: book.coverUrl,
            alt: '',
            loading: 'lazy',
            referrerpolicy: 'no-referrer'
        });
        image.addEventListener('error', fallback, { once: true });
        wrapper.appendChild(image);
    } else {
        fallback();
    }

    return wrapper;
}

/**
 * Obtiene las iniciales de un nombre.
 *
 * @param {string} value texto
 * @returns {string} una o dos iniciales
 */
export function initials(value) {
    const words = String(value || '')
        .trim()
        .split(/\s+/)
        .filter(Boolean);

    if (words.length === 0) {
        return 'LL';
    }
    if (words.length === 1) {
        return words[0].slice(0, 2).toUpperCase();
    }
    return (words[0][0] + words[words.length - 1][0]).toUpperCase();
}

/**
 * Formatea un número decimal corto.
 *
 * @param {number} value valor
 * @param {number} [digits] decimales
 * @returns {string} texto
 */
export function formatDecimal(value, digits = 1) {
    return new Intl.NumberFormat(getLocale(), {
        minimumFractionDigits: digits,
        maximumFractionDigits: digits
    }).format(Number(value) || 0);
}

/**
 * Convierte un número a texto con separadores.
 *
 * @param {number} value valor
 * @returns {string} texto
 */
export function numberText(value) {
    return formatNumber(value);
}

/**
 * Texto de recuento con singular y plural.
 *
 * <p>Usa la clave {@code baseKey.one} cuando el número es exactamente 1 y la
 * clave {@code baseKey} en el resto de casos, de modo que la interfaz nunca
 * muestre «1 libros».</p>
 *
 * @param {string} baseKey clave de traducción en plural
 * @param {number} count   cantidad
 * @param {object} [params] variables adicionales de la traducción
 * @returns {string} texto traducido
 */
export function countLabel(baseKey, count, params = {}) {
    const value = Number(count) || 0;
    return t(value === 1 ? `${baseKey}.one` : baseKey, {
        ...params,
        count: formatNumber(value)
    });
}

/**
 * Devuelve la fecha local de hoy en formato YYYY-MM-DD.
 *
 * @returns {string} fecha
 */
export function todayIso() {
    const now = new Date();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    return `${now.getFullYear()}-${month}-${day}`;
}

/**
 * Valida una ruta interna de continuación tras iniciar sesión.
 *
 * @param {string|null} value ruta solicitada
 * @param {string} fallback ruta por defecto
 * @returns {string} ruta segura
 */
export function safeNext(value, fallback) {
    if (!value) {
        return fallback;
    }

    try {
        const url = new URL(value, window.location.origin);
        if (url.origin !== window.location.origin) {
            return fallback;
        }
        if (!/^\/[a-z]+\.html$/i.test(url.pathname) || /\/(login|register)\.html$/i.test(url.pathname)) {
            return fallback;
        }
        return `${url.pathname}${url.search}`;
    } catch (error) {
        return fallback;
    }
}

/**
 * Crea una función con retardo.
 *
 * @param {Function} callback función
 * @param {number} wait milisegundos
 * @returns {Function} función retardada
 */
export function debounce(callback, wait = 300) {
    let timer = null;
    return (...args) => {
        clearTimeout(timer);
        timer = setTimeout(() => callback(...args), wait);
    };
}

/**
 * Activa o desactiva un botón de mostrar contraseña.
 *
 * @param {HTMLButtonElement} button botón
 * @param {HTMLInputElement} input campo
 */
export function setupPasswordToggle(button, input) {
    if (!button || !input) {
        return;
    }

    button.addEventListener('click', () => {
        const visible = input.type === 'text';
        input.type = visible ? 'password' : 'text';
        button.setAttribute('aria-pressed', String(!visible));
        button.setAttribute('aria-label', t(visible ? 'auth.showPassword' : 'auth.hidePassword'));
        button.textContent = t(visible ? 'auth.showPassword' : 'auth.hidePassword');
    });
}

/**
 * Lleva el foco al contenido principal.
 *
 * @param {HTMLElement} [target] destino
 */
export function focusMain(target) {
    const node = target || document.getElementById('contenido');
    if (node) {
        node.focus({ preventScroll: true });
    }
}

/**
 * Convierte un valor de formulario en número o null.
 *
 * @param {string} value texto
 * @returns {number|null} número o null
 */
export function optionalNumber(value) {
    if (value === null || value === undefined || String(value).trim() === '') {
        return null;
    }
    const number = Number(value);
    return Number.isFinite(number) ? number : null;
}

/**
 * Vacía un formulario y limpia sus errores.
 *
 * @param {HTMLFormElement} form formulario
 */
export function resetForm(form) {
    form.reset();
    clearFormErrors(form);
}
