/*
 * Biblioteca personal: filtros, progreso, edición, alta manual, reseñas y
 * eliminación. Toda la autorización real la aplica la API.
 */
import { ApiError, api } from '../api.js';
import { CONFIG } from '../config.js';
import { formatDate, formatNumber, formatPercent, onLanguageChange, t } from '../i18n.js';
import { initLayout } from '../layout.js';
import { dateField, integerField, requireField, valueOf } from '../forms.js';
import {
    clear,
    clearFormErrors,
    confirmDialog,
    countLabel,
    coverElement,
    describeError,
    el,
    emptyState,
    notifyError,
    pagination,
    qs,
    resetForm,
    setBusy,
    showFormErrors,
    showLocalFieldErrors,
    statusBadge,
    toast
} from '../ui.js';

const layout = await initLayout();

if (layout) {
    const state = {
        page: 0,
        size: CONFIG.libraryPageSize,
        status: '',
        data: null
    };
    let loadToken = 0;
    let currentEntry = null;

    bindEvents();
    await load(0);

    onLanguageChange(() => {
        if (state.data) {
            renderList();
        }
    });

    function bindEvents() {
        qs('#library-status').addEventListener('change', (event) => {
            state.status = event.target.value;
            void load(0);
        });

        qs('#open-manual').addEventListener('click', openManual);

        qs('#progress-form').addEventListener('submit', submitProgress);
        qs('#edit-form').addEventListener('submit', submitEdit);
        qs('#manual-form').addEventListener('submit', submitManual);
    }

    async function load(page) {
        const token = ++loadToken;
        state.page = Math.max(0, page);

        const alert = qs('#library-alert');
        const loadingBlock = qs('#library-loading');
        const content = qs('#library-content');

        alert.hidden = true;
        if (!state.data) {
            loadingBlock.hidden = false;
        }
        content.setAttribute('aria-busy', 'true');

        const params = new URLSearchParams({
            page: String(state.page),
            size: String(state.size)
        });
        if (state.status) {
            params.set('status', state.status);
        }

        try {
            const data = await api(`/api/library?${params.toString()}`);
            if (token !== loadToken) {
                return;
            }
            state.data = data;
            renderList();
        } catch (error) {
            if (token !== loadToken) {
                return;
            }
            clear(alert);
            alert.appendChild(el('span', {
                text: describeError(error, 'library.error.load')
            }));
            alert.appendChild(el('button', {
                type: 'button',
                class: 'btn btn-sm btn-outline-danger ms-2',
                text: t('common.retry'),
                onclick: () => void load(state.page)
            }));
            alert.hidden = false;
            if (!state.data) {
                clear(content);
            }
        } finally {
            if (token === loadToken) {
                loadingBlock.hidden = true;
                content.removeAttribute('aria-busy');
            }
        }
    }

    function renderList() {
        const content = clear(qs('#library-content'));
        const data = state.data;
        const count = qs('#library-count');

        if (!data) {
            return;
        }

        count.textContent = countLabel('library.count', data.totalElements);

        if (data.items.length === 0) {
            renderEmpty(content);
            return;
        }

        const grid = el('div', { class: 'book-grid' });
        data.items.forEach((entry) => grid.appendChild(renderCard(entry)));
        content.appendChild(grid);

        const nav = pagination({
            page: data.page,
            totalPages: data.totalPages,
            label: t('library.pagination.label'),
            onChange: (page) => void load(page)
        });
        if (nav) {
            content.appendChild(nav);
        }
    }

    function renderEmpty(content) {
        const filtered = Boolean(state.status);
        const actions = el('div', { class: 'quick-actions' });

        if (filtered) {
            actions.appendChild(el('button', {
                type: 'button',
                class: 'btn btn-outline-primary',
                text: t('common.all'),
                onclick: () => {
                    state.status = '';
                    qs('#library-status').value = '';
                    void load(0);
                }
            }));
        } else {
            actions.appendChild(el('a', {
                class: 'btn btn-primary',
                href: CONFIG.pages.catalog,
                text: t('library.addCatalog')
            }));
            actions.appendChild(el('button', {
                type: 'button',
                class: 'btn btn-outline-primary',
                text: t('library.addManual'),
                onclick: openManual
            }));
        }

        content.appendChild(emptyState({
            icon: '▤',
            title: t(filtered ? 'library.empty.filtered.title' : 'library.empty.title'),
            text: t(filtered ? 'library.empty.filtered.text' : 'library.empty.text'),
            action: actions
        }));
    }

    function renderCard(entry) {
        const book = entry.book;
        const total = Number(entry.totalPages) || 0;
        const current = Number(entry.currentPage) || 0;
        const percent = Math.max(0, Math.min(100, Number(entry.progressPercent) || 0));

        const facts = el('div', { class: 'book-facts' });
        if (book.publishYear) {
            facts.appendChild(el('span', { text: t('catalog.year', { year: book.publishYear }) }));
        }
        facts.appendChild(el('span', {
            text: total > 0 ? t('catalog.pages', { count: formatNumber(total) }) : t('library.card.unknownPages')
        }));

        const actions = el('div', { class: 'book-actions' },
            el('button', {
                type: 'button',
                class: 'btn btn-outline-primary',
                text: t('library.actions.progress'),
                disabled: total <= 0,
                title: total <= 0 ? t('library.card.unknownPages') : null,
                onclick: () => openProgress(entry)
            }),
            el('button', {
                type: 'button',
                class: 'btn btn-outline-secondary',
                text: t('library.actions.edit'),
                onclick: () => openEdit(entry)
            })
        );

        if (entry.status === 'READ') {
            actions.appendChild(el('a', {
                class: 'btn btn-outline-secondary',
                href: `${CONFIG.pages.reviews}?book=${entry.id}`,
                text: t('library.actions.review')
            }));
        }

        actions.appendChild(el('button', {
            type: 'button',
            class: 'btn btn-outline-secondary btn-danger-link',
            text: t('library.actions.delete'),
            onclick: () => removeEntry(entry)
        }));

        return el('article', { class: 'book-card' },
            el('div', { class: 'book-card-top' },
                coverElement(book),
                el('div', { class: 'book-card-info' },
                    el('h2', { class: 'book-title', text: book.title }),
                    el('p', { class: 'book-author', text: book.author || t('library.card.unknownAuthor') }),
                    facts,
                    statusBadge(entry.status)
                )
            ),
            el('div', { class: 'book-card-body' },
                el('div', { class: 'book-progress-label' },
                    el('span', { text: t('library.card.progress') }),
                    el('span', { text: formatPercent(percent) })
                ),
                el('div', {
                    class: 'progress',
                    role: 'progressbar',
                    'aria-valuemin': '0',
                    'aria-valuemax': '100',
                    'aria-valuenow': String(percent),
                    'aria-label': `${t('library.card.progress')}: ${formatPercent(percent)}`
                }, el('div', { class: 'progress-bar', style: { width: `${percent}%` } })),
                el('div', { class: 'book-progress-label' },
                    el('span', {
                        text: total > 0
                            ? t('library.card.pages', {
                                current: formatNumber(current),
                                total: formatNumber(total)
                            })
                            : t('library.card.unknownPages')
                    })
                ),
                renderDates(entry),
                actions
            )
        );
    }

    function renderDates(entry) {
        const dates = el('div', { class: 'book-dates' });
        if (entry.startedAt) {
            dates.appendChild(el('span', {
                text: t('library.card.started', { date: formatDate(entry.startedAt) })
            }));
        }
        if (entry.finishedAt) {
            dates.appendChild(el('span', {
                text: t('library.card.finished', { date: formatDate(entry.finishedAt) })
            }));
        }
        return dates;
    }

    function openProgress(entry) {
        currentEntry = entry;
        const form = qs('#progress-form');
        resetForm(form);
        form.dataset.id = String(entry.id);
        form.dataset.total = String(entry.totalPages || 0);
        qs('#progress-book').textContent = entry.book.title;
        qs('#progress-total').textContent = entry.totalPages > 0
            ? t('library.progress.max', { total: formatNumber(entry.totalPages) })
            : t('library.card.unknownPages');

        const input = qs('#progress-currentPage');
        input.value = String(entry.currentPage || 0);
        input.max = entry.totalPages > 0 ? String(entry.totalPages) : '';

        const dialog = qs('#progress-dialog');
        dialog.showModal();
        input.focus();
        input.select();
    }

    async function submitProgress(event) {
        event.preventDefault();
        const form = event.currentTarget;
        clearFormErrors(form);

        const value = valueOf(form, 'currentPage');
        const total = Number(form.dataset.total) || 0;
        const errors = {};

        integerField(errors, 'currentPage', value, { min: 0, required: true });
        if (!errors.currentPage && total > 0 && Number(value) > total) {
            errors.currentPage = t('library.progress.invalid');
        }
        if (total <= 0) {
            errors.currentPage = t('library.progress.noTotal');
        }

        if (Object.keys(errors).length > 0) {
            showLocalFieldErrors(form, errors);
            return;
        }

        const button = qs('#progress-submit');
        setBusy(button, true);
        try {
            await api(`/api/library/${form.dataset.id}/progress`, {
                method: 'PATCH',
                body: { currentPage: Number(value) }
            });
            qs('#progress-dialog').close();
            toast(t('library.toast.progress'), 'success');
            await load(state.page);
        } catch (error) {
            showFormErrors(form, error);
            notifyError(error);
        } finally {
            setBusy(button, false);
        }
    }

    function openEdit(entry) {
        currentEntry = entry;
        const form = qs('#edit-form');
        resetForm(form);
        form.dataset.id = String(entry.id);
        qs('#edit-book').textContent = entry.book.title;
        qs('#edit-status').value = entry.status;
        qs('#edit-totalPages').value = entry.totalPages > 0 ? String(entry.totalPages) : '';
        qs('#edit-startedAt').value = entry.startedAt || '';
        qs('#edit-dialog').showModal();
        qs('#edit-status').focus();
    }

    async function submitEdit(event) {
        event.preventDefault();
        const form = event.currentTarget;
        clearFormErrors(form);

        const totalPages = valueOf(form, 'totalPages');
        const startedAt = valueOf(form, 'startedAt');
        const errors = {};

        integerField(errors, 'totalPages', totalPages, { min: 1 });
        dateField(errors, 'startedAt', startedAt);

        if (Object.keys(errors).length > 0) {
            showLocalFieldErrors(form, errors);
            return;
        }

        const button = qs('#edit-submit');
        setBusy(button, true);
        try {
            await api(`/api/library/${form.dataset.id}`, {
                method: 'PUT',
                body: {
                    status: valueOf(form, 'status'),
                    totalPages: totalPages ? Number(totalPages) : null,
                    startedAt: startedAt || null
                }
            });
            qs('#edit-dialog').close();
            toast(t('library.toast.updated'), 'success');
            await load(state.page);
        } catch (error) {
            showFormErrors(form, error);
            notifyError(error);
        } finally {
            setBusy(button, false);
        }
    }

    function openManual() {
        const form = qs('#manual-form');
        resetForm(form);
        qs('#manual-dialog').showModal();
        qs('#manual-title-field').focus();
    }

    async function submitManual(event) {
        event.preventDefault();
        const form = event.currentTarget;
        clearFormErrors(form);

        const title = valueOf(form, 'title');
        const author = valueOf(form, 'author');
        const totalPages = valueOf(form, 'totalPages');
        const publishYear = valueOf(form, 'publishYear');
        const errors = {};

        requireField(errors, 'title', title);
        if (title.length > 500) {
            errors.title = t('validation.maxLength', { max: 500 });
        }
        if (author.length > 500) {
            errors.author = t('validation.maxLength', { max: 500 });
        }
        integerField(errors, 'totalPages', totalPages, { min: 1 });
        integerField(errors, 'publishYear', publishYear, { min: 1000, max: 2100 });

        if (Object.keys(errors).length > 0) {
            showLocalFieldErrors(form, errors);
            return;
        }

        const button = qs('#manual-submit');
        setBusy(button, true);
        try {
            await api('/api/library', {
                method: 'POST',
                body: {
                    title,
                    author: author || null,
                    totalPages: totalPages ? Number(totalPages) : null,
                    publishYear: publishYear ? Number(publishYear) : null
                }
            });
            qs('#manual-dialog').close();
            state.status = '';
            qs('#library-status').value = '';
            toast(t('library.toast.added'), 'success');
            await load(0);
        } catch (error) {
            if (error instanceof ApiError && error.status === 409) {
                showFormErrors(form, error);
                notifyError(error, 'errors.alreadyInLibrary');
            } else {
                showFormErrors(form, error);
                notifyError(error);
            }
        } finally {
            setBusy(button, false);
        }
    }

    async function removeEntry(entry) {
        const confirmed = await confirmDialog({
            title: t('library.delete.title'),
            message: t('library.delete.message', { title: entry.book.title }),
            confirmText: t('library.delete.confirm'),
            danger: true
        });
        if (!confirmed) {
            return;
        }

        try {
            await api(`/api/library/${entry.id}`, { method: 'DELETE' });
            toast(t('library.toast.deleted'), 'success');
            const lastItemOnPage = state.data && state.data.items.length === 1;
            const nextPage = lastItemOnPage && state.page > 0 ? state.page - 1 : state.page;
            await load(nextPage);
        } catch (error) {
            notifyError(error);
        }
    }
}
