-- Añade el alemán como idioma de interfaz admitido.
-- V1 y V2 son inmutables: el cambio de la restricción se hace en una migración nueva.
ALTER TABLE users DROP CONSTRAINT ck_users_language;

ALTER TABLE users
    ADD CONSTRAINT ck_users_language CHECK (language IN ('es', 'en', 'de'));
