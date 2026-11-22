/*
 * Panel: métricas de la biblioteca, actividad mensual, autorías, metas y
 * últimas incorporaciones. Los datos se recargan al cambiar de año.
 */
import { api, download } from '../api.js';
import { renderMonthlyChart } from '../charts.js';
import {
    formatMonth,
    formatNumber,
    formatPercent,
    onLanguageChange,
    t
} from '../i18n.js';
import { initLayout } from '../layout.js';
import {
    clear,
    countLabel,
    coverElement,
    describeError,
    el,
    formatDecimal,
    notifyError,
    qs,
    setBusy,
    starsElement,
    statusBadge
} from '../ui.js';

const layout = await initLayout();

if (layout) {
    const state = {
        year: new Date().getFullYear(),
        data: null
    };
    let renderToken = 0;
    let loading = false;

    populateYears();
    bindEvents();
    await load();

    onLanguageChange(() => {
        if (state.data) {
            void render();
        }
    });

    function populateYears() {
        const select = qs('#dashboard-year');
        if (!select) {
            return;
        }

        const current = new Date().getFullYear();
        clear(select);
        for (let year = current - 4; year <= current + 1; year += 1) {
            select.appendChild(el('option', { value: year, text: String(year) }));
        }
        select.value = String(state.year);
    }

    function bindEvents() {
        qs('#dashboard-year').addEventListener('change', (event) => {
            state.year = Number(event.target.value);
            void load();
        });

        qs('#download-pdf').addEventListener('click', async (event) => {
            const button = event.currentTarget;
            setBusy(button, true);
            try {
                await download(
                    `/api/analytics/report.pdf?year=${encodeURIComponent(state.year)}&lang=${encodeURIComponent(document.documentElement.lang)}`,
                    `librolog-informe-${state.year}.pdf`
                );
            } catch (error) {
                notifyError(error, 'data.export.error');
            } finally {
                setBusy(button, false);
            }
        });
    }

    async function load() {
        if (loading) {
            return;
        }
        loading = true;

        const alert = qs('#dashboard-alert');
        const loadingBlock = qs('#dashboard-loading');
        const content = qs('#dashboard-content');

        alert.hidden = true;
        loadingBlock.hidden = false;
        content.hidden = true;

        try {
            const [stats, summary, goals, recent] = await Promise.all([
                api('/api/library/stats'),
                api(`/api/analytics/summary?year=${state.year}`),
                api(`/api/goals?year=${state.year}`),
                api('/api/library?page=0&size=5')
            ]);

            state.data = { stats, summary, goals, recent };
            await render();
        } catch (error) {
            console.error('No se pudo cargar el panel', error);
            state.data = null;
            clear(alert);
            alert.appendChild(el('span', {
                text: describeError(error, 'dashboard.loadError')
            }));
            alert.appendChild(el('button', {
                type: 'button',
                class: 'btn btn-sm btn-outline-danger ms-2',
                text: t('common.retry'),
                onclick: () => void load()
            }));
            alert.hidden = false;
        } finally {
            loading = false;
            loadingBlock.hidden = true;
        }
    }

    async function render() {
        if (!state.data) {
            return;
        }

        const token = ++renderToken;
        const { stats, summary, goals, recent } = state.data;
        const content = qs('#dashboard-content');
        content.hidden = false;

        renderStats(stats, summary);
        qs('#monthly-title').textContent = t('dashboard.chart.title', { year: state.year });
        renderMonthlyTable(summary.monthlyActivity);
        renderAuthors(summary, state.year);
        renderGenres(summary, state.year);
        renderGoals(goals, state.year);
        renderRecent(recent);
        renderRatings(summary);

        const hasActivity = (summary.monthlyActivity || [])
            .some((month) => month.pagesRead > 0 || month.booksFinished > 0);
        const chartWrap = qs('#monthly-chart').parentElement;
        const empty = qs('#monthly-empty');

        chartWrap.hidden = !hasActivity;
        empty.hidden = hasActivity;

        if (hasActivity) {
            try {
                await renderMonthlyChart(qs('#monthly-chart'), summary.monthlyActivity);
                if (token !== renderToken) {
                    return;
                }
                qs('#monthly-chart').setAttribute('aria-label',
                    `${t('dashboard.chart.title', { year: state.year })}. ${t('dashboard.chart.pages')} / ${t('dashboard.chart.books')}`);
            } catch (error) {
                console.error(error);
                chartWrap.hidden = true;
                empty.hidden = false;
                const details = qs('#monthly-table')?.closest('details');
                if (details) {
                    details.open = true;
                }
            }
        }
    }

    function renderStats(stats, summary) {
        const grid = clear(qs('#stat-grid'));
        const cards = [
            {
                label: t('dashboard.stat.total'),
                value: formatNumber(stats.total)
            },
            {
                label: t('dashboard.stat.reading'),
                value: formatNumber(stats.reading)
            },
            {
                label: t('dashboard.stat.read'),
                value: formatNumber(stats.read)
            },
            {
                label: t('dashboard.stat.finishedYear', { year: state.year }),
                value: formatNumber(summary.finishedThisYear)
            },
            {
                label: t('dashboard.stat.pagesYear', { year: state.year }),
                value: formatNumber(summary.pagesReadThisYear)
            },
            {
                label: t('dashboard.stat.avgPages', { year: state.year }),
                value: summary.averagePagesPerBook > 0 ? formatDecimal(summary.averagePagesPerBook, 1) : '—'
            },
            {
                label: t('dashboard.stat.average'),
                value: summary.reviewsCount > 0 ? formatDecimal(summary.averageRating, 1) : '—',
                hint: countLabel('dashboard.ratings.count', summary.reviewsCount)
            }
        ];

        cards.forEach((card) => {
            grid.appendChild(el('article', { class: 'stat-card' },
                el('p', { class: 'stat-label', text: card.label }),
                el('p', { class: 'stat-value', text: card.value }),
                card.hint ? el('p', { class: 'stat-hint', text: card.hint }) : null
            ));
        });
    }

    function renderMonthlyTable(months) {
        const container = clear(qs('#monthly-table'));
        const table = el('table', { class: 'data-table' },
            el('thead', {},
                el('tr', {},
                    el('th', { scope: 'col', text: t('dashboard.chart.tableMonth') }),
                    el('th', { scope: 'col', text: t('dashboard.chart.tablePages') }),
                    el('th', { scope: 'col', text: t('dashboard.chart.tableBooks') })
                )),
            el('tbody', {},
                (months || []).map((month) => el('tr', {},
                    el('th', { scope: 'row', text: formatMonth(month.month) }),
                    el('td', { text: formatNumber(month.pagesRead) }),
                    el('td', { text: formatNumber(month.booksFinished) })
                ))
            )
        );
        container.appendChild(table);
    }

    function renderAuthors(summary, year) {
        const container = clear(qs('#authors-list'));
        qs('#authors-title').textContent = t('dashboard.authors.title', { year });
        const authors = summary.topAuthors || [];

        if (authors.length === 0) {
            container.appendChild(el('p', { class: 'text-muted mb-0', text: t('dashboard.authors.empty') }));
            return;
        }

        const max = Math.max(...authors.map((author) => author.books), 1);
        authors.forEach((author) => {
            container.appendChild(el('div', { class: 'author-row' },
                el('div', { class: 'author-row-head' },
                    el('span', { text: author.author || t('common.unknown') }),
                    el('span', { text: formatNumber(author.books) })
                ),
                el('div', { class: 'mini-bar', 'aria-hidden': 'true' },
                    el('span', { style: { width: `${Math.round((author.books / max) * 100)}%` } })
                )
            ));
        });
    }

    function renderGenres(summary, year) {
        const container = clear(qs('#genres-list'));
        qs('#genres-title').textContent = t('dashboard.genres.title', { year });
        const genres = summary.topGenres || [];

        if (genres.length === 0) {
            container.appendChild(el('p', { class: 'text-muted mb-0', text: t('dashboard.genres.empty') }));
            return;
        }

        const max = Math.max(...genres.map((genre) => genre.books), 1);
        genres.forEach((genre) => {
            container.appendChild(el('div', { class: 'author-row' },
                el('div', { class: 'author-row-head' },
                    el('span', { text: genre.genre }),
                    el('span', { text: formatNumber(genre.books) })
                ),
                el('div', { class: 'mini-bar', 'aria-hidden': 'true' },
                    el('span', { style: { width: `${Math.round((genre.books / max) * 100)}%` } })
                )
            ));
        });
    }

    function renderGoals(goals, year) {
        const container = clear(qs('#goals-list'));
        qs('#goals-title').textContent = t('dashboard.goals.title', { year });

        if (!goals || goals.length === 0) {
            container.appendChild(el('p', { class: 'text-muted mb-0', text: t('dashboard.goals.empty') }));
            return;
        }

        goals.slice(0, 3).forEach((goal) => {
            const percent = Math.max(0, Math.min(100, goal.booksPercent));
            container.appendChild(el('div', { class: 'goal-progress-row' },
                el('div', { class: 'goal-progress-head' },
                    el('strong', { text: goalTitle(goal) }),
                    el('span', { text: `${formatPercent(percent)}` })
                ),
                el('div', {
                    class: 'progress',
                    role: 'progressbar',
                    'aria-valuemin': '0',
                    'aria-valuemax': '100',
                    'aria-valuenow': String(percent),
                    'aria-label': goalTitle(goal)
                }, el('div', { class: 'progress-bar', style: { width: `${percent}%` } })),
                el('div', { class: 'goal-progress-head' },
                    el('span', {
                        text: goal.targetBooks === 1
                            ? t('goals.card.booksProgress.one', { read: formatNumber(goal.booksRead) })
                            : t('goals.card.booksProgress', {
                                read: formatNumber(goal.booksRead),
                                target: formatNumber(goal.targetBooks)
                            })
                    }),
                    el('span', {
                        class: `pace-badge ${goal.onTrack ? 'on-track' : 'behind'}`,
                        text: t(goal.onTrack ? 'goals.card.onTrack' : 'goals.card.behind')
                    })
                )
            ));
        });
    }

    function renderRecent(recent) {
        const container = clear(qs('#recent-list'));
        const items = recent && recent.items ? recent.items : [];

        if (items.length === 0) {
            container.appendChild(el('p', { class: 'text-muted mb-0', text: t('dashboard.recent.empty') }));
            return;
        }

        items.forEach((entry) => {
            container.appendChild(el('div', { class: 'recent-item' },
                coverElement(entry.book, 'thumb'),
                el('div', { class: 'recent-item-info' },
                    el('p', { class: 'recent-item-title', text: entry.book.title }),
                    el('p', {
                        class: 'recent-item-meta',
                        text: entry.book.author || t('library.card.unknownAuthor')
                    })
                ),
                statusBadge(entry.status)
            ));
        });
    }

    function renderRatings(summary) {
        const container = clear(qs('#ratings-list'));
        const empty = qs('#ratings-empty');
        const distribution = summary.ratingDistribution || [];
        const total = distribution.reduce((sum, item) => sum + item.count, 0);

        empty.hidden = total > 0;
        container.hidden = total === 0;

        if (total === 0) {
            return;
        }

        const max = Math.max(...distribution.map((item) => item.count), 1);
        [...distribution].sort((a, b) => b.rating - a.rating).forEach((item) => {
            container.appendChild(el('div', { class: 'rating-row' },
                el('div', { class: 'rating-row-head' },
                    starsElement(item.rating),
                    el('span', { text: formatNumber(item.count) })
                ),
                el('div', { class: 'mini-bar', 'aria-hidden': 'true' },
                    el('span', {
                        style: {
                            width: `${Math.round((item.count / max) * 100)}%`,
                            background: 'var(--ll-accent)'
                        }
                    })
                )
            ));
        });
    }

    function goalTitle(goal) {
        return goal.period === 'MONTHLY'
            ? t('goals.card.monthly', { month: formatMonth(goal.month), year: goal.year })
            : t('goals.card.annual', { year: goal.year });
    }
}
