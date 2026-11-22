-- ============================================================================
-- LibroLog · V1 · Esquema inicial
-- PostgreSQL 18
--
-- Este archivo es la fuente de verdad del esquema.
-- No se modifica después de aplicarse: los cambios van en V2, V3, etc.
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 1. Usuarios y roles
-- ---------------------------------------------------------------------------
CREATE TABLE users (
    id             BIGSERIAL PRIMARY KEY,
    username       VARCHAR(50)  NOT NULL,
    email          VARCHAR(255) NOT NULL,
    password       VARCHAR(100),
    full_name      VARCHAR(120),
    language       VARCHAR(5)   NOT NULL DEFAULT 'es',
    enabled        BOOLEAN      NOT NULL DEFAULT TRUE,
    auth_provider  VARCHAR(20)  NOT NULL DEFAULT 'LOCAL',
    provider_id    VARCHAR(255),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT uq_users_provider UNIQUE (auth_provider, provider_id),
    CONSTRAINT ck_users_language CHECK (language IN ('es', 'en')),
    CONSTRAINT ck_users_provider CHECK (auth_provider IN ('LOCAL', 'GOOGLE')),
    CONSTRAINT ck_users_local_password CHECK (auth_provider <> 'LOCAL' OR password IS NOT NULL),
    CONSTRAINT ck_users_local_provider CHECK (auth_provider <> 'LOCAL' OR provider_id IS NULL),
    CONSTRAINT ck_users_google_provider CHECK (auth_provider <> 'GOOGLE' OR provider_id IS NOT NULL)
);

CREATE TABLE roles (
    id    BIGSERIAL PRIMARY KEY,
    name  VARCHAR(30) NOT NULL,
    CONSTRAINT uq_roles_name UNIQUE (name),
    CONSTRAINT ck_roles_name CHECK (name IN ('USER', 'ADMIN'))
);

INSERT INTO roles (name) VALUES ('USER'), ('ADMIN');

