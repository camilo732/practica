/*
 * Datos: exportación CSV/JSON, plantilla descargable e importación de CSV.
 * jQuery se usa únicamente aquí, para los eventos del área de arrastre.
 */
import { download, upload } from '../api.js';
import { CONFIG } from '../config.js';
import { formatNumber, onLanguageChange, t } from '../i18n.js';
import { initLayout } from '../layout.js';
import {
    clear,
    describeError,
    el,
    notifyError,
    qs,
    setBusy,
    toast
} from '../ui.js';

const layout = await initLayout();

if (layout) {
    const $ = window.jQuery;
    const state = { file: null };

    bindExports();
    bindDropzone();
    updateSubmitState();

    onLanguageChange(() => {
        if (state.file) {
            renderSelectedFile();
        }
    });

    function bindExports() {
        qs('#export-csv').addEventListener('click', (event) => void runExport(
            event.currentTarget,
            '/api/export/csv',
            'librolog-biblioteca.csv'
        ));

        qs('#export-json').addEventListener('click', (event) => void runExport(
            event.currentTarget,
            '/api/export/json',
            'librolog-biblioteca.json'
        ));

        qs('#download-template').addEventListener('click', downloadTemplate);

        qs('#import-submit').addEventListener('click', submitImport);
        qs('#remove-file').addEventListener('click', clearFile);
    }

    async function runExport(button, path, filename) {
        setBusy(button, true);
        try {
            await download(path, filename);
        } catch (error) {
            notifyError(error, 'data.export.error');
        } finally {
            setBusy(button, false);
        }
    }

    function downloadTemplate() {
        const header = 'title,author,status,currentPage,totalPages,startedAt,finishedAt,isbn,publishYear,rating';
        const example = '"Título de ejemplo","Autoría de ejemplo",READ,120,320,2026-01-01,2026-02-01,9780000000000,2020,4';
        const content = `\uFEFF${header}\r\n${example}\r\n`;

        const blob = new Blob([content], { type: 'text/csv;charset=utf-8' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = 'librolog-plantilla.csv';
        link.rel = 'noopener';
        document.body.appendChild(link);
        link.click();
        link.remove();
        setTimeout(() => URL.revokeObjectURL(url), 2000);
    }

    function bindDropzone() {
        if (!$) {
            console.error('jQuery no está disponible en la página de datos.');
            return;
        }

        const $dropzone = $('#dropzone');
        const $input = $('#csv-file');
        let dragDepth = 0;

        $dropzone.on('click', () => $input.trigger('click'));

        $dropzone.on('keydown', (event) => {
            if (event.key === 'Enter' || event.key === ' ') {
                event.preventDefault();
                $input.trigger('click');
            }
        });

        $input.on('change', (event) => {
            const file = event.target.files && event.target.files[0];
            if (file) {
                acceptFile(file);
            }
        });

        $dropzone.on('dragenter dragover', (event) => {
            event.preventDefault();
            event.stopPropagation();
            if (event.type === 'dragenter') {
                dragDepth += 1;
            }
            $dropzone.addClass('is-dragging');
        });

        $dropzone.on('dragleave', (event) => {
            event.preventDefault();
            event.stopPropagation();
            dragDepth = Math.max(0, dragDepth - 1);
            if (dragDepth === 0) {
                $dropzone.removeClass('is-dragging');
            }
        });

        $dropzone.on('drop', (event) => {
            event.preventDefault();
            event.stopPropagation();
            dragDepth = 0;
            $dropzone.removeClass('is-dragging');

            const transfer = event.originalEvent && event.originalEvent.dataTransfer;
            const file = transfer && transfer.files && transfer.files[0];
            if (file) {
                acceptFile(file);
            }
        });

        // Evita que soltar un archivo fuera de la zona abra el CSV en el navegador.
        $(document).on('dragover drop', (event) => event.preventDefault());
    }

    function acceptFile(file) {
        const alert = qs('#data-alert');
        alert.hidden = true;

        if (!/\.csv$/i.test(file.name)) {
            toast(t('data.import.invalidType'), 'danger');
            clearFile();
            return;
        }
        if (file.size === 0) {
            toast(t('errors.emptyFile'), 'danger');
            clearFile();
            return;
        }
        if (file.size > CONFIG.maxCsvBytes) {
            toast(t('data.import.tooLarge'), 'danger');
            clearFile();
            return;
        }

        state.file = file;
        renderSelectedFile();
        updateSubmitState();
    }

    function renderSelectedFile() {
        const container = qs('#selected-file');
        const name = qs('#selected-file-name');
        const meta = qs('#selected-file-meta');

        if (!state.file) {
            container.hidden = true;
            name.textContent = '';
            meta.textContent = '';
            return;
        }

        name.textContent = state.file.name;
        meta.textContent = `${formatBytes(state.file.size)} · ${t('data.import.formats')}`;
        container.hidden = false;
    }

    function clearFile() {
        state.file = null;
        const input = qs('#csv-file');
        if (input) {
            input.value = '';
        }
        renderSelectedFile();
        updateSubmitState();
    }

    function updateSubmitState() {
        const button = qs('#import-submit');
        if (button) {
            button.disabled = !state.file;
        }
    }

    async function submitImport() {
        const alert = qs('#data-alert');
        alert.hidden = true;

        if (!state.file) {
            toast(t('data.import.noFile'), 'warning');
            return;
        }

        const button = qs('#import-submit');
        const formData = new FormData();
        formData.append('file', state.file, state.file.name);

        setBusy(button, true, 'data.import.uploading');
        try {
            const report = await upload('/api/import/csv', formData);
            renderReport(report);
            toast(t('data.import.success'), 'success');
            clearFile();
            qs('#import-report').scrollIntoView({ behavior: 'smooth', block: 'start' });
        } catch (error) {
            alert.textContent = describeError(error, 'data.import.error');
            alert.hidden = false;
            notifyError(error, 'data.import.error');
        } finally {
            setBusy(button, false);
            updateSubmitState();
        }
    }

    function renderReport(report) {
        const panel = qs('#import-report');
        const container = clear(qs('#report-content'));

        const counts = el('div', { class: 'report-counts' },
            countCard('data.import.report.format', t(`data.format.${report.format}`) || report.format),
            countCard('data.import.report.total', formatNumber(report.totalRows)),
            countCard('data.import.report.imported', formatNumber(report.importedBooks)),
            countCard('data.import.report.reviews', formatNumber(report.importedReviews)),
            countCard('data.import.report.skipped', formatNumber(report.skipped)),
            countCard('data.import.report.errors', formatNumber(report.errorCount))
        );
        container.appendChild(counts);

        if (report.errors && report.errors.length > 0) {
            const table = el('table', { class: 'data-table' },
                el('thead', {},
                    el('tr', {},
                        el('th', { scope: 'col', text: t('data.import.report.line') }),
                        el('th', { scope: 'col', text: t('data.import.report.message') })
                    )
                ),
                el('tbody', {},
                    report.errors.map((row) => el('tr', {},
                        el('th', { scope: 'row', text: formatNumber(row.line) }),
                        el('td', { text: row.message })
                    ))
                )
            );
            container.appendChild(el('div', { class: 'table-scroll' }, table));
        } else {
            container.appendChild(el('p', {
                class: 'text-muted mb-0',
                text: t('data.import.report.noErrors')
            }));
        }

        panel.hidden = false;
    }

    function countCard(labelKey, value) {
        return el('div', { class: 'report-count' },
            el('span', { text: t(labelKey) }),
            el('strong', { text: String(value) })
        );
    }

    function formatBytes(bytes) {
        if (bytes < 1024) {
            return `${bytes} B`;
        }
        if (bytes < 1024 * 1024) {
            return `${(bytes / 1024).toFixed(1)} KB`;
        }
        return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
    }
}
