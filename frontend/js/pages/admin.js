/*
 * Panel de administración: resumen de cuentas, búsqueda y filtros, acciones
 * sobre usuarios y auditoría. La autorización real la aplica la API: aquí solo
 * se oculta la interfaz a quien no tiene el rol ADMIN.
 */
import { ApiError, api } from '../api.js';
import { CONFIG } from '../config.js';
import {
    formatDateTime,
    formatNumber,
    onLanguageChange,
    t
} from '../i18n.js';
import { initLayout } from '../layout.js';
import {
    clear,
    confirmDialog,
    countLabel,
    describeError,
    el,
    emptyState,
    formatDecimal,
    notifyError,
    pagination,
    qs,
    setBusy,
    toast
} from '../ui.js';

const layout = await initLayout();

if (layout) {
    const currentUser = layout.user;
    const canAdminister = Boolean(currentUser && Array.isArray(currentUser.roles)
        && currentUser.roles.includes('ADMIN'));

    const state = {
        page: 0,
        size: 20,
        q: '',
        role: '',
        status: '',
        auditPage: 0,
        stats: null,
        usage: null,
        users: null,
        audit: null
    };
    let usersToken = 0;

    if (!canAdminister) {
        qs('#admin-forbidden').hidden = false;
        qs('#admin-content').hidden = true;
    } else {
        qs('#admin-content').hidden = false;
        bindEvents();
        await Promise.all([loadStats(), loadUsage(), loadUsers(0), loadAudit(0)]);

        onLanguageChange(() => {
            if (state.stats) {
                renderStats(state.stats);
            }
            if (state.usage) {
                renderUsage(state.usage);
            }
            if (state.users) {
                renderUsers(state.users);
            }
            if (state.audit) {
                renderAudit(state.audit);
            }
        });
    }

    function bindEvents() {
        qs('#admin-filters').addEventListener('submit', (event) => {
            event.preventDefault();
            state.q = qs('#admin-q').value.trim();
            state.role = qs('#admin-role').value;
            state.status = qs('#admin-status').value;
            void loadUsers(0);
        });

        qs('#admin-reset').addEventListener('click', () => {
            qs('#admin-q').value = '';
            qs('#admin-role').value = '';
            qs('#admin-status').value = '';
            state.q = '';
            state.role = '';
            state.status = '';
            void loadUsers(0);
        });
    }

    async function loadStats() {
        try {
            state.stats = await api('/api/admin/stats');
            renderStats(state.stats);
        } catch (error) {
            notifyError(error, 'admin.error.load');
        }
    }

    async function loadUsage() {
        try {
            state.usage = await api('/api/admin/usage');
            renderUsage(state.usage);
        } catch (error) {
            notifyError(error, 'admin.error.load');
        }
    }

    function renderUsage(usage) {
        const grid = clear(qs('#admin-usage'));
        const cards = [
            [t('admin.usage.entries'), formatNumber(usage.libraryEntries)],
            [t('admin.usage.perUser'), formatDecimal(usage.averageBooksPerUser, 1)],
            [t('admin.usage.finished'), formatNumber(usage.finishedBooks)],
            [t('admin.usage.reviews'), formatNumber(usage.reviews)],
            [t('admin.usage.goals', { year: usage.year }),
                `${formatNumber(usage.usersWithGoal)} (${formatDecimal(usage.goalAdoptionPercent, 1)} %)`],
            [t('admin.usage.active'), formatNumber(usage.activeUsersThisMonth)],
            [t('admin.usage.pages'), formatNumber(usage.pagesLoggedThisMonth)]
        ];

        cards.forEach(([label, value]) => {
            grid.appendChild(el('div', { class: 'stat-card' },
                el('span', { class: 'stat-label', text: label }),
                el('strong', { class: 'stat-value', text: value })
            ));
        });
    }

    function renderStats(stats) {
        const grid = clear(qs('#admin-stats'));
        const cards = [
            ['admin.stats.total', stats.totalUsers],
            ['admin.stats.enabled', stats.enabledUsers],
            ['admin.stats.admins', stats.adminUsers],
            ['admin.stats.locked', stats.lockedUsers],
            ['admin.stats.google', stats.googleUsers],
            ['admin.stats.newMonth', stats.newUsersThisMonth]
        ];

        cards.forEach(([key, value]) => {
            grid.appendChild(el('div', { class: 'stat-card' },
                el('span', { class: 'stat-label', text: t(key) }),
                el('strong', { class: 'stat-value', text: formatNumber(value) })
            ));
        });
    }

    async function loadUsers(page) {
        state.page = Math.max(0, page);
        const token = ++usersToken;

        const loading = qs('#admin-loading');
        const table = qs('#admin-users-table');
        const alert = qs('#admin-alert');
        const paginationBox = qs('#admin-pagination');

        loading.hidden = false;
        table.hidden = true;
        alert.hidden = true;
        clear(paginationBox);

        const params = new URLSearchParams();
        if (state.q) {
            params.set('q', state.q);
        }
        if (state.role) {
            params.set('role', state.role);
        }
        if (state.status) {
            params.set('status', state.status);
        }
        params.set('page', String(state.page));
        params.set('size', String(state.size));

        try {
            const data = await api(`/api/admin/users?${params.toString()}`);
            if (token !== usersToken) {
                return;
            }
            state.users = data;
            renderUsers(data);
            table.hidden = false;
        } catch (error) {
            if (token !== usersToken) {
                return;
            }
            showUsersError(error);
        } finally {
            if (token === usersToken) {
                loading.hidden = true;
            }
        }
    }

    function showUsersError(error) {
        const alert = clear(qs('#admin-alert'));
        alert.hidden = false;
        alert.appendChild(el('span', {
            text: error instanceof ApiError && error.status === 403
                ? t('admin.error.forbidden')
                : describeError(error, 'admin.error.load')
        }));
        alert.appendChild(el('button', {
            type: 'button',
            class: 'btn btn-sm btn-outline-danger ms-2',
            text: t('common.retry'),
            onclick: () => void loadUsers(state.page)
        }));
    }

    function renderUsers(data) {
        const container = clear(qs('#admin-users-table'));
        qs('#admin-count').textContent = countLabel('admin.count', data.totalElements);

        if (!data.items.length) {
            container.appendChild(emptyState({
                title: t('admin.empty'),
                text: t('admin.users.subtitle'),
                icon: '👤'
            }));
        } else {
            const head = el('tr', {},
                el('th', { scope: 'col', text: t('admin.table.user') }),
                el('th', { scope: 'col', text: t('admin.table.email') }),
                el('th', { scope: 'col', text: t('admin.table.provider') }),
                el('th', { scope: 'col', text: t('admin.table.roles') }),
                el('th', { scope: 'col', text: t('admin.table.status') }),
                el('th', { scope: 'col', text: t('admin.table.created') }),
                el('th', { scope: 'col', text: t('admin.table.actions') })
            );

            const body = el('tbody', {}, data.items.map(userRow));
            const table = el('table', { class: 'data-table admin-table' },
                el('caption', { class: 'visually-hidden', text: t('admin.table.caption') }),
                el('thead', {}, head),
                body);

            container.appendChild(el('div', { class: 'table-scroll' }, table));
        }

        const box = clear(qs('#admin-pagination'));
        const nav = pagination({
            page: data.page,
            totalPages: data.totalPages,
            label: t('admin.table.caption'),
            onChange: (page) => void loadUsers(page)
        });
        if (nav) {
            box.appendChild(nav);
        }
    }

    function userRow(user) {
        const self = user.id === currentUser.id;
        const hasAdmin = (user.roles || []).includes('ADMIN');
        const actions = el('div', { class: 'admin-actions' });

        actions.appendChild(actionButton('admin.action.view', 'btn-outline-secondary',
            () => void openDetail(user.id), user.username));

        const toggleLabel = user.enabled ? 'admin.action.disable' : 'admin.action.enable';
        actions.appendChild(actionButton(toggleLabel,
            user.enabled ? 'btn-outline-danger' : 'btn-outline-secondary',
            (event) => confirmAndRun(event.currentTarget, {
                path: `/api/admin/users/${user.id}/status`,
                method: 'PATCH',
                body: { enabled: !user.enabled },
                titleKey: user.enabled ? 'admin.confirm.disableTitle' : 'admin.confirm.enableTitle',
                messageKey: user.enabled ? 'admin.confirm.disableText' : 'admin.confirm.enableText',
                toastKey: user.enabled ? 'admin.toast.disabled' : 'admin.toast.enabled',
                danger: user.enabled,
                name: user.username
            }),
            self ? t('admin.action.self') : user.username,
            self));

        actions.appendChild(actionButton(hasAdmin ? 'admin.action.revokeAdmin' : 'admin.action.grantAdmin',
            hasAdmin ? 'btn-outline-danger' : 'btn-outline-primary',
            (event) => confirmAndRun(event.currentTarget, {
                path: `/api/admin/users/${user.id}/role`,
                method: 'PATCH',
                body: { admin: !hasAdmin },
                titleKey: hasAdmin ? 'admin.confirm.revokeTitle' : 'admin.confirm.grantTitle',
                messageKey: hasAdmin ? 'admin.confirm.revokeText' : 'admin.confirm.grantText',
                toastKey: hasAdmin ? 'admin.toast.revoked' : 'admin.toast.granted',
                danger: hasAdmin,
                name: user.username
            }),
            self ? t('admin.action.self') : user.username,
            self));

        if (user.locked) {
            actions.appendChild(actionButton('admin.action.unlock', 'btn-outline-primary',
                (event) => confirmAndRun(event.currentTarget, {
                    path: `/api/admin/users/${user.id}/unlock`,
                    method: 'POST',
                    titleKey: 'admin.confirm.unlockTitle',
                    messageKey: 'admin.confirm.unlockText',
                    toastKey: 'admin.toast.unlocked',
                    name: user.username
                }), user.username));
        }

        actions.appendChild(actionButton('admin.action.revokeSessions', 'btn-outline-danger',
            (event) => confirmAndRun(event.currentTarget, {
                path: `/api/admin/users/${user.id}/sessions/revoke`,
                method: 'POST',
                titleKey: 'admin.confirm.sessionsTitle',
                messageKey: 'admin.confirm.sessionsText',
                toastKey: 'admin.toast.sessionsRevoked',
                danger: true,
                name: user.username
            }), user.username));

        return el('tr', {},
            el('td', {},
                el('strong', { class: 'admin-username', text: user.username }),
                user.fullName ? el('span', { class: 'admin-subtext', text: user.fullName }) : null
            ),
            el('td', { class: 'admin-email', text: user.email }),
            el('td', {},
                el('span', {
                    class: `badge admin-provider admin-provider-${String(user.authProvider).toLowerCase()}`,
                    text: t(`admin.provider.${String(user.authProvider).toLowerCase()}`)
                })
            ),
            el('td', {}, (user.roles || []).map((role) => el('span', {
                class: 'badge role-badge me-1',
                text: t(`admin.role.${role}`)
            }))),
            el('td', {}, statusBadge(user)),
            el('td', { class: 'admin-date', text: formatDateTime(user.createdAt) }),
            el('td', {}, actions)
        );
    }

    function statusBadge(user) {
        const status = !user.enabled ? 'disabled' : (user.locked ? 'locked' : 'enabled');
        return el('span', {
            class: `badge admin-status admin-status-${status}`,
            text: t(`admin.status.${status}`)
        });
    }

    function actionButton(labelKey, className, handler, suffix, disabled = false) {
        const label = t(labelKey);
        const fullLabel = suffix ? `${label}: ${suffix}` : label;
        return el('button', {
            type: 'button',
            class: `btn btn-sm ${className}`,
            text: label,
            title: fullLabel,
            'aria-label': fullLabel,
            disabled,
            onclick: handler
        });
    }

    async function confirmAndRun(button, options) {
        const confirmed = await confirmDialog({
            title: t(options.titleKey),
            message: t(options.messageKey, { name: options.name }),
            confirmText: t('admin.confirm.confirm'),
            danger: Boolean(options.danger)
        });
        if (!confirmed) {
            return;
        }

        setBusy(button, true);
        try {
            await api(options.path, {
                method: options.method,
                body: options.body
            });
            toast(t(options.toastKey), 'success');
            await Promise.all([loadStats(), loadUsers(state.page), loadAudit(0)]);
        } catch (error) {
            notifyError(error, 'admin.error.action');
        } finally {
            setBusy(button, false);
        }
    }

    async function openDetail(id) {
        try {
            const user = await api(`/api/admin/users/${id}`);
            renderDetail(user);
            qs('#admin-detail').showModal();
        } catch (error) {
            notifyError(error, 'admin.error.load');
        }
    }

    function renderDetail(user) {
        const rows = clear(qs('#admin-detail-rows'));

        const add = (labelKey, value) => {
            rows.appendChild(el('div', { class: 'profile-row' },
                el('dt', { text: t(labelKey) }),
                el('dd', {
                    text: value === null || value === undefined || value === ''
                        ? t('common.notAvailable')
                        : value
                })
            ));
        };

        add('admin.detail.id', String(user.id));
        add('admin.detail.username', user.username);
        add('admin.detail.fullName', user.fullName);
        add('admin.detail.email', user.email);
        add('admin.detail.language', t(`lang.${user.language || 'es'}`));
        add('admin.detail.provider', t(`admin.provider.${String(user.authProvider).toLowerCase()}`));
        add('admin.detail.status', statusBadge(user).textContent);
        add('admin.detail.failedAttempts', formatNumber(user.failedAttempts));
        add('admin.detail.lockedUntil', user.lockedUntil ? formatDateTime(user.lockedUntil) : null);
        add('admin.detail.createdAt', formatDateTime(user.createdAt));
        add('admin.detail.updatedAt', user.updatedAt ? formatDateTime(user.updatedAt) : null);

        rows.appendChild(el('div', { class: 'profile-row' },
            el('dt', { text: t('admin.detail.roles') }),
            el('dd', {}, (user.roles || []).map((role) => el('span', {
                class: 'badge role-badge me-1',
                text: t(`admin.role.${role}`)
            })))
        ));
    }

    async function loadAudit(page) {
        state.auditPage = Math.max(0, page);
        const loading = qs('#admin-audit-loading');
        const container = qs('#admin-audit-table');
        const paginationBox = qs('#admin-audit-pagination');

        loading.hidden = false;
        container.hidden = true;
        clear(paginationBox);

        try {
            const data = await api(`/api/admin/audit?page=${state.auditPage}&size=10`);
            state.audit = data;
            renderAudit(data);
            container.hidden = false;
        } catch (error) {
            notifyError(error, 'admin.error.load');
        } finally {
            loading.hidden = true;
        }
    }

    function renderAudit(data) {
        const container = clear(qs('#admin-audit-table'));

        if (!data.items.length) {
            container.appendChild(emptyState({
                title: t('admin.audit.empty'),
                text: t('admin.audit.subtitle'),
                icon: '📋'
            }));
        } else {
            const head = el('tr', {},
                el('th', { scope: 'col', text: t('admin.audit.when') }),
                el('th', { scope: 'col', text: t('admin.audit.actor') }),
                el('th', { scope: 'col', text: t('admin.audit.target') }),
                el('th', { scope: 'col', text: t('admin.audit.action') }),
                el('th', { scope: 'col', text: t('admin.audit.detail') })
            );

            const body = el('tbody', {}, data.items.map((entry) => el('tr', {},
                el('td', { class: 'admin-date', text: formatDateTime(entry.createdAt) }),
                el('td', { text: entry.actorUsername }),
                el('td', { text: entry.targetUsername }),
                el('td', { text: t(`admin.audit.action.${entry.action}`) }),
                el('td', { text: entry.detail || t('common.notAvailable') })
            )));

            container.appendChild(el('div', { class: 'table-scroll' },
                el('table', { class: 'data-table admin-table' },
                    el('caption', { class: 'visually-hidden', text: t('admin.audit.title') }),
                    el('thead', {}, head),
                    body)));
        }

        const box = clear(qs('#admin-audit-pagination'));
        const nav = pagination({
            page: data.page,
            totalPages: data.totalPages,
            label: t('admin.audit.title'),
            onChange: (page) => void loadAudit(page)
        });
        if (nav) {
            box.appendChild(nav);
        }
    }
}
