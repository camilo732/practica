/*
 * Registro de cuentas locales. El backend devuelve la sesión ya iniciada.
 */
import { ApiError, authProviders, register } from '../api.js';
import { CONFIG } from '../config.js';
import { getLanguage, setLanguage, t } from '../i18n.js';
import { initLayout } from '../layout.js';
import { isEmail, passwordError, requireField, usernameError, valueOf } from '../forms.js';
import {
    clearFormErrors,
    describeError,
    notifyError,
    qs,
    setBusy,
    setupPasswordToggle,
    showFormErrors,
    showLocalFieldErrors,
    toast
} from '../ui.js';

const layout = await initLayout();

if (layout) {
    const form = qs('#register-form');
    const submit = qs('#register-submit');
    const languageSelect = qs('#language');

    setupPasswordToggle(qs('#password-toggle'), qs('#password'));

    if (languageSelect) {
        languageSelect.value = getLanguage();
        languageSelect.addEventListener('change', () => {
            setLanguage(languageSelect.value);
        });
    }

    void setupGoogle();

    async function setupGoogle() {
        const panel = qs('#google-login');
        if (!panel) {
            return;
        }
        try {
            const providers = await authProviders();
            if (!providers.google) {
                return;
            }
            const button = qs('#google-login-button');
            if (button) {
                button.href = `/api/auth/oauth2/start?next=${encodeURIComponent(CONFIG.pages.dashboard)}`;
            }
            panel.hidden = false;
        } catch (error) {
            // El registro local sigue disponible aunque falle la consulta.
        }
    }

    form.addEventListener('submit', async (event) => {
        event.preventDefault();
        clearFormErrors(form);

        const username = valueOf(form, 'username');
        const email = valueOf(form, 'email');
        const password = valueOf(form, 'password');
        const fullName = valueOf(form, 'fullName');
        const language = valueOf(form, 'language') || getLanguage();
        const errors = {};

        requireField(errors, 'username', username);
        if (!errors.username) {
            const message = usernameError(username);
            if (message) {
                errors.username = message;
            }
        }

        requireField(errors, 'email', email);
        if (!errors.email && !isEmail(email)) {
            errors.email = t('validation.email');
        }

        if (!password) {
            errors.password = t('validation.required');
        } else {
            const message = passwordError(password);
            if (message) {
                errors.password = message;
            }
        }

        if (fullName.length > 120) {
            errors.fullName = t('validation.maxLength', { max: 120 });
        }

        if (Object.keys(errors).length > 0) {
            showLocalFieldErrors(form, errors);
            return;
        }

        setBusy(submit, true, 'auth.register.submitting');
        try {
            const user = await register({
                username,
                email,
                password,
                fullName: fullName || null,
                language
            });

            setLanguage(user && user.language ? user.language : language);
            toast(t('auth.successRegister'), 'success');
            window.location.assign(CONFIG.pages.dashboard);
        } catch (error) {
            if (error instanceof ApiError && Object.keys(error.fieldErrors).length > 0) {
                showFormErrors(form, error);
            }
            const summary = qs('[data-form-error]', form);
            if (summary) {
                summary.textContent = describeError(error,
                    error instanceof ApiError && error.status === 409
                        ? 'errors.registerConflict'
                        : undefined);
                summary.hidden = false;
            }
            notifyError(error,
                error instanceof ApiError && error.status === 409
                    ? 'errors.registerConflict'
                    : undefined);
        } finally {
            setBusy(submit, false);
        }
    });
}
