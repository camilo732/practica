/*
 * Configuración compartida del frontend de LibroLog.
 * No contiene secretos: todo se resuelve contra el mismo origen.
 */
function detectApiBase() {
    const meta = document.querySelector('meta[name="librolog-api-base"]');
    return meta && meta.content ? meta.content.replace(/\/$/, '') : '';
}

export const CONFIG = Object.freeze({
    appName: 'LibroLog',
    apiBase: detectApiBase(),
    clientHeader: 'X-LibroLog-Client',
    clientValue: 'web',
    defaultLanguage: 'es',
    languages: ['es', 'en', 'de'],
    storageKeys: {
        language: 'librolog.language'
    },
    libraryPageSize: 12,
    reviewsPageSize: 8,
    maxCsvBytes: 10 * 1024 * 1024,
    pages: {
        index: '/index.html',
        login: '/login.html',
        register: '/register.html',
        dashboard: '/dashboard.html',
        library: '/library.html',
        catalog: '/catalog.html',
        reviews: '/reviews.html',
        goals: '/goals.html',
        data: '/data.html',
        profile: '/profile.html',
        admin: '/admin.html',
        oauthCallback: '/oauth2-callback.html'
    }
});