CREATE TABLE user_roles (
    user_id  BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id  BIGINT NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- ---------------------------------------------------------------------------
-- 2. Libros y biblioteca personal
-- ---------------------------------------------------------------------------
CREATE TABLE books (
    id                BIGSERIAL PRIMARY KEY,
    open_library_key  VARCHAR(100),
    title             VARCHAR(500) NOT NULL,
    author            VARCHAR(500),
    cover_url         VARCHAR(1000),
    total_pages       INTEGER,
    publish_year      INTEGER,
    isbn              VARCHAR(20),
    description       TEXT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_books_open_library_key UNIQUE (open_library_key),
    CONSTRAINT ck_books_total_pages CHECK (total_pages IS NULL OR total_pages > 0),
    CONSTRAINT ck_books_publish_year CHECK (publish_year IS NULL OR publish_year BETWEEN 1000 AND 2100)
);

CREATE INDEX idx_books_title_lower ON books (lower(title));
CREATE INDEX idx_books_author_lower ON books (lower(author));

CREATE TABLE user_books (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    book_id       BIGINT NOT NULL REFERENCES books (id) ON DELETE RESTRICT,
    status        VARCHAR(20) NOT NULL DEFAULT 'WANT_TO_READ',
    current_page  INTEGER NOT NULL DEFAULT 0,
    total_pages   INTEGER NOT NULL DEFAULT 0,
    started_at    DATE,
    finished_at   DATE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_user_books UNIQUE (user_id, book_id),
    CONSTRAINT ck_user_books_status CHECK (status IN ('WANT_TO_READ', 'READING', 'READ')),
    CONSTRAINT ck_user_books_current_page CHECK (current_page >= 0),
    CONSTRAINT ck_user_books_total_pages CHECK (total_pages >= 0)
);

CREATE INDEX idx_user_books_user_status ON user_books (user_id, status);
CREATE INDEX idx_user_books_book ON user_books (book_id);

-- ---------------------------------------------------------------------------
-- 3. Reseñas
-- ---------------------------------------------------------------------------
CREATE TABLE reviews (
    id                 BIGSERIAL PRIMARY KEY,
    user_book_id       BIGINT NOT NULL REFERENCES user_books (id) ON DELETE CASCADE,
    rating             SMALLINT NOT NULL,
    pace_rating        SMALLINT,
    characters_rating  SMALLINT,
    usefulness_rating  SMALLINT,
    content            TEXT,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_reviews_user_book UNIQUE (user_book_id),
    CONSTRAINT ck_reviews_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT ck_reviews_pace CHECK (pace_rating IS NULL OR pace_rating BETWEEN 1 AND 5),
    CONSTRAINT ck_reviews_characters CHECK (characters_rating IS NULL OR characters_rating BETWEEN 1 AND 5),
    CONSTRAINT ck_reviews_usefulness CHECK (usefulness_rating IS NULL OR usefulness_rating BETWEEN 1 AND 5)
);

-- ---------------------------------------------------------------------------
-- 4. Metas de lectura
-- ---------------------------------------------------------------------------
CREATE TABLE reading_goals (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    period        VARCHAR(10) NOT NULL,
    year          INTEGER NOT NULL,
    month         SMALLINT,
    target_books  INTEGER NOT NULL,
    target_pages  INTEGER NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_goals_period CHECK (period IN ('ANNUAL', 'MONTHLY')),
    CONSTRAINT ck_goals_year CHECK (year BETWEEN 2000 AND 2100),
    CONSTRAINT ck_goals_month CHECK (
        (period = 'ANNUAL' AND month IS NULL)
        OR (period = 'MONTHLY' AND month BETWEEN 1 AND 12)
    ),
    CONSTRAINT ck_goals_target_books CHECK (target_books > 0),
    CONSTRAINT ck_goals_target_pages CHECK (target_pages > 0)
);

CREATE UNIQUE INDEX uq_goals_annual
    ON reading_goals (user_id, year)
    WHERE period = 'ANNUAL';

CREATE UNIQUE INDEX uq_goals_monthly
    ON reading_goals (user_id, year, month)
    WHERE period = 'MONTHLY';

-- ---------------------------------------------------------------------------
-- 5. Bitácora de páginas leídas (la llena el trigger)
-- ---------------------------------------------------------------------------
CREATE TABLE reading_logs (
    id            BIGSERIAL PRIMARY KEY,
    user_book_id  BIGINT NOT NULL REFERENCES user_books (id) ON DELETE CASCADE,
    user_id       BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    log_date      DATE NOT NULL DEFAULT CURRENT_DATE,
    pages_read    INTEGER NOT NULL,
    current_page  INTEGER NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_logs_pages CHECK (pages_read > 0),
    CONSTRAINT ck_logs_current_page CHECK (current_page >= 0)
);

CREATE INDEX idx_logs_user_date ON reading_logs (user_id, log_date);
CREATE INDEX idx_logs_user_book_date ON reading_logs (user_book_id, log_date);

-- ============================================================================
-- 6. Triggers
-- ============================================================================

-- 6.1 updated_at genérico
CREATE OR REPLACE FUNCTION fn_set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at := now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_books_updated_at
    BEFORE UPDATE ON books
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_user_books_updated_at
    BEFORE UPDATE ON user_books
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_reviews_updated_at
    BEFORE UPDATE ON reviews
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_reading_goals_updated_at
    BEFORE UPDATE ON reading_goals
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

-- 6.2 Validación y cambio automático de estado
CREATE OR REPLACE FUNCTION fn_user_books_before_write()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.current_page < 0 THEN
        RAISE EXCEPTION 'current_page no puede ser negativo';
    END IF;

    IF NEW.total_pages > 0 AND NEW.current_page > NEW.total_pages THEN
        RAISE EXCEPTION 'current_page (%) no puede superar total_pages (%)',
            NEW.current_page, NEW.total_pages;
    END IF;

    -- Si hay progreso pero el estado seguía en "quiero leerlo", pasa a "leyendo".
    IF NEW.status = 'WANT_TO_READ' AND NEW.current_page > 0 THEN
        NEW.status := 'READING';
    END IF;

    IF NEW.status = 'READING' AND NEW.started_at IS NULL THEN
        NEW.started_at := CURRENT_DATE;
    END IF;

    -- Si se llega a la última página, el estado pasa a READ.
    IF NEW.total_pages > 0 AND NEW.current_page = NEW.total_pages THEN
        NEW.status := 'READ';
    END IF;

    IF NEW.status = 'READ' THEN
        IF NEW.finished_at IS NULL THEN
            NEW.finished_at := CURRENT_DATE;
        END IF;
    ELSE
        NEW.finished_at := NULL;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_user_books_before_write
    BEFORE INSERT OR UPDATE ON user_books
    FOR EACH ROW EXECUTE FUNCTION fn_user_books_before_write();

-- 6.3 Bitácora automática al aumentar current_page
CREATE OR REPLACE FUNCTION fn_user_books_after_update()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.current_page > OLD.current_page THEN
        INSERT INTO reading_logs (user_book_id, user_id, log_date, pages_read, current_page)
        VALUES (
            NEW.id,
            NEW.user_id,
            CURRENT_DATE,
            NEW.current_page - OLD.current_page,
            NEW.current_page
        );
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_user_books_after_update
    AFTER UPDATE OF current_page ON user_books
    FOR EACH ROW EXECUTE FUNCTION fn_user_books_after_update();
