package com.librolog.entity;

import com.librolog.entity.enums.RoleName;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Rol de seguridad de LibroLog.
 */
@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 30)
    private RoleName name;

    /**

     * Identificador del rol

     *

     * @return identificador del rol

     */
    public Long getId() { return id; }

    /**

     * Establece Identificador del rol

     *

     * @param id identificador del rol

     */
    public void setId(Long id) { this.id = id; }

    /**

     * Nombre del rol

     *

     * @return nombre del rol

     */
    public RoleName getName() { return name; }

    /**

     * Establece Nombre del rol

     *

     * @param name nombre del rol

     */
    public void setName(RoleName name) { this.name = name; }
}
