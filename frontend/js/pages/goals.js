/*
 * Metas de lectura: filtro por año, alta, edición de objetivos, eliminación y
 * gráfico de avance frente al ritmo esperado.
 */
import { ApiError, api } from '../api.js';
import { renderGoalsChart } from '../charts.js';
import {
    formatMonth,
    formatNumber,
    formatPercent,
    monthLabel,
    onLanguageChange,
    t
} from '../i18n.js';
import { initLayout } from '../layout.js';
import { requireField, valueOf } from '../forms.js';
import {
    clear,
    clearFormErrors,
    confirmDialog,
    countLabel,
    describeError,
    el,
    emptyState,
    formatDecimal,
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
        year: new Date().getFullYear(),
        goals: null,
        editing: null
    };
    let loadToken = 0;

    populateYears();
    populateMonths();
    bindEvents();
    await load();

    onLanguageChange(() => {
        populateMonths();
        if (state.goals) {
            renderGoals();
            renderChart();
        }
    });

    function populateYears() {
        const select = qs('#goals-year');
        const current = new Date().getFullYear();
        clear(select);
        for (let year = current - 4; year <= current + 1; year += 1) {
            select.appendChild(el('option', { value: year, text: String(year) }));
        }
        select.value = String(state.year);
    }

    function setYearOption(year) {
        const select = qs('#goals-year');
        const value = String(year);
        if (!Array.from(select.options).some((option) => option.value === value)) {
            select.appendChild(el('option', { value, text: value }));
        }
        select.value = value;
    }

    function populateMonths() {
        const select = qs('#goal-month');
        const selected = select.value;
        clear(select);
        for (let month = 1; month <= 12; month += 1) {
            select.appendChild(el('option', {
                value: String(month),
                text: monthLabel(month)
            }));
        }
        if (selected) {
            select.value = selected;
        }
    }

    function bindEvents() {
        qs('#goals-year').addEventListener('change', (event) => {
            state.year = Number(event.target.value);
            void load();
        });

        qs('#open-goal').addEventListener('click', openNew);
        qs('#goal-form').addEventListener('submit', submitGoal);
        qs('#goal-period').addEventListener('change', syncPeriodFields);
    }

    async function load() {
        const token = ++loadToken;
        const alert = qs('#goals-alert');
        const loading = qs('#goals-loading');

        alert.hidden = true;
        if (!state.goals) {
            loading.hidden = false;
        }

        try {
            const goals = await api(`/api/goals?year=${state.year}`);
            if (token !== loadToken) {
                return;
            }
            state.goals = goals || [];
            renderGoals();
            renderChart();
        } catch (error) {
            if (token !== loadToken) {
                return;
            }
            clear(alert);
            alert.appendChild(el('span', { text: describeError(error, 'goals.error.load') }));
            alert.appendChild(el('button', {
                type: 'button',
                class: 'btn btn-sm btn-outline-danger ms-2',
                text: t('common.retry'),
                onclick: () => void load()
            }));
            alert.hidden = false;
        } finally {
            if (token === loadToken) {
                loading.hidden = true;
            }
        }
    }

    function renderGoals() {
        const container = clear(qs('#goals-list'));
        const goals = state.goals || [];

        qs('#goals-count').textContent = goals.length > 0
            ? countLabel('goals.count', goals.length)
            : '';

        if (goals.length === 0) {
            container.appendChild(emptyState({
                icon: '◎',
                title: t('goals.empty.title', { year: state.year }),
                text: t('goals.empty.text'),
                action: el('button', {
                    type: 'button',
                    class: 'btn btn-primary',
                    text: t('goals.new'),
                    onclick: openNew
                })
            }));
            return;
        }

        goals
            .slice()
            .sort((a, b) => compareGoals(a, b))
            .forEach((goal) => container.appendChild(renderCard(goal)));
    }

    function renderCard(goal) {
        const booksPercent = clampPercent(goal.booksPercent);
        const pagesPercent = clampPercent(goal.pagesPercent);

        const metrics = el('div', { class: 'goal-metrics' },
            el('span', { text: t('goals.card.expected', { percent: formatPercent(goal.expectedPercent) }) }),
            el('span', {
                text: goal.daysRemaining > 0
                    ? countLabel('goals.card.daysRemaining', goal.daysRemaining)
                    : t('goals.card.finishedPeriod')
            }),
            el('span', {
                text: t('goals.card.required', {
                    books: formatDecimal(goal.requiredBooksPerMonth, 1),
                    pages: formatDecimal(goal.requiredPagesPerDay, 0)
                })
            })
        );

        return el('article', { class: 'goal-card' },
            el('div', { class: 'goal-head' },
                el('div', {},
                    el('h2', { text: goalTitle(goal) }),
                    el('p', {
                        class: 'goal-meta',
                        text: `${countLabel('goals.card.targetBooks', goal.targetBooks)} · `
                            + countLabel('goals.card.targetPages', goal.targetPages)
                    })
                ),
                el('span', {
                    class: `pace-badge ${goal.onTrack ? 'on-track' : 'behind'}`,
                    text: t(goal.onTrack ? 'goals.card.onTrack' : 'goals.card.behind')
                })
            ),
            el('div', { class: 'goal-progress' },
                progressRow(
                    progressText('books', goal.booksRead, goal.targetBooks),
                    booksPercent
                ),
                progressRow(
                    progressText('pages', goal.pagesRead, goal.targetPages),
                    pagesPercent
                )
            ),
            metrics,
            el('div', { class: 'goal-actions' },
                el('button', {
                    type: 'button',
                    class: 'btn btn-sm btn-outline-primary',
                    text: t('goals.card.edit'),
                    onclick: () => openEdit(goal)
                }),
                el('button', {
                    type: 'button',
                    class: 'btn btn-sm btn-outline-secondary',
                    text: t('goals.card.delete'),
                    onclick: () => void removeGoal(goal)
                })
            )
        );
    }

    function progressRow(label, percent) {
        return el('div', { class: 'goal-progress-row' },
            el('div', { class: 'goal-progress-head' },
                el('strong', { text: label }),
                el('span', { text: formatPercent(percent) })
            ),
            el('div', {
                class: 'progress',
                role: 'progressbar',
                'aria-valuemin': '0',
                'aria-valuemax': '100',
                'aria-valuenow': String(percent),
                'aria-label': label
            }, el('div', { class: 'progress-bar', style: { width: `${percent}%` } }))
        );
    }

    function progressText(kind, read, target) {
        const key = `goals.card.${kind}Progress`;
        return target === 1
            ? t(`${key}.one`, { read: formatNumber(read) })
            : t(key, { read: formatNumber(read), target: formatNumber(target) });
    }

    function renderChart() {
        const panel = qs('#goals-chart-panel');
        const goals = (state.goals || []).filter((goal) => goal.targetBooks > 0);

        panel.hidden = goals.length === 0;
        if (goals.length === 0) {
            return;
        }

        const container = clear(qs('#goals-table'));
        const table = el('table', { class: 'data-table' },
            el('thead', {},
                el('tr', {},
                    el('th', { scope: 'col', text: t('goals.chart.tableGoal') }),
                    el('th', { scope: 'col', text: t('goals.chart.tableActual') }),
                    el('th', { scope: 'col', text: t('goals.chart.tableExpected') })
                )
            ),
            el('tbody', {},
                goals.map((goal) => el('tr', {},
                    el('th', { scope: 'row', text: goalTitle(goal) }),
                    el('td', { text: formatPercent(clampPercent(goal.booksPercent)) }),
                    el('td', { text: formatPercent(clampPercent(goal.expectedPercent)) })
                ))
            )
        );
        container.appendChild(table);

        void renderGoalsChart(qs('#goals-chart'), goals).catch((error) => {
            console.error(error);
            panel.hidden = true;
        });
    }

    function openNew() {
        state.editing = null;
        const form = qs('#goal-form');
        resetForm(form);

        qs('#goal-dialog-title').textContent = t('goals.form.titleNew');
        qs('#goal-period').value = 'ANNUAL';
        qs('#goal-period').disabled = false;
        qs('#goal-year').value = String(state.year);
        qs('#goal-year').disabled = false;
        qs('#goal-month').value = String(new Date().getMonth() + 1);
        qs('#goal-month').disabled = false;
        qs('#goal-targetBooks').value = '';
        qs('#goal-targetPages').value = '';
        qs('#goal-period-note').hidden = true;

        const submit = qs('#goal-submit');
        submit.textContent = t('goals.form.submitNew');
        submit.disabled = false;

        syncPeriodFields();
        qs('#goal-dialog').showModal();
        qs('#goal-period').focus();
    }

    function openEdit(goal) {
        state.editing = goal;
        const form = qs('#goal-form');
        resetForm(form);

        qs('#goal-dialog-title').textContent = t('goals.form.titleEdit');
        qs('#goal-period').value = goal.period;
        qs('#goal-period').disabled = true;
        qs('#goal-year').value = String(goal.year);
        qs('#goal-year').disabled = true;
        qs('#goal-month').value = goal.month ? String(goal.month) : '1';
        qs('#goal-month').disabled = true;
        qs('#goal-targetBooks').value = String(goal.targetBooks);
        qs('#goal-targetPages').value = String(goal.targetPages);
        qs('#goal-period-note').hidden = false;

        const submit = qs('#goal-submit');
        submit.textContent = t('goals.form.submitEdit');
        submit.disabled = false;

        syncPeriodFields();
        qs('#goal-dialog').showModal();
        qs('#goal-targetBooks').focus();
        qs('#goal-targetBooks').select();
    }

    function syncPeriodFields() {
        const monthly = qs('#goal-period').value === 'MONTHLY';
        const group = qs('#goal-month-group');
        group.hidden = !monthly;
        qs('#goal-month').disabled = !monthly || Boolean(state.editing);
    }

    async function submitGoal(event) {
        event.preventDefault();
        const form = event.currentTarget;
        clearFormErrors(form);

        const period = qs('#goal-period').value;
        const year = valueOf(form, 'year');
        const month = qs('#goal-month').value;
        const targetBooks = valueOf(form, 'targetBooks');
        const targetPages = valueOf(form, 'targetPages');
        const errors = {};

        requireField(errors, 'year', year);
        requireField(errors, 'targetBooks', targetBooks);
        requireField(errors, 'targetPages', targetPages);

        const yearNumber = Number(year);
        if (year && (!Number.isInteger(yearNumber) || yearNumber < 2000 || yearNumber > 2100)) {
            errors.year = t('errors.yearRange');
        }
        if (targetBooks && (!Number.isInteger(Number(targetBooks)) || Number(targetBooks) < 1)) {
            errors.targetBooks = t('validation.positive');
        }
        if (targetPages && (!Number.isInteger(Number(targetPages)) || Number(targetPages) < 1)) {
            errors.targetPages = t('validation.positive');
        }
        if (period === 'MONTHLY' && !month) {
            errors.month = t('validation.required');
        }

        if (Object.keys(errors).length > 0) {
            showLocalFieldErrors(form, errors);
            return;
        }

        const button = qs('#goal-submit');
        setBusy(button, true);
        try {
            if (state.editing) {
                await api(`/api/goals/${state.editing.id}`, {
                    method: 'PUT',
                    body: {
                        targetBooks: Number(targetBooks),
                        targetPages: Number(targetPages)
                    }
                });
                toast(t('goals.toast.updated'), 'success');
            } else {
                await api('/api/goals', {
                    method: 'POST',
                    body: {
                        period,
                        year: yearNumber,
                        month: period === 'MONTHLY' ? Number(month) : null,
                        targetBooks: Number(targetBooks),
                        targetPages: Number(targetPages)
                    }
                });
                toast(t('goals.toast.created'), 'success');
            }

            qs('#goal-dialog').close();
            state.year = yearNumber;
            setYearOption(yearNumber);
            await load();
        } catch (error) {
            if (error instanceof ApiError && error.status === 409) {
                notifyError(error, 'goals.error.exists');
            } else {
                showFormErrors(form, error);
                notifyError(error);
            }
        } finally {
            setBusy(button, false);
        }
    }

    async function removeGoal(goal) {
        const confirmed = await confirmDialog({
            title: t('goals.delete.title'),
            message: t('goals.delete.message', { period: goalTitle(goal) }),
            confirmText: t('common.delete'),
            danger: true
        });
        if (!confirmed) {
            return;
        }

        try {
            await api(`/api/goals/${goal.id}`, { method: 'DELETE' });
            toast(t('goals.toast.deleted'), 'success');
            await load();
        } catch (error) {
            notifyError(error);
        }
    }

    function goalTitle(goal) {
        return goal.period === 'MONTHLY'
            ? t('goals.card.monthly', { month: formatMonth(goal.month), year: goal.year })
            : t('goals.card.annual', { year: goal.year });
    }

    function compareGoals(a, b) {
        if (a.period !== b.period) {
            return a.period === 'ANNUAL' ? -1 : 1;
        }
        return (a.month || 0) - (b.month || 0);
    }

    function clampPercent(value) {
        return Math.max(0, Math.min(100, Number(value) || 0));
    }
}
