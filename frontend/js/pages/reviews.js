/*
 * Reseñas: listado paginado, distribución de calificaciones, alta y edición
 * mediante el endpoint de upsert y eliminación con confirmación.
 */
import { ApiError, api } from '../api.js';
import { renderRatingChart } from '../charts.js';
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
    starsElement,
    toast
} from '../ui.js';

const layout = await initLayout();

if (layout) {
    const state = {
        page: 0,
        size: CONFIG.reviewsPageSize,
        data: null,
        readBooks: [],
        summary: null,
        editing: null
    };
    let loadToken = 0;

    bindEvents();
    await Promise.all([load(), loadReadBooks(), loadSummary()]);

    onLanguageChange(() => {
        if (state.data) {
            renderList();
        }
        populateBookSelect();
        renderRatings();
    });

    const presetBook = new URLSearchParams(window.location.search).get('book');
    if (presetBook) {
        void openForBook(presetBook);
    }

    function bindEvents() {
        qs('#open-review').addEventListener('click', () => void openNew());
        qs('#review-form').addEventListener('submit', submitReview);
        qs('#review-book').addEventListener('change', prefillFromBook);
        qs('#review-content').addEventListener('input', updateCounter);
    }

    async function load(page = state.page) {
        const token = ++loadToken;
        state.page = Math.max(0, page);

        const alert = qs('#reviews-alert');
        const loading = qs('#reviews-loading');

        alert.hidden = true;
        if (!state.data) {
            loading.hidden = false;
        }

        try {
            const data = await api(`/api/reviews?page=${state.page}&size=${state.size}`);
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
            alert.appendChild(el('span', { text: describeError(error, 'reviews.error.load') }));
            alert.appendChild(el('button', {
                type: 'button',
                class: 'btn btn-sm btn-outline-danger ms-2',
                text: t('common.retry'),
                onclick: () => void load(state.page)
            }));
            alert.hidden = false;
        } finally {
            if (token === loadToken) {
                loading.hidden = true;
            }
        }
    }

    async function loadReadBooks() {
        try {
            const data = await api('/api/library?status=READ&page=0&size=100');
            state.readBooks = data.items || [];
            populateBookSelect();
        } catch (error) {
            console.error('No se pudieron cargar los libros terminados', error);
        }
    }

    async function loadSummary() {
        try {
            state.summary = await api(`/api/analytics/summary?year=${new Date().getFullYear()}`);
        } catch (error) {
            state.summary = null;
        }
        renderRatings();
    }

    function renderList() {
        const container = clear(qs('#reviews-list'));
        const data = state.data;

        if (!data) {
            return;
        }

        qs('#reviews-count').textContent = countLabel('reviews.count', data.totalElements);

        if (data.items.length === 0) {
            container.appendChild(emptyState({
                icon: '★',
                title: t('reviews.empty.title'),
                text: t('reviews.empty.text'),
                action: el('button', {
                    type: 'button',
                    class: 'btn btn-primary',
                    text: t('reviews.new'),
                    onclick: () => void openNew()
                })
            }));
            return;
        }

        data.items.forEach((review) => {
            container.appendChild(renderCard(review));
        });

        const nav = pagination({
            page: data.page,
            totalPages: data.totalPages,
            onChange: (page) => void load(page)
        });
        if (nav) {
            container.appendChild(nav);
        }
    }

    function renderCard(review) {
        const book = review.book || {};

        const subratings = el('div', { class: 'subratings' });
        [
            ['reviews.card.pace', review.paceRating],
            ['reviews.card.characters', review.charactersRating],
            ['reviews.card.usefulness', review.usefulnessRating]
        ].forEach(([key, value]) => {
            if (value) {
                subratings.appendChild(el('span', { class: 'subrating' },
                    el('span', { text: `${t(key)}:` }),
                    starsElement(value, t('reviews.subratingAria', {
                        label: t(key),
                        rating: value
                    }))
                ));
            }
        });

        const head = el('div', { class: 'review-head' },
            el('div', { class: 'review-book' },
                coverElement(book, 'thumb'),
                el('div', {},
                    el('h2', { text: book.title || t('common.unknown') }),
                    el('p', { text: book.author || t('library.card.unknownAuthor') })
                )
            ),
            el('div', { class: 'rating-summary' },
                starsElement(review.rating),
                el('span', {
                    class: 'text-muted small',
                    text: formatDateTime(review.updatedAt)
                })
            )
        );

        const body = el('div', { class: 'review-body' });
        if (subratings.childElementCount > 0) {
            body.appendChild(subratings);
        }
        body.appendChild(el('p', {
            class: 'review-text',
            text: review.content || t('reviews.card.noContent')
        }));

        const actions = el('div', { class: 'review-actions' },
            el('button', {
                type: 'button',
                class: 'btn btn-sm btn-outline-secondary',
                text: t('common.edit'),
                onclick: () => openEdit(review)
            }),
            el('button', {
                type: 'button',
                class: 'btn btn-sm btn-outline-secondary',
                text: t('common.delete'),
                onclick: () => void removeReview(review)
            })
        );

        return el('article', { class: 'review-card' }, head, body, actions);
    }

    function renderRatings() {
        const container = clear(qs('#ratings-table'));
        const empty = qs('#ratings-empty');
        const canvas = qs('#ratings-chart');
        const distribution = (state.summary && state.summary.ratingDistribution) || [];
        const total = distribution.reduce((sum, item) => sum + item.count, 0);

        empty.hidden = total > 0;
        canvas.parentElement.hidden = total === 0;

        if (total === 0) {
            return;
        }

        const table = el('table', { class: 'data-table' },
            el('thead', {},
                el('tr', {},
                    el('th', { scope: 'col', text: t('common.rating') }),
                    el('th', { scope: 'col', text: t('dashboard.stat.reviews') })
                )
            ),
            el('tbody', {},
                [...distribution].sort((a, b) => b.rating - a.rating).map((item) => el('tr', {},
                    el('th', { scope: 'row', text: `${item.rating} ★` }),
                    el('td', { text: formatNumber(item.count) })
                ))
            )
        );
        container.appendChild(table);

        void renderRatingChart(canvas, distribution).catch((error) => {
            console.error(error);
            container.closest('details').open = true;
        });
    }

    function populateBookSelect() {
        const select = qs('#review-book');
        if (!select) {
            return;
        }

        const selected = select.value;
        clear(select);
        select.appendChild(el('option', {
            value: '',
            text: t('reviews.selectBook')
        }));

        state.readBooks.forEach((entry) => {
            select.appendChild(el('option', {
                value: String(entry.id),
                text: entry.book.author
                    ? `${entry.book.title} — ${entry.book.author}`
                    : entry.book.title
            }));
        });

        if (selected && state.readBooks.some((entry) => String(entry.id) === selected)) {
            select.value = selected;
        }
    }

    async function openNew() {
        state.editing = null;
        const form = qs('#review-form');
        resetForm(form);
        updateCounter();

        const select = qs('#review-book');
        select.disabled = false;

        const noBooks = qs('#review-no-books');
        const hasBooks = state.readBooks.length > 0;
        noBooks.hidden = hasBooks;
        form.hidden = !hasBooks;

        qs('#review-dialog-title').textContent = t('reviews.form.titleNew');
        const submit = qs('#review-submit');
        submit.textContent = t('reviews.form.submitNew');
        submit.disabled = !hasBooks;

        const dialog = qs('#review-dialog');
        if (!dialog.open) {
            dialog.showModal();
        }
        if (hasBooks) {
            select.focus();
        }
    }

    async function openForBook(userBookId) {
        await openNew();
        const select = qs('#review-book');
        const exists = state.readBooks.some((entry) => String(entry.id) === String(userBookId));

        if (!exists) {
            qs('#review-dialog').close();
            toast(t('reviews.error.needRead'), 'warning');
            return;
        }

        select.value = String(userBookId);
        await prefillFromBook();
    }

    function setFormMode(editing) {
        qs('#review-dialog-title').textContent = t(editing ? 'reviews.form.titleEdit' : 'reviews.form.titleNew');
        const submit = qs('#review-submit');
        submit.textContent = t(editing ? 'reviews.form.submitEdit' : 'reviews.form.submitNew');
        submit.disabled = false;
    }

    function openEdit(review) {
        state.editing = review;
        const form = qs('#review-form');
        resetForm(form);
        qs('#review-no-books').hidden = true;
        form.hidden = false;

        const select = qs('#review-book');
        populateBookSelect();
        select.value = String(review.userBookId);
        select.disabled = true;

        qs('#review-rating').value = String(review.rating);
        qs('#review-pace').value = review.paceRating ? String(review.paceRating) : '';
        qs('#review-characters').value = review.charactersRating ? String(review.charactersRating) : '';
        qs('#review-usefulness').value = review.usefulnessRating ? String(review.usefulnessRating) : '';
        qs('#review-content').value = review.content || '';
        updateCounter();

        qs('#review-dialog-title').textContent = t('reviews.form.titleEdit');
        const submit = qs('#review-submit');
        submit.textContent = t('reviews.form.submitEdit');
        submit.disabled = false;

        qs('#review-dialog').showModal();
        qs('#review-rating').focus();
    }

    async function prefillFromBook() {
        if (state.editing) {
            return;
        }

        const userBookId = qs('#review-book').value;
        if (!userBookId) {
            return;
        }

        const form = qs('#review-form');
        clearFormErrors(form);
        qs('#review-rating').value = '';
        qs('#review-pace').value = '';
        qs('#review-characters').value = '';
        qs('#review-usefulness').value = '';
        qs('#review-content').value = '';
        updateCounter();

        try {
            const review = await api(`/api/library/${userBookId}/review`);
            qs('#review-rating').value = String(review.rating);
            qs('#review-pace').value = review.paceRating ? String(review.paceRating) : '';
            qs('#review-characters').value = review.charactersRating ? String(review.charactersRating) : '';
            qs('#review-usefulness').value = review.usefulnessRating ? String(review.usefulnessRating) : '';
            qs('#review-content').value = review.content || '';
            updateCounter();
            setFormMode(true);
        } catch (error) {
            // 404 significa que ese libro aún no tiene reseña: es el caso normal.
            if (!(error instanceof ApiError) || error.status !== 404) {
                notifyError(error);
            } else {
                setFormMode(false);
            }
        }
    }

    async function submitReview(event) {
        event.preventDefault();
        const form = event.currentTarget;
        clearFormErrors(form);

        const userBookId = qs('#review-book').value;
        const rating = qs('#review-rating').value;
        const errors = {};

        if (!userBookId) {
            errors.userBookId = t('validation.select');
        }
        if (!rating) {
            errors.rating = t('validation.required');
        }

        if (Object.keys(errors).length > 0) {
            showLocalFieldErrors(form, errors);
            return;
        }

        const button = qs('#review-submit');
        setBusy(button, true);
        try {
            await api(`/api/library/${userBookId}/review`, {
                method: 'PUT',
                body: {
                    rating: Number(rating),
                    paceRating: numberOrNull(qs('#review-pace').value),
                    charactersRating: numberOrNull(qs('#review-characters').value),
                    usefulnessRating: numberOrNull(qs('#review-usefulness').value),
                    content: qs('#review-content').value.trim() || null
                }
            });
            qs('#review-dialog').close();
            toast(t('reviews.toast.saved'), 'success');
            await Promise.all([load(state.page), loadSummary()]);
        } catch (error) {
            showFormErrors(form, error);
            if (error instanceof ApiError && error.status === 400) {
                notifyError(error, 'errors.reviewNeedsRead');
            } else if (error instanceof ApiError && error.status === 409) {
                notifyError(error);
            } else {
                notifyError(error);
            }
        } finally {
            setBusy(button, false);
        }
    }

    async function removeReview(review) {
        const confirmed = await confirmDialog({
            title: t('reviews.delete.title'),
            message: t('reviews.delete.message', { title: review.book.title }),
            confirmText: t('common.delete'),
            danger: true
        });
        if (!confirmed) {
            return;
        }

        try {
            await api(`/api/library/${review.userBookId}/review`, { method: 'DELETE' });
            toast(t('reviews.toast.deleted'), 'success');
            const lastOnPage = state.data && state.data.items.length === 1;
            const page = lastOnPage && state.page > 0 ? state.page - 1 : state.page;
            await Promise.all([load(page), loadSummary()]);
        } catch (error) {
            notifyError(error);
        }
    }

    function updateCounter() {
        const content = qs('#review-content');
        const counter = qs('#review-counter');
        if (content && counter) {
            counter.textContent = t('reviews.form.counter', {
                count: formatNumber(content.value.length)
            });
        }
    }

    function numberOrNull(value) {
        return value === '' || value === null || value === undefined ? null : Number(value);
    }
}
