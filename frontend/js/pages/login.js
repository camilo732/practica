/*
 * Inicio de sesión: token en memoria y cookie de refresco HttpOnly.
 */
import { ApiError, authProviders, login } from '../api.js';
import { CONFIG } from '../config.js';
import { hasStoredLanguage, setLanguage, t } from '../i18n.js';
import { initLayout } from '../layout.js';
import { requireField, valueOf } from '../forms.js';
import {
    clearFormErrors,
    describeError,
    notifyError,
    qs,
    safeNext,
    setBusy,
    setupPasswordToggle,
    showFormErrors,
    showLocalFieldErrors,
    toast
} from '../ui.js';

const layout = await initLayout();

if (layout) {
    const form = qs('#login-form');
    const submit = qs('#login-submit');
    const params = new URLSearchParams(window.location.search);
    const next = safeNext(params.get('next'), CONFIG.pages.dashboard);

    setupPasswordToggle(qs('#password-toggle'), qs('#password'));

    if (layout.user) {
        const banner = qs('#login-session');
        if (banner) {
            banner.hidden = false;
        }
    }

    if (params.get('expired') === '1') {
        toast(t('errors.unauthorized'), 'warning');
    }

    const oauthStatus = params.get('oauth');
    if (oauthStatus === 'error') {
        toast(t('auth.googleError'), 'danger');
    } else if (oauthStatus === 'unavailable') {
        toast(t('auth.googleUnavailable'), 'warning');
    }

    void setupGoogle(next);

    async function setupGoogle(target) {
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
                button.href = `/api/auth/oauth2/start?next=${encodeURIComponent(target)}`;
            }
            panel.hidden = false;
        } catch (error) {
            // El acceso local sigue disponible aunque falle la consulta.
        }
    }

    form.addEventListener('submit', async (event) => {
        event.preventDefault();
        clearFormErrors(form);

        const usernameOrEmail = valueOf(form, 'usernameOrEmail');
        const password = valueOf(form, 'password');
        const errors = {};
        requireField(errors, 'usernameOrEmail', usernameOrEmail);
        requireField(errors, 'password', password);

        if (Object.keys(errors).length > 0) {
            showLocalFieldErrors(form, errors);
            return;
        }

        setBusy(submit, true, 'auth.login.submitting');
        try {
            const user = await login({ usernameOrEmail, password });

            if (!hasStoredLanguage() && user && user.language) {
                setLanguage(user.language, { persist: false });
            }

            toast(t('auth.successLogin'), 'success');
            window.location.assign(next);
        } catch (error) {
            if (error instanceof ApiError && Object.keys(error.fieldErrors).length > 0) {
                showFormErrors(form, error);
            }
            const summary = qs('[data-form-error]', form);
            if (summary) {
                summary.textContent = describeError(error,
                    error instanceof ApiError && error.status === 401
                        ? 'errors.invalidCredentials'
                        : undefined);
                summary.hidden = false;
            }
            if (!(error instanceof ApiError) || error.status !== 400) {
                notifyError(error,
                    error instanceof ApiError && error.status === 401
                        ? 'errors.invalidCredentials'
                        : undefined);
            }
        } finally {
            setBusy(submit, false);
        }
    });
}
