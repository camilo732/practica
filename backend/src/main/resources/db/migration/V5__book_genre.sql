-- ============================================================================
-- LibroLog — V5 — Género de los libros
-- PostgreSQL 18
--
-- Añade el género principal del libro (primer tema útil que devuelve
-- Open Library) para calcular los "géneros más leídos" del anteproyecto.
-- No modifica V1..V4: los cambios se acumulan en migraciones nuevas.
-- ============================================================================

ALTER TABLE books ADD COLUMN genre VARCHAR(100);

CREATE INDEX idx_books_genre_lower ON books (lower(genre));
