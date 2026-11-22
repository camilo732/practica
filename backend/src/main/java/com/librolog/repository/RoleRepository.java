package com.librolog.repository;

import com.librolog.entity.Role;
import com.librolog.entity.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio de roles.
 */
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * Busca un rol por su nombre.
     *
     * @param name nombre del rol
     * @return rol encontrado, si existe
     */
    Optional<Role> findByName(RoleName name);
}
