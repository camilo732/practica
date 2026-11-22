package com.librolog.repository;

import com.librolog.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

/**
 * Repositorio de refresh tokens.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Busca una sesión por el hash del token.
     *
     * @param tokenHash hash SHA-256 en hexadecimal
     * @return sesión encontrada, si existe
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Revoca todas las sesiones activas de un usuario.
     *
     * <p>Se usa cuando se detecta la reutilización de un token revocado: si un
     * atacante intenta reutilizarlo, se cierran todas las sesiones de la
     * víctima.</p>
     *
     * @param userId identificador del usuario
     * @param now    fecha de revocación
     * @return número de sesiones revocadas
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update RefreshToken t
               set t.revokedAt = :now
             where t.user.id = :userId
               and t.revokedAt is null
            """)
    int revokeAllByUserId(@Param("userId") Long userId, @Param("now") Instant now);
}
