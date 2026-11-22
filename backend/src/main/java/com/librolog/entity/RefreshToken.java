package com.librolog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Sesión persistente de un usuario.
 *
 * <p>Solo se guarda el hash SHA-256 del refresh token en hexadecimal. El token
 * en claro viaja únicamente en la cookie HttpOnly y nunca se almacena.</p>
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Inicializa la fecha de creación antes de insertar.
     */
    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    /**
     * Indica si el token sigue siendo válido.
     *
     * @return verdadero si no está revocado ni caducado
     */
    public boolean isActive() {
        return revokedAt == null && expiresAt.isAfter(Instant.now());
    }

    /**
     * Devuelve el identificador de la sesión.
     *
     * @return identificador
     */
    public Long getId() { return id; }

    /**
     * Establece el identificador de la sesión.
     *
     * @param id identificador
     */
    public void setId(Long id) { this.id = id; }

    /**
     * Devuelve el usuario propietario.
     *
     * @return usuario
     */
    public User getUser() { return user; }

    /**
     * Establece el usuario propietario.
     *
     * @param user usuario
     */
    public void setUser(User user) { this.user = user; }

    /**
     * Devuelve el hash SHA-256 del token.
     *
     * @return hash hexadecimal de 64 caracteres
     */
    public String getTokenHash() { return tokenHash; }

    /**
     * Establece el hash SHA-256 del token.
     *
     * @param tokenHash hash hexadecimal de 64 caracteres
     */
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }

    /**
     * Devuelve la fecha de caducidad.
     *
     * @return caducidad del token
     */
    public Instant getExpiresAt() { return expiresAt; }

    /**
     * Establece la fecha de caducidad.
     *
     * @param expiresAt caducidad del token
     */
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    /**
     * Devuelve la fecha de revocación.
     *
     * @return fecha de revocación, o {@code null} si sigue activo
     */
    public Instant getRevokedAt() { return revokedAt; }

    /**
     * Establece la fecha de revocación.
     *
     * @param revokedAt fecha de revocación
     */
    public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }

    /**
     * Devuelve el agente de usuario que creó la sesión.
     *
     * @return agente de usuario
     */
    public String getUserAgent() { return userAgent; }

    /**
     * Establece el agente de usuario que creó la sesión.
     *
     * @param userAgent agente de usuario
     */
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    /**
     * Devuelve la dirección IP que creó la sesión.
     *
     * @return dirección IP
     */
    public String getIpAddress() { return ipAddress; }

    /**
     * Establece la dirección IP que creó la sesión.
     *
     * @param ipAddress dirección IP
     */
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    /**
     * Devuelve la fecha de creación.
     *
     * @return fecha de creación
     */
    public Instant getCreatedAt() { return createdAt; }
}
