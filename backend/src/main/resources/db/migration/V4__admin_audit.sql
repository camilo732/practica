-- ============================================================================
-- LibroLog — V4 — Administración de usuarios
-- PostgreSQL 18
--
-- Añade la tabla de auditoría de las acciones administrativas. No modifica
-- V1, V2 ni V3: los cambios se acumulan en migraciones nuevas.
-- ============================================================================

CREATE TABLE admin_audit_logs (
    id              BIGSERIAL PRIMARY KEY,
    actor_id        BIGINT REFERENCES users (id) ON DELETE SET NULL,
    actor_username  VARCHAR(50)  NOT NULL,
    target_user_id  BIGINT REFERENCES users (id) ON DELETE SET NULL,
    target_username VARCHAR(50)  NOT NULL,
    action          VARCHAR(40)  NOT NULL,
    detail          VARCHAR(255),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_admin_audit_action CHECK (action IN (
        'USER_ENABLED',
        'USER_DISABLED',
        'ROLE_GRANTED',
        'ROLE_REVOKED',
        'USER_UNLOCKED',
        'SESSIONS_REVOKED'))
);

CREATE INDEX idx_admin_audit_created ON admin_audit_logs (created_at DESC);
CREATE INDEX idx_admin_audit_target ON admin_audit_logs (target_user_id);
