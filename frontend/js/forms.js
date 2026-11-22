/*
 * Validaciones locales compartidas. La API vuelve a validar siempre: estas
 * comprobaciones solo mejoran la respuesta inmediata en el navegador.
 */
import { t } from './i18n.js';

/**
 * Devuelve el valor recortado de un campo.
 *
 * @param {HTMLFormElement} form formulario
 * @param {string} name nombre del campo
 * @returns {string} valor
 */
export function valueOf(form, name) {
    const field = form.elements.namedItem(name);
    return field && 'value' in field ? String(field.value).trim() : '';
}

/**
 * Comprueba un correo con una expresión sencilla y suficiente para la interfaz.
 *
 * @param {string} value correo
 * @returns {boolean} válido
 */
export function isEmail(value) {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
}

/**
 * Valida un nombre de usuario como el backend.
 *
 * @param {string} value usuario
 * @returns {string|null} mensaje o null
 */
export function usernameError(value) {
    if (!value) {
        return t('validation.required');
    }
    if (!/^[A-Za-z0-9._-]{3,30}$/.test(value)) {
        return t('validation.username');
    }
    return null;
}

/**
 * Valida una contraseña como el backend.
 *
 * @param {string} value contraseña
 * @returns {string|null} mensaje o null
 */
export function passwordError(value) {
    if (!value) {
        return t('validation.required');
    }
    if (value.length < 10 || value.length > 72 || !/[A-Za-z]/.test(value) || !/\d/.test(value)) {
        return t('validation.password');
    }
    return null;
}

/**
 * Añade un error si el valor está vacío.
 *
 * @param {Record<string, string>} errors acumulador
 * @param {string} name campo
 * @param {string} value valor
 * @param {string} [message] mensaje alternativo
 */
export function requireField(errors, name, value, message) {
    if (!value) {
        errors[name] = message || t('validation.required');
    }
}

/**
 * Comprueba un número entero dentro de un rango.
 *
 * @param {Record<string, string>} errors acumulador
 * @param {string} name campo
 * @param {string} value texto
 * @param {{min?: number, max?: number, required?: boolean}} [options] rango
 */
export function integerField(errors, name, value, options = {}) {
    const { min = 0, max = Number.MAX_SAFE_INTEGER, required = false } = options;

    if (!value) {
        if (required) {
            errors[name] = t('validation.required');
        }
        return;
    }

    const number = Number(value);
    if (!Number.isInteger(number)) {
        errors[name] = t('validation.number');
        return;
    }
    if (number < min) {
        errors[name] = t('validation.min', { min });
    }
    if (number > max) {
        errors[name] = t('validation.max', { max });
    }
}

/**
 * Comprueba una fecha ISO opcional.
 *
 * @param {Record<string, string>} errors acumulador
 * @param {string} name campo
 * @param {string} value fecha
 */
export function dateField(errors, name, value) {
    if (value && !/^\d{4}-\d{2}-\d{2}$/.test(value)) {
        errors[name] = t('validation.date');
    }
}
