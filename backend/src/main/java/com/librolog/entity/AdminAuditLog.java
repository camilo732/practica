package com.librolog.entity;

import com.librolog.entity.enums.AdminAuditAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Registro de una acción realizada desde el panel de administración.
 *
 * <p>Se guardan los nombres de usuario como copia histórica: si una cuenta se
 * elimina, la auditoría sigue siendo legible aunque la relación quede a
 * {@code null}.</p>
 */
@Entity
@Table(name = "admin_audit_logs")
public class AdminAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    @Column(name = "actor_username", nullable = false, length = 50)
    private String actorUsername;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id")
    private User targetUser;

    @Column(name = "target_username", nullable = false, length = 50)
    private String targetUsername;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AdminAuditAction action;

    @Column(length = 255)
    private String detail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Inicializa la fecha antes de insertar el registro.
     */
    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    /**
     * Identificador del registro.
     *
     * @return identificador
     */
    public Long getId() {
        return id;
    }

    /**
     * Establece el identificador del registro.
     *
     * @param id identificador
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Administrador que realizó la acción.
     *
     * @return usuario administrador
     */
    public User getActor() {
        return actor;
    }

    /**
     * Establece el administrador que realizó la acción.
     *
     * @param actor usuario administrador
     */
    public void setActor(User actor) {
        this.actor = actor;
    }

    /**
     * Nombre del administrador en el momento de la acción.
     *
     * @return nombre de usuario
     */
    public String getActorUsername() {
        return actorUsername;
    }

    /**
     * Establece el nombre del administrador.
     *
     * @param actorUsername nombre de usuario
     */
    public void setActorUsername(String actorUsername) {
        this.actorUsername = actorUsername;
    }

    /**
     * Cuenta sobre la que se actuó.
     *
     * @return usuario destino
     */
    public User getTargetUser() {
        return targetUser;
    }

    /**
     * Establece la cuenta sobre la que se actuó.
     *
     * @param targetUser usuario destino
     */
    public void setTargetUser(User targetUser) {
        this.targetUser = targetUser;
    }

    /**
     * Nombre de la cuenta destino en el momento de la acción.
     *
     * @return nombre de usuario
     */
    public String getTargetUsername() {
        return targetUsername;
    }

    /**
     * Establece el nombre de la cuenta destino.
     *
     * @param targetUsername nombre de usuario
     */
    public void setTargetUsername(String targetUsername) {
        this.targetUsername = targetUsername;
    }

    /**
     * Acción registrada.
     *
     * @return acción
     */
    public AdminAuditAction getAction() {
        return action;
    }

    /**
     * Establece la acción registrada.
     *
     * @param action acción
     */
    public void setAction(AdminAuditAction action) {
        this.action = action;
    }

    /**
     * Detalle adicional de la acción.
     *
     * @return detalle o {@code null}
     */
    public String getDetail() {
        return detail;
    }

    /**
     * Establece el detalle adicional.
     *
     * @param detail detalle
     */
    public void setDetail(String detail) {
        this.detail = detail;
    }

    /**
     * Fecha de la acción.
     *
     * @return fecha
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Establece la fecha de la acción.
     *
     * @param createdAt fecha
     */
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
