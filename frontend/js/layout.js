/*
 * Cabecera, pie, guardián de sesión y cambio de idioma comunes a todas las
 * páginas. Cada página es un documento HTML real: no hay enrutador SPA.
 */
import { ensureSession, getCurrentUser, logout } from './api.js';
import {
    applyTranslations,
    getLanguage,
    hasStoredLanguage,
    onLanguageChange,
    setLanguage,
    t
} from './i18n.js';
import { CONFIG } from './config.js';
import { initials, qs, qsa } from './ui.js';

const NAV_ITEMS = [
    { page: 'dashboard', href: CONFIG.pages.dashboard, key: 'nav.dashboard' },
    { page: 'library', href: CONFIG.pages.library, key: 'nav.library' },
    { page: 'catalog', href: CONFIG.pages.catalog, key: 'nav.catalog' },
    { page: 'reviews', href: CONFIG.pages.reviews, key: 'nav.reviews' },
    { page: 'goals', href: CONFIG.pages.goals, key: 'nav.goals' },
    { page: 'data', href: CONFIG.pages.data, key: 'nav.data' },
    { page: 'admin', href: CONFIG.pages.admin, key: 'nav.admin', adminOnly: true }
];

function isAdmin(user) {
    return Boolean(user && Array.isArray(user.roles) && user.roles.includes('ADMIN'));
}

function visibleNavItems(user) {
    return NAV_ITEMS.filter((item) => !item.adminOnly || isAdmin(user));
}

let initialized = false;

/**
 * Inicializa la página: sesión, cabecera, pie, idioma y cierre de sesión.
 *
 * @returns {Promise<{user: object|null, page: string, authenticated: boolean}|null>}
 * null si la página redirige al login
 */
export async function initLayout() {
    if (initialized) {
        return { user: getCurrentUser(), page: pageName(), authenticated: Boolean(getCurrentUser()) };
    }
    initialized = true;

    const page = pageName();
    const auth = document.body.dataset.auth || 'optional';

    document.body.classList.add('is-booting');
    const user = await ensureSession();

    if (user && user.language && !hasStoredLanguage()) {
        setLanguage(user.language, { persist: false, notify: false });
    }

    if (auth === 'required' && !user) {
        redirectToLogin();
        return null;
    }

    renderHeader(user, page);
    renderFooter();
    applyTranslations(document);
    updateTitle(page);

    document.body.classList.remove('is-booting');
    document.body.classList.add('app-ready');

    document.addEventListener('click', (event) => {
        const logoutButton = event.target.closest('[data-action="logout"]');
        if (logoutButton) {
            event.preventDefault();
            void signOut();
            return;
        }

        const closeButton = event.target.closest('[data-close-dialog]');
        if (closeButton) {
            const dialog = closeButton.closest('dialog');
            if (dialog) {
                dialog.close();
            }
            return;
        }

        if (event.target instanceof HTMLDialogElement) {
            event.target.close();
        }
    });

    document.addEventListener('librolog:session-expired', () => {
        if (document.body.dataset.auth === 'required') {
            redirectToLogin(true);
        }
    });

    document.addEventListener('click', (event) => {
        const header = document.getElementById('site-header');
        const menu = qs('#main-menu');
        const toggler = qs('.navbar-toggler', header || document);
        if (header && menu && toggler && !header.contains(event.target)) {
            menu.classList.remove('show');
            toggler.setAttribute('aria-expanded', 'false');
        }
    });

    document.addEventListener('keydown', (event) => {
        if (event.key !== 'Escape') {
            return;
        }
        const menu = qs('#main-menu');
        const toggler = qs('.navbar-toggler');
        if (menu && toggler) {
            menu.classList.remove('show');
            toggler.setAttribute('aria-expanded', 'false');
        }
    });

    onLanguageChange(() => {
        renderHeader(getCurrentUser(), page);
        updateTitle(page);
        applyTranslations(document);
    });

    return { user, page, authenticated: Boolean(user) };
}

function pageName() {
    return document.body.dataset.page || 'index';
}

function redirectToLogin(expired = false) {
    const next = encodeURIComponent(`${window.location.pathname}${window.location.search}`);
    const suffix = expired ? '&expired=1' : '';
    window.location.replace(`${CONFIG.pages.login}?next=${next}${suffix}`);
}

async function signOut() {
    await logout();
    window.location.href = CONFIG.pages.index;
}

function updateTitle(page) {
    const key = document.body.dataset.titleKey || `page.${page}.title`;
    document.title = `${t(key)} · ${CONFIG.appName}`;
}

/**
 * Pinta la cabecera con navegación, usuario e idioma.
 *
 * @param {object|null} user usuario actual
 * @param {string} page página activa
 */
