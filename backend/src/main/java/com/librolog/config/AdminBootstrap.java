package com.librolog.config;

import com.librolog.entity.Role;
import com.librolog.entity.User;
import com.librolog.entity.enums.RoleName;
import com.librolog.repository.RoleRepository;
import com.librolog.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Concede el rol ADMIN a las cuentas indicadas en {@code APP_ADMIN_EMAILS}.
 *
 * <p>Se ejecuta al arrancar y es idempotente. Solo actúa sobre cuentas que ya
 * existen: no crea usuarios ni cambia contraseñas. Si no hay ningún
 * administrador, deja un aviso en el registro con las instrucciones.</p>
 *
 * <p>Se desactiva en el perfil {@code test}, donde la base H2 arranca vacía y
 * no ejecuta las migraciones de Flyway.</p>
 */
@Component
@Profile("!test")
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final String adminEmails;

    /**
     * Crea el inicializador.
     *
     * @param userRepository repositorio de usuarios
     * @param roleRepository repositorio de roles
     * @param adminEmails    correos separados por coma
     */
    public AdminBootstrap(UserRepository userRepository,
                          RoleRepository roleRepository,
                          @Value("${app.admin.emails:}") String adminEmails) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.adminEmails = adminEmails;
    }

    /**
     * Promueve las cuentas configuradas y comprueba que quede un administrador.
     *
     * @param args argumentos de arranque
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Role adminRole = roleRepository.findByName(RoleName.ADMIN).orElse(null);
        if (adminRole == null) {
            log.error("No existe el rol ADMIN. Ejecuta las migraciones de Flyway.");
            return;
        }

        int promoted = 0;
        if (adminEmails != null && !adminEmails.isBlank()) {
            for (String raw : adminEmails.split(",")) {
                String email = raw.trim().toLowerCase(Locale.ROOT);
                if (email.isEmpty()) {
                    continue;
                }

                User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
                if (user == null) {
                    log.warn("APP_ADMIN_EMAILS contiene un correo sin cuenta registrada.");
                    continue;
                }

                boolean alreadyAdmin = user.getRoles().stream()
                        .anyMatch(role -> role.getName() == RoleName.ADMIN);
                if (!alreadyAdmin) {
                    user.addRole(adminRole);
                    userRepository.save(user);
                    promoted++;
                }
            }
        }

        if (promoted > 0) {
            log.info("Rol ADMIN concedido a {} cuenta(s) configurada(s).", promoted);
        }

        if (userRepository.countByRoles_Name(RoleName.ADMIN) == 0) {
            log.warn("No hay administradores. Añade APP_ADMIN_EMAILS con el correo de una cuenta registrada.");
        }
    }
}
