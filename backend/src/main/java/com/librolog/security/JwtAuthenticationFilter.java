package com.librolog.security;

import com.librolog.entity.User;
import com.librolog.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Autentica cada petición a partir del token Bearer.
 *
 * <p>Tras validar la firma se consulta el usuario en la base de datos: así una
 * cuenta deshabilitada o eliminada deja de tener acceso de inmediato, aunque su
 * token siga vigente.</p>
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    /**
     * Crea el filtro con sus dependencias.
     *
     * @param jwtService     servicio de tokens
     * @param userRepository repositorio de usuarios
     */
    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    /**
     * Lee el token, lo valida y publica la autenticación en el contexto.
     *
     * @param request  petición HTTP
     * @param response respuesta HTTP
     * @param chain    siguiente filtro
     * @throws ServletException si falla la cadena
     * @throws IOException      si falla la lectura
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            authenticate(request, header.substring(BEARER_PREFIX.length()).trim());
        }
        chain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, String token) {
        if (token.isEmpty() || SecurityContextHolder.getContext().getAuthentication() != null) {
            return;
        }

        try {
            JwtPayload payload = jwtService.parse(token);
            userRepository.findById(payload.userId())
                    .filter(User::isEnabled)
                    .ifPresent(user -> {
                        Set<String> roles = new LinkedHashSet<>();
                        user.getRoles().forEach(role -> roles.add(role.getName().name()));

                        List<SimpleGrantedAuthority> authorities = roles.stream()
                                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                                .toList();

                        AuthenticatedUser principal =
                                new AuthenticatedUser(user.getId(), user.getUsername(), Set.copyOf(roles));
                        var authentication =
                                new UsernamePasswordAuthenticationToken(principal, null, authorities);
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    });
        } catch (JwtException | IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
        }
    }
}
