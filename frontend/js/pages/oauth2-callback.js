/*
 * Retorno de Google OAuth2: la cookie HttpOnly ya está puesta, así que se
 * canjea por un token de acceso en memoria y se vuelve a la página de destino.
 */
import { refreshSession } from '../api.js';
import { CONFIG } from '../config.js';
import { applyTranslations } from '../i18n.js';
import { qs, safeNext } from '../ui.js';

applyTranslations(document);

const params = new URLSearchParams(window.location.search);
const next = safeNext(params.get('next'), CONFIG.pages.dashboard);

const renewed = await refreshSession();

if (renewed) {
    window.location.replace(next);
} else {
    const card = qs('#oauth-card');
    const spinner = qs('#oauth-spinner');
    const status = qs('#oauth-status');
    const actions = qs('#oauth-actions');

    if (card) {
        card.setAttribute('aria-busy', 'false');
    }
    if (spinner) {
        spinner.remove();
    }
    if (status) {
        status.hidden = true;
    }
    if (actions) {
        actions.hidden = false;
    }
}
