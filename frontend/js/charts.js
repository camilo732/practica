/*
 * Gráficos con Chart.js. Se cargan solo en las páginas que los necesitan y se
 * recrean al cambiar de idioma para que las etiquetas queden traducidas.
 */
import { formatMonth, monthLabel, t } from './i18n.js';

const charts = new Map();

/**
 * Espera a que Chart.js esté disponible en la página.
 *
 * @returns {Promise<any>} constructor de Chart.js
 */
export function ensureChart() {
    if (window.Chart) {
        return Promise.resolve(window.Chart);
    }

    return new Promise((resolve, reject) => {
        const started = Date.now();
        const timer = setInterval(() => {
            if (window.Chart) {
                clearInterval(timer);
                resolve(window.Chart);
            } else if (Date.now() - started > 5000) {
                clearInterval(timer);
                reject(new Error('Chart.js no está disponible.'));
            }
        }, 50);
    });
}

function palette() {
    const style = getComputedStyle(document.documentElement);
    return {
        primary: style.getPropertyValue('--ll-primary').trim() || '#2f5d50',
        primarySoft: style.getPropertyValue('--ll-primary-soft').trim() || 'rgba(47, 93, 80, 0.18)',
        accent: style.getPropertyValue('--ll-accent-strong').trim() || '#8a5a00',
        accentSoft: style.getPropertyValue('--ll-accent-soft').trim() || 'rgba(183, 121, 31, 0.2)',
        ink: style.getPropertyValue('--ll-ink').trim() || '#232323',
        muted: style.getPropertyValue('--ll-muted').trim() || '#5f5a51',
        grid: style.getPropertyValue('--ll-border').trim() || '#ded3c0',
        surface: style.getPropertyValue('--ll-surface').trim() || '#fffdf8'
    };
}

function reduceMotion() {
    return window.matchMedia('(prefers-reduced-motion: reduce)').matches;
}

function baseOptions(colors) {
    return {
        responsive: true,
        maintainAspectRatio: false,
        animation: reduceMotion() ? false : { duration: 450 },
        plugins: {
            legend: {
                labels: {
                    color: colors.ink,
                    usePointStyle: true,
                    boxWidth: 10,
                    padding: 18
                }
            },
            tooltip: {
                backgroundColor: colors.surface,
                titleColor: colors.ink,
                bodyColor: colors.ink,
                borderColor: colors.grid,
                borderWidth: 1,
                padding: 10
            }
        }
    };
}

function destroy(key) {
    const current = charts.get(key);
    if (current) {
        current.destroy();
        charts.delete(key);
    }
}

/**
 * Dibuja la actividad mensual (páginas y libros).
 *
 * @param {HTMLCanvasElement} canvas lienzo
 * @param {Array<{month: number, pagesRead: number, booksFinished: number}>} months datos
 * @returns {Promise<void>} promesa
 */
export async function renderMonthlyChart(canvas, months) {
    if (!canvas || !months) {
        return;
    }

    const Chart = await ensureChart();
    const colors = palette();
    const key = canvas.id || 'monthly';
    destroy(key);

    const labels = months.map((item) => formatMonth(item.month, 'short'));
    const options = baseOptions(colors);
    options.scales = {
        x: {
            grid: { display: false },
            ticks: { color: colors.muted }
        },
        y: {
            beginAtZero: true,
            position: 'left',
            grid: { color: colors.grid },
            ticks: { color: colors.muted, precision: 0 },
            title: {
                display: true,
                text: t('dashboard.chart.pages'),
                color: colors.muted
            }
        },
        y1: {
            beginAtZero: true,
            position: 'right',
            grid: { drawOnChartArea: false },
            ticks: { color: colors.muted, precision: 0 },
            title: {
                display: true,
                text: t('dashboard.chart.books'),
                color: colors.muted
            }
        }
    };

    charts.set(key, new Chart(canvas, {
        type: 'bar',
        data: {
            labels,
            datasets: [
                {
                    label: t('dashboard.chart.pages'),
                    data: months.map((item) => item.pagesRead),
                    backgroundColor: colors.primarySoft,
                    borderColor: colors.primary,
                    borderWidth: 1,
                    borderRadius: 6,
                    yAxisID: 'y'
                },
                {
                    type: 'line',
                    label: t('dashboard.chart.books'),
                    data: months.map((item) => item.booksFinished),
                    borderColor: colors.accent,
                    backgroundColor: colors.accent,
                    pointBackgroundColor: colors.accent,
                    tension: 0.3,
                    borderWidth: 2,
                    pointRadius: 3,
                    yAxisID: 'y1'
                }
            ]
        },
        options
    }));
}

/**
 * Dibuja la distribución de calificaciones.
 *
 * @param {HTMLCanvasElement} canvas lienzo
 * @param {Array<{rating: number, count: number}>} distribution datos
 * @returns {Promise<void>} promesa
 */
export async function renderRatingChart(canvas, distribution) {
    if (!canvas || !distribution) {
        return;
    }

    const Chart = await ensureChart();
    const colors = palette();
    const key = canvas.id || 'ratings';
    destroy(key);

    const options = baseOptions(colors);
    options.cutout = '62%';

    charts.set(key, new Chart(canvas, {
        type: 'doughnut',
        data: {
            labels: distribution.map((item) => t('reviews.chart.label', { rating: item.rating })),
            datasets: [{
                data: distribution.map((item) => item.count),
                backgroundColor: [
                    colors.accentSoft,
                    colors.primarySoft,
                    colors.accent,
                    colors.primary,
                    colors.muted
                ],
                borderColor: colors.surface,
                borderWidth: 2,
                hoverOffset: 4
            }]
        },
        options
    }));
}

/**
 * Dibuja el avance de las metas frente al ritmo esperado.
 *
 * @param {HTMLCanvasElement} canvas lienzo
 * @param {Array<object>} goals metas
 * @returns {Promise<void>} promesa
 */
export async function renderGoalsChart(canvas, goals) {
    if (!canvas || !goals) {
        return;
    }

    const Chart = await ensureChart();
    const colors = palette();
    const key = canvas.id || 'goals';
    destroy(key);

    const labels = goals.map((goal) => goal.period === 'MONTHLY'
        ? `${monthLabel(goal.month)} ${goal.year}`
        : String(goal.year));

    const options = baseOptions(colors);
    options.indexAxis = 'y';
    options.scales = {
        x: {
            beginAtZero: true,
            max: 100,
            grid: { color: colors.grid },
            ticks: {
                color: colors.muted,
                callback: (value) => `${value}%`
            }
        },
        y: {
            grid: { display: false },
            ticks: { color: colors.ink }
        }
    };

    charts.set(key, new Chart(canvas, {
        type: 'bar',
        data: {
            labels,
            datasets: [
                {
                    label: t('goals.chart.actual'),
                    data: goals.map((goal) => goal.booksPercent),
                    backgroundColor: colors.primary,
                    borderRadius: 5
                },
                {
                    label: t('goals.chart.expected'),
                    data: goals.map((goal) => goal.expectedPercent),
                    backgroundColor: colors.accent,
                    borderRadius: 5
                }
            ]
        },
        options
    }));
}

/**
 * Destruye un gráfico concreto.
 *
 * @param {string} key identificador
 */
export function destroyChart(key) {
    destroy(key);
}