export function renderHeader(user, page) {
    const header = document.getElementById('site-header');
    if (!header) {
        return;
    }

    const navLinks = visibleNavItems(user).map((item) => {
        const current = item.page === page ? 'page' : null;
        return `<li class="nav-item">
            <a class="nav-link${current ? ' active' : ''}" href="${item.href}"
               ${current ? 'aria-current="page"' : ''} data-i18n="${item.key}">${t(item.key)}</a>
        </li>`;
    }).join('');

    const publicActions = `
        <a class="btn btn-sm btn-outline-primary" href="${CONFIG.pages.login}" data-i18n="nav.login">${t('nav.login')}</a>
        <a class="btn btn-sm btn-primary" href="${CONFIG.pages.register}" data-i18n="nav.register">${t('nav.register')}</a>`;

    const userActions = user ? `
        <a class="user-chip" href="${CONFIG.pages.profile}" title="${escapeAttribute(t('nav.profile'))}">
            <span class="user-avatar" aria-hidden="true">${escapeText(initials(user.fullName || user.username))}</span>
            <span class="user-name">${escapeText(user.username)}</span>
        </a>
        <button type="button" class="btn btn-sm btn-outline-secondary" data-action="logout"
                data-i18n="nav.logout">${t('nav.logout')}</button>` : publicActions;

    header.innerHTML = `
        <nav class="navbar navbar-expand-lg site-navbar" aria-label="${escapeAttribute(t('a11y.mainNav'))}">
            <div class="container nav-shell">
                <a class="navbar-brand brand" href="${user ? CONFIG.pages.dashboard : CONFIG.pages.index}">
                    <img src="/assets/logo.svg" alt="" width="34" height="34" class="brand-mark">
                    <span class="brand-text">
                        <span class="brand-name">${CONFIG.appName}</span>
                        <span class="brand-tagline" data-i18n="app.tagline">${t('app.tagline')}</span>
                    </span>
                </a>

                <button class="navbar-toggler" type="button" aria-controls="main-menu"
                        aria-expanded="false" aria-label="${escapeAttribute(t('a11y.menu'))}">
                    <span class="navbar-toggler-icon" aria-hidden="true"></span>
                </button>

                <div class="collapse navbar-collapse" id="main-menu">
                    ${user ? `<ul class="navbar-nav main-nav">${navLinks}</ul>` : ''}
                    <div class="nav-actions">
                        <div class="lang-switch" role="group" aria-label="${escapeAttribute(t('a11y.language'))}">
                            <button type="button" data-lang="es" aria-pressed="${getLanguage() === 'es'}"
                                    title="${escapeAttribute(t('lang.es'))}">${t('lang.esShort')}</button>
                            <button type="button" data-lang="en" aria-pressed="${getLanguage() === 'en'}"
                                    title="${escapeAttribute(t('lang.en'))}">${t('lang.enShort')}</button>
                            <button type="button" data-lang="de" aria-pressed="${getLanguage() === 'de'}"
                                    title="${escapeAttribute(t('lang.de'))}">${t('lang.deShort')}</button>
                        </div>
                        ${userActions}
                    </div>
                </div>
            </div>
        </nav>`;

    qsa('[data-lang]', header).forEach((button) => {
        button.addEventListener('click', () => setLanguage(button.dataset.lang));
    });

    const toggler = qs('.navbar-toggler', header);
    const menu = qs('#main-menu', header);

    if (toggler && menu) {
        const closeMenu = () => {
            menu.classList.remove('show');
            toggler.setAttribute('aria-expanded', 'false');
        };

        toggler.addEventListener('click', (event) => {
            event.stopPropagation();
            const open = menu.classList.toggle('show');
            toggler.setAttribute('aria-expanded', String(open));
        });

        qsa('a', menu).forEach((link) => link.addEventListener('click', closeMenu));
    }
}

function renderFooter() {
    const footer = document.getElementById('site-footer');
    if (!footer) {
        return;
    }

    footer.innerHTML = `
        <div class="container footer-shell">
            <div>
                <p class="footer-brand">${CONFIG.appName}</p>
                <p class="footer-note" data-i18n="footer.project">${t('footer.project')}</p>
                <p class="footer-note" data-i18n="footer.stack">${t('footer.stack')}</p>
            </div>
            <div class="footer-links">
                <a href="/swagger-ui.html" target="_blank" rel="noopener noreferrer"
                   aria-label="${escapeAttribute(t('footer.docs'))} (${escapeAttribute(t('a11y.external'))})"
                   data-i18n="footer.docs">${t('footer.docs')}</a>
                <span class="footer-copy">${t('footer.rights', { year: new Date().getFullYear() })}</span>
            </div>
        </div>`;
}

function escapeText(value) {
    return String(value ?? '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;');
}

function escapeAttribute(value) {
    return String(value ?? '')
        .replace(/&/g, '&amp;')
        .replace(/"/g, '&quot;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;');
}
