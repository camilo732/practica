/*
 * Perfil: datos de la cuenta, estado de la sesión, idioma y accesos útiles.
 */
import { api, getCurrentUser } from '../api.js';
import {
    formatDateTime,
    getLanguage,
    onLanguageChange,
    setLanguage,
    t
} from '../i18n.js';
import { initLayout } from '../layout.js';
import {
    clear,
    describeError,
    el,
    initials,
    qs,
    toast
} from '../ui.js';

const layout = await initLayout();

if (layout) {
    const languageSelect = qs('#profile-language');
    languageSelect.value = getLanguage();
    languageSelect.addEventListener('change', () => {
        setLanguage(languageSelect.value);
        toast(t('toast.saved'), 'success');
    });

    let user = null;

    await load();

    onLanguageChange(() => {
        languageSelect.value = getLanguage();
        if (user) {
            render(user);
        }
    });

    async function load() {
        const alert = qs('#profile-alert');
        const loading = qs('#profile-loading');
        const content = qs('#profile-content');

        alert.hidden = true;
        loading.hidden = false;
        content.hidden = true;

        try {
            user = await api('/api/auth/me');
            render(user);
        } catch (error) {
            clear(alert);
            alert.appendChild(el('span', { text: describeError(error, 'profile.error.load') }));
            alert.appendChild(el('button', {
                type: 'button',
                class: 'btn btn-sm btn-outline-danger ms-2',
                text: t('common.retry'),
                onclick: () => void load()
            }));
            alert.hidden = false;
            user = getCurrentUser();
        } finally {
            loading.hidden = true;
        }
    }

    function render(data) {
        qs('#profile-content').hidden = false;
        qs('#profile-avatar').textContent = initials(data.fullName || data.username);
        qs('#profile-display-name').textContent = data.fullName || data.username;
        languageSelect.value = getLanguage();

        const rows = clear(qs('#profile-rows'));
        rows.append(
            row('profile.username', data.username),
            row('profile.fullName', data.fullName || t('profile.noFullName')),
            row('profile.email', data.email),
            row('profile.language', t(`lang.${data.language || 'es'}`)),
            row('profile.status', data.enabled ? t('profile.enabled') : t('profile.disabled')),
            row('profile.createdAt', formatDateTime(data.createdAt))
        );

        const rolesRow = el('div', { class: 'profile-row' },
            el('dt', { text: t('profile.roles') }),
            el('dd', {},
                (data.roles || []).map((role) => el('span', {
                    class: 'badge role-badge me-1',
                    text: role
                }))
            )
        );
        rows.appendChild(rolesRow);

        const adminCard = qs('#profile-admin-card');
        if (adminCard) {
            adminCard.hidden = !(data.roles || []).includes('ADMIN');
        }
    }

    function row(labelKey, value) {
        return el('div', { class: 'profile-row' },
            el('dt', { text: t(labelKey) }),
            el('dd', { text: value || t('common.notAvailable') })
        );
    }
}
