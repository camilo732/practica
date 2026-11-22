package com.librolog.entity.enums;

/**
 * Estado de un libro dentro de la biblioteca personal.
 */
public enum BookStatus {

    /** El usuario quiere leerlo pero aún no ha empezado. */
    WANT_TO_READ,

    /** El usuario lo está leyendo. */
    READING,

    /** El usuario lo terminó. */
    READ
}
