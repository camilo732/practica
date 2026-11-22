/*
 * Portada pública: estado del servicio y bienvenida si ya hay sesión.
 */
import { initLayout } from '../layout.js';
import { api } from '../api.js';
import { onLanguageChange, t } from '../i18n.js';
import { qs } from '../ui.js';

const layout = await initLayout();

if (layout) {
    let serviceInfo = null;
    let serviceFailed = false;

    renderSession(layout.user);
    loadServiceInfo();

    onLanguageChange(() => {
        renderSession(layout.user);
        renderServiceStatus();
    });

    async function loadServiceInfo() {
        const text = qs('#status-text');
        if (text) {
            text.textContent = t('common.loading');
        }

        try {
            serviceInfo = await api('/api/public/info');
            serviceFailed = false;
        } catch (error) {
            serviceInfo = null;
            serviceFailed = true;
        }
        renderServiceStatus();
    }

    function renderServiceStatus() {
        const dot = qs('#status-dot');
        const text = qs('#status-text');
        const version = qs('#status-version');

        if (!dot || !text || !version) {
            return;
        }

        dot.classList.remove('up', 'down');

        if (serviceInfo) {
            dot.classList.add('up');
            text.textContent = t('index.statusUp');
            version.textContent = t('index.statusVersion', { version: serviceInfo.version || '1.0.0' });
        } else if (serviceFailed) {
            dot.classList.add('down');
            text.textContent = t('index.statusDown');
            version.textContent = '';
        }
    }
}

function renderSession(user) {
    const publicActions = qs('#hero-public-actions');
    const sessionBlock = qs('#hero-session');
    const sessionText = qs('#index-session-text');

    if (!publicActions || !sessionBlock) {
        return;
    }

    if (user) {
        publicActions.hidden = true;
        sessionBlock.hidden = false;
        if (sessionText) {
            sessionText.textContent = t('index.sessionActive', {
                name: user.fullName || user.username
            });
        }
    } else {
        publicActions.hidden = false;
        sessionBlock.hidden = true;
    }
}
