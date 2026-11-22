/*
 * Motor de internacionalización del frontend.
 *
 * El idioma se guarda en localStorage, se aplica al documento completo y se
 * anuncia con un evento para que las páginas vuelvan a pintar sus datos.
 */
import { CONFIG } from './config.js';
import es from './i18n/es.js';
import en from './i18n/en.js';
import de from './i18n/de.js';

const dictionaries = { es, en, de };
const LOCALES = { es: 'es-ES', en: 'en-GB', de: 'de-DE' };
const listeners = new Set();

let currentLanguage = readStoredLanguage();

function readStoredLanguage() {
    try {
        const stored = localStorage.getItem(CONFIG.storageKeys.language);
        if (CONFIG.languages.includes(stored)) {
            return stored;
        }
    } catch (error) {
        // El almacenamiento puede estar bloqueado; se usa el idioma del navegador.
    }

    const browser = (navigator.language || CONFIG.defaultLanguage).toLowerCase();
    if (browser.startsWith('es')) {
        return 'es';
    }
    return browser.startsWith('de') ? 'de' : 'en';
}

/**
 * Devuelve el idioma activo.
 *
 * @returns {string} código es, en o de
 */
export function getLanguage() {
    return currentLanguage;
}

/**
 * Devuelve el locale de Intl asociado al idioma activo.
 *
 * @returns {string} locale BCP 47
 */
export function getLocale() {
    return LOCALES[currentLanguage] || LOCALES[CONFIG.defaultLanguage];
}

/**
 * Traduce una clave y sustituye los marcadores {nombre}.
 *
 * @param {string} key clave del diccionario
 * @param {Record<string, unknown>} [params] valores de los marcadores
 * @returns {string} texto traducido
 */
export function t(key, params) {
    const dictionary = dictionaries[currentLanguage] || dictionaries[CONFIG.defaultLanguage];
    let value = dictionary[key];

    if (value === undefined) {
        value = dictionaries[CONFIG.defaultLanguage][key];
    }
    if (value === undefined) {
        return key;
    }

    if (params) {
        value = value.replace(/\{(\w+)\}/g, (match, name) =>
            Object.prototype.hasOwnProperty.call(params, name) ? String(params[name]) : match);
    }
    return value;
}

/**
 * Cambia el idioma activo, lo persiste y vuelve a traducir el documento.
 *
 * @param {string} language código es, en o de
 * @param {{persist?: boolean, notify?: boolean}} [options] opciones
 * @returns {boolean} verdadero si el idioma era válido
 */
export function setLanguage(language, options = {}) {
    if (!CONFIG.languages.includes(language)) {
        return false;
    }

    const { persist = true, notify = true } = options;
    const changed = language !== currentLanguage;
    currentLanguage = language;
    document.documentElement.lang = language;

    if (persist) {
        try {
            localStorage.setItem(CONFIG.storageKeys.language, language);
        } catch (error) {
            // Sin persistencia el cambio sigue funcionando en esta página.
        }
    }

    applyTranslations(document);

    if (notify && changed) {
        listeners.forEach((listener) => {
            try {
                listener(language);
            } catch (error) {
                console.error('Error al actualizar una vista tras cambiar de idioma', error);
            }
        });
        document.dispatchEvent(new CustomEvent('librolog:languagechange', {
            detail: { language }
        }));
    }

    return true;
}

/**
 * Indica si el navegador ya guardó una preferencia de idioma.
 *
 * @returns {boolean} verdadero si existe preferencia
 */
export function hasStoredLanguage() {
    try {
        return CONFIG.languages.includes(localStorage.getItem(CONFIG.storageKeys.language));
    } catch (error) {
        return false;
    }
}

/**
 * Suscribe una función a los cambios de idioma.
 *
 * @param {(language: string) => void} listener función a ejecutar
 * @returns {() => void} función para cancelar la suscripción
 */
export function onLanguageChange(listener) {
    listeners.add(listener);
    return () => listeners.delete(listener);
}

/**
 * Traduce todos los nodos marcados dentro de un contenedor.
 *
 * Marcadores admitidos:
 * data-i18n (texto), data-i18n-placeholder, data-i18n-title,
 * data-i18n-aria-label y data-i18n-content (meta description).
 *
 * @param {ParentNode} [root] contenedor a traducir
 */
export function applyTranslations(root = document) {
    document.documentElement.lang = currentLanguage;

    root.querySelectorAll('[data-i18n]').forEach((node) => {
        node.textContent = t(node.dataset.i18n);
    });

    root.querySelectorAll('[data-i18n-placeholder]').forEach((node) => {
        node.setAttribute('placeholder', t(node.dataset.i18nPlaceholder));
    });

    root.querySelectorAll('[data-i18n-title]').forEach((node) => {
        node.setAttribute('title', t(node.dataset.i18nTitle));
    });

    root.querySelectorAll('[data-i18n-aria-label]').forEach((node) => {
        node.setAttribute('aria-label', t(node.dataset.i18nAriaLabel));
    });

    root.querySelectorAll('[data-i18n-content]').forEach((node) => {
        node.setAttribute('content', t(node.dataset.i18nContent));
    });
}

/**
 * Formatea un número con el locale activo.
 *
 * @param {number} value valor
 * @param {Intl.NumberFormatOptions} [options] opciones
 * @returns {string} número formateado
 */
export function formatNumber(value, options = {}) {
    return new Intl.NumberFormat(getLocale(), options).format(Number(value) || 0);
}

/**
 * Formatea un porcentaje entero.
 *
 * @param {number} value porcentaje
 * @returns {string} porcentaje formateado
 */
export function formatPercent(value) {
    return new Intl.NumberFormat(getLocale(), {
        style: 'percent',
        maximumFractionDigits: 0
    }).format((Number(value) || 0) / 100);
}

/**
 * Formatea una fecha ISO (YYYY-MM-DD) sin desplazarla por la zona horaria.
 *
 * @param {string|null|undefined} value fecha ISO
 * @param {Intl.DateTimeFormatOptions} [options] opciones
 * @returns {string} fecha formateada o cadena vacía
 */
export function formatDate(value, options = { dateStyle: 'medium' }) {
    if (!value) {
        return '';
    }

    const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(String(value));
    const date = match
        ? new Date(Number(match[1]), Number(match[2]) - 1, Number(match[3]))
        : new Date(value);

    if (Number.isNaN(date.getTime())) {
        return '';
    }
    return new Intl.DateTimeFormat(getLocale(), options).format(date);
}

/**
 * Formatea una marca temporal ISO.
 *
 * @param {string|null|undefined} value instante ISO
 * @returns {string} fecha y hora formateadas o cadena vacía
 */
export function formatDateTime(value) {
    if (!value) {
        return '';
    }
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        return '';
    }
    return new Intl.DateTimeFormat(getLocale(), {
        dateStyle: 'medium',
        timeStyle: 'short'
    }).format(date);
}

/**
 * Devuelve el nombre de un mes (1-12) en el idioma activo.
 *
 * @param {number} month mes
 * @param {'long'|'short'} [style] longitud
 * @returns {string} nombre del mes
 */
export function formatMonth(month, style = 'long') {
    const date = new Date(2024, Math.max(0, Math.min(11, Number(month) - 1)), 1);
    return new Intl.DateTimeFormat(getLocale(), { month: style }).format(date);
}

/**
 * Devuelve el nombre de un mes en mayúscula inicial.
 *
 * @param {number} month mes
 * @returns {string} nombre capitalizado
 */
export function monthLabel(month) {
    const value = formatMonth(month, 'long');
    return value.charAt(0).toUpperCase() + value.slice(1);
}
