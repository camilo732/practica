/*
 * Catálogo: búsqueda en Open Library a través de la API, aviso de caché y
 * alta directa en la biblioteca personal.
 */
import { ApiError, api } from '../api.js';
import { formatNumber, onLanguageChange, t } from '../i18n.js';
import { initLayout } from '../layout.js';
import { integerField, requireField, valueOf } from '../forms.js';
import {
    clear,
    clearFormErrors,
    countLabel,
    coverElement,
    describeError,
    el,
    emptyState,
    notifyError,
    qs,
    resetForm,
    setBusy,
    showFormErrors,
    showLocalFieldErrors,
    toast
} from '../ui.js';

const layout = await initLayout();

if (layout) {
    const state = {
        searched: false,
        query: '',
        results: [],
        fromCache: false,
        added: new Set()
    };
    let searchToken = 0;

    bindEvents();
    renderInitial();

    onLanguageChange(() => {
        renderResults();
        const summary = qs('#catalog-summary');
        if (summary && state.searched && state.results.length > 0) {
            summary.textContent = countLabel('catalog.results', state.results.length, {
                query: state.query
            });
        }
    });

    function bindEvents() {
        qs('#catalog-form').addEventListener('submit', search);
        qs('#open-manual').addEventListener('click', openManual);
        qs('#manual-form').addEventListener('submit', submitManual);
    }

    async function search(event) {
        event.preventDefault();
        const form = event.currentTarget;
        clearFormErrors(form);

        const query = valueOf(form, 'q');
        const limit = Number(valueOf(form, 'limit')) || 20;
        const errors = {};
        requireField(errors, 'q', query);
        if (!errors.q && query.length < 2) {
            errors.q = t('catalog.hint');
        }

        if (Object.keys(errors).length > 0) {
            showLocalFieldErrors(form, errors);
            return;
        }

        const button = qs('#catalog-submit');
        const alert = qs('#catalog-alert');
        const notice = qs('#catalog-notice');
        const loading = qs('#catalog-loading');
        const token = ++searchToken;

        alert.hidden = true;
        notice.hidden = true;
        loading.hidden = false;
        clear(qs('#catalog-results'));
        qs('#catalog-summary').textContent = '';
        setBusy(button, true, 'catalog.searching');

        try {
            const data = await api(`/api/books/search?q=${encodeURIComponent(query)}&limit=${limit}`);
            if (token !== searchToken) {
                return;
            }

            state.searched = true;
            state.query = query;
            state.results = data.items || [];
            state.fromCache = Boolean(data.fromCache);
            renderResults();
        } catch (error) {
            if (token !== searchToken) {
                return;
            }

            state.searched = true;
            state.query = query;
            state.results = [];
            state.fromCache = false;
            renderResults();

            if (error instanceof ApiError && error.status === 503) {
                notice.textContent = error.retryAfter
                    ? t('catalog.error.offline', { seconds: error.retryAfter })
                    : t('errors.serviceUnavailable');
                notice.hidden = false;
            } else {
                alert.textContent = describeError(error, 'catalog.error.search');
                alert.hidden = false;
                notifyError(error, 'catalog.error.search');
            }
        } finally {
            if (token === searchToken) {
                loading.hidden = true;
                setBusy(button, false);
            }
        }
    }

    function renderInitial() {
        const results = clear(qs('#catalog-results'));
        results.appendChild(emptyState({
            icon: '⌕',
            title: t('catalog.initial.title'),
            text: t('catalog.initial.text')
        }));
    }

    function renderResults() {
        const container = clear(qs('#catalog-results'));
        const summary = qs('#catalog-summary');
        const notice = qs('#catalog-notice');

        if (!state.searched) {
            renderInitial();
            return;
        }

        if (state.fromCache && state.results.length > 0) {
            notice.textContent = t('catalog.cacheNotice');
            notice.hidden = false;
        }

        if (state.results.length === 0) {
            summary.textContent = '';
            const action = el('button', {
                type: 'button',
                class: 'btn btn-outline-primary',
                text: t('catalog.manual'),
                onclick: openManual
            });
            container.appendChild(emptyState({
                icon: '⌕',
                title: t('catalog.empty.title'),
                text: t('catalog.empty.text'),
                action
            }));
            return;
        }

        summary.textContent = countLabel('catalog.results', state.results.length, {
            query: state.query
        });

        state.results.forEach((book) => {
            container.appendChild(renderCard(book));
        });
    }

    function renderCard(book) {
        const info = el('div', { class: 'book-card-info' },
            el('h2', { class: 'book-title', text: book.title || t('common.unknown') }),
            el('p', {
                class: 'book-author',
                text: book.author ? t('catalog.by', { author: book.author }) : t('library.card.unknownAuthor')
            })
        );

        const facts = el('div', { class: 'book-facts' });
        if (book.totalPages) {
            facts.appendChild(el('span', { text: t('catalog.pages', { count: formatNumber(book.totalPages) }) }));
        }
        if (book.publishYear) {
            facts.appendChild(el('span', { text: t('catalog.year', { year: book.publishYear }) }));
        }
        if (book.isbn) {
            facts.appendChild(el('span', { text: t('catalog.isbn', { isbn: book.isbn }) }));
        }
        info.appendChild(facts);

        const body = el('div', { class: 'catalog-card-body' });

        if (book.description) {
            const description = el('p', {
                class: 'catalog-description clamp-3',
                text: book.description,
                hidden: true
            });
            const toggle = el('button', {
                type: 'button',
                class: 'btn btn-link btn-sm p-0 align-self-start',
                'aria-expanded': 'false',
                text: t('catalog.showDescription')
            });
            toggle.addEventListener('click', () => {
                const open = description.hidden;
                description.hidden = !open;
                description.classList.toggle('clamp-3', !open);
                toggle.setAttribute('aria-expanded', String(open));
                toggle.textContent = t(open ? 'catalog.hideDescription' : 'catalog.showDescription');
            });
            body.append(toggle, description);
        }

        const addButton = el('button', {
            type: 'button',
            class: 'btn btn-primary',
            text: t('catalog.add')
        });
        addButton.addEventListener('click', () => void addBook(book, addButton));
        body.appendChild(addButton);

        if (state.added.has(book.id)) {
            markAdded(addButton);
        }

        return el('article', { class: 'catalog-card' },
            el('div', { class: 'catalog-card-top' },
                coverElement(book),
                info
            ),
            body
        );
    }

    async function addBook(book, button) {
        if (state.added.has(book.id)) {
            return;
        }

        setBusy(button, true, 'catalog.adding');
        try {
            await api('/api/library', {
                method: 'POST',
                body: { bookId: book.id }
            });
            state.added.add(book.id);
            toast(t('library.toast.added'), 'success');
        } catch (error) {
            if (error instanceof ApiError && error.status === 409) {
                state.added.add(book.id);
                toast(t('catalog.already'), 'warning');
            } else {
                notifyError(error);
            }
        } finally {
            setBusy(button, false);
            if (state.added.has(book.id)) {
                markAdded(button);
            }
        }
    }

    function markAdded(button) {
        button.disabled = true;
        button.textContent = t('catalog.added');
        button.classList.remove('btn-primary');
        button.classList.add('btn-outline-primary');
    }

    function openManual() {
        const form = qs('#manual-form');
        resetForm(form);
        const dialog = qs('#manual-dialog');
        dialog.showModal();
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
            toast(t('library.toast.added'), 'success');
        } catch (error) {
            if (error instanceof ApiError && error.status === 409) {
                notifyError(error, 'errors.alreadyInLibrary');
            } else {
                showFormErrors(form, error);
                notifyError(error);
            }
        } finally {
            setBusy(button, false);
        }
    }
}
