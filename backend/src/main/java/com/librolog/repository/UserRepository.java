package com.librolog.repository;

import com.librolog.entity.User;
import com.librolog.entity.enums.AuthProvider;
import com.librolog.entity.enums.RoleName;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

/**
 * Repositorio de usuarios.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Busca un usuario por correo sin distinguir mayúsculas.
     *
     * @param email correo electrónico
     * @return usuario encontrado, si existe
     */
    Optional<User> findByEmailIgnoreCase(String email);

    /**
     * Busca un usuario por nombre de usuario sin distinguir mayúsculas.
     *
     * @param username nombre de usuario
     * @return usuario encontrado, si existe
     */
    Optional<User> findByUsernameIgnoreCase(String username);

    /**
     * Comprueba si existe un correo.
     *
     * @param email correo electrónico
     * @return verdadero si ya existe
     */
    boolean existsByEmailIgnoreCase(String email);

    /**
     * Comprueba si existe un nombre de usuario.
     *
     * @param username nombre de usuario
     * @return verdadero si ya existe
     */
    boolean existsByUsernameIgnoreCase(String username);

    /**
     * Busca una cuenta vinculada a un proveedor externo.
     *
     * @param provider  proveedor de autenticación
     * @param providerId identificador del proveedor
     * @return usuario encontrado, si existe
     */
    Optional<User> findByAuthProviderAndProviderId(AuthProvider provider, String providerId);

    /**
     * Busca usuarios para el panel de administración con filtros opcionales.
     *
     * <p>Los filtros de texto llegan como cadena vacía y el estado como
     * {@code null} para evitar comparaciones sin tipo en PostgreSQL.</p>
     *
     * @param q       texto libre para usuario, correo o nombre completo
     * @param enabled estado de la cuenta, o {@code null} para no filtrar
     * @param role    rol requerido en mayúsculas, o cadena vacía para no filtrar
     * @param pageable paginación y orden
     * @return página de usuarios
     */
    @Query("""
            select u
              from User u
             where (length(:q) = 0
                    or lower(u.username) like lower(concat('%', :q, '%'))
                    or lower(u.email) like lower(concat('%', :q, '%'))
                    or lower(coalesce(u.fullName, '')) like lower(concat('%', :q, '%')))
               and (coalesce(:enabled, u.enabled) = u.enabled)
               and (length(:role) = 0
                    or exists (select r from u.roles r where str(r.name) = :role))
            """)
    Page<User> searchForAdmin(@Param("q") String q,
                              @Param("enabled") Boolean enabled,
                              @Param("role") String role,
                              Pageable pageable);

    /**
     * Cuenta las cuentas habilitadas.
     *
     * @return número de cuentas habilitadas
     */
    long countByEnabledTrue();

    /**
     * Cuenta las cuentas bloqueadas temporalmente.
     *
     * @param now fecha actual
     * @return número de cuentas bloqueadas
     */
    long countByLockedUntilAfter(Instant now);

    /**
     * Cuenta las cuentas de un proveedor de autenticación.
     *
     * @param provider proveedor
     * @return número de cuentas
     */
    long countByAuthProvider(AuthProvider provider);

    /**
     * Cuenta las cuentas creadas después de una fecha.
     *
     * @param since fecha de referencia
     * @return número de cuentas
     */
    long countByCreatedAtAfter(Instant since);

    /**
     * Cuenta las cuentas que tienen un rol.
     *
     * @param role rol
     * @return número de cuentas
     */
    long countByRoles_Name(RoleName role);

    /**
     * Cuenta las cuentas habilitadas que tienen un rol.
     *
     * @param role rol
     * @return número de cuentas
     */
    long countByRoles_NameAndEnabledTrue(RoleName role);
}
