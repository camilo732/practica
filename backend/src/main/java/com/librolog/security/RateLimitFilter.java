package com.librolog.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Limita las peticiones a {@code /api/auth/**} por dirección IP.
 *
 * <p>Devuelve 429 con {@code Retry-After} cuando se supera el cupo. Las rutas
 * que validan credenciales (registro e inicio de sesión) usan el cupo
 * configurado; las de sesión (renovación y cierre) tienen un cupo mayor porque
 * el navegador las usa al cargar cada página. Las lecturas del perfil no se
 * limitan: no aceptan credenciales y el token de acceso ya está validado.</p>
 *
 * <p>Es la primera barrera contra la fuerza bruta; la segunda es el bloqueo de
 * la cuenta tras varios intentos fallidos.</p>
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_ENTRIES = 10_000;

    /** Multiplicador del cupo para las rutas que usan la cookie de sesión. */
    private static final int SESSION_QUOTA_MULTIPLIER = 6;

    /** Mínimo de peticiones por minuto para renovar o cerrar sesión. */
    private static final int SESSION_QUOTA_MINIMUM = 60;

    private final int requestsPerMinute;
    private final Map<String, Bucket> credentialBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> sessionBuckets = new ConcurrentHashMap<>();

    /**
     * Crea el filtro con el cupo indicado.
     *
     * @param requestsPerMinute peticiones permitidas por minuto y por IP
     */
    public RateLimitFilter(int requestsPerMinute) {
        this.requestsPerMinute = requestsPerMinute;
    }

    /**
     * Solo se aplica a las rutas de autenticación que consumen credenciales o
     * sesión.
     *
     * @param request petición HTTP
     * @return verdadero si el filtro debe omitirse
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())
                || !uri.startsWith("/api/auth/")) {
            return true;
        }

        // El perfil es una lectura autenticada por Bearer: no acepta
        // credenciales ni la cookie de refresco, así que no necesita cupo.
        return "GET".equalsIgnoreCase(request.getMethod()) && uri.equals("/api/auth/me");
    }

    /**
     * Consume un permiso del cubo asociado a la IP.
     *
     * @param request  petición HTTP
     * @param response respuesta HTTP
     * @param chain    siguiente filtro
     * @throws ServletException si falla la cadena
     * @throws IOException      si falla la escritura
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        boolean sessionRoute = isSessionRoute(request.getRequestURI());
        Map<String, Bucket> buckets = sessionRoute ? sessionBuckets : credentialBuckets;

        if (buckets.size() > MAX_ENTRIES) {
            buckets.clear();
        }

        String clientKey = (sessionRoute ? "session:" : "credentials:") + request.getRemoteAddr();
        Bucket bucket = buckets.computeIfAbsent(clientKey, key -> newBucket(sessionRoute));
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (!probe.isConsumed()) {
            long retryAfter = Math.max(1L, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()));
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfter));
            ProblemJsonWriter.write(response, HttpStatus.TOO_MANY_REQUESTS.value(),
                    "Demasiadas solicitudes",
                    "Has realizado demasiadas peticiones de autenticación. Inténtalo de nuevo en unos segundos.");
            return;
        }

        response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
        chain.doFilter(request, response);
    }

    private static boolean isSessionRoute(String uri) {
        return uri.endsWith("/refresh") || uri.endsWith("/logout");
    }

    private Bucket newBucket(boolean sessionRoute) {
        int limit = sessionRoute
                ? Math.max(SESSION_QUOTA_MINIMUM, requestsPerMinute * SESSION_QUOTA_MULTIPLIER)
                : requestsPerMinute;

        return Bucket.builder()
                .addLimit(Bandwidth.classic(limit,
                        Refill.greedy(limit, Duration.ofMinutes(1))))
                .build();
    }
}
