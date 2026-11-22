-- ============================================================================
-- LibroLog · V2 · Endurecimiento de la autenticación
-- PostgreSQL 18
--
-- Añade:
--   1. Contadores de intentos fallidos y bloqueo temporal de la cuenta.
--   2. Tabla de refresh tokens (solo se guarda el hash SHA-256).
--
-- No modifica V1: los cambios se acumulan en migraciones nuevas.
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 1. Protección contra fuerza bruta
-- ---------------------------------------------------------------------------
ALTER TABLE users
    ADD COLUMN failed_attempts SMALLINT NOT NULL DEFAULT 0,
    ADD COLUMN locked_until    TIMESTAMPTZ;

ALTER TABLE users
    ADD CONSTRAINT ck_users_failed_attempts CHECK (failed_attempts >= 0);

-- ---------------------------------------------------------------------------
-- 2. Refresh tokens
--
-- El token en claro nunca se almacena. Se guarda su hash SHA-256 en
-- hexadecimal (64 caracteres). Si la base se filtra, los tokens no son
-- utilizables.
-- ---------------------------------------------------------------------------
CREATE TABLE refresh_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  VARCHAR(64)  NOT NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,
    revoked_at  TIMESTAMPTZ,
    user_agent  VARCHAR(255),
    ip_address  VARCHAR(45),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_refresh_tokens_hash UNIQUE (token_hash),
    CONSTRAINT ck_refresh_tokens_hash CHECK (token_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_refresh_tokens_expires CHECK (expires_at > created_at)
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);

-- Índice parcial: solo interesan los tokens vivos al validar una sesión.
CREATE INDEX idx_refresh_tokens_active
    ON refresh_tokens (user_id, expires_at)
    WHERE revoked_at IS NULL;
