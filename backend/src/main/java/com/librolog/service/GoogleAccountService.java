package com.librolog.service;

import com.librolog.entity.Role;
import com.librolog.entity.User;
import com.librolog.entity.enums.AuthProvider;
import com.librolog.entity.enums.RoleName;
import com.librolog.exception.OAuthLoginException;
import com.librolog.repository.RoleRepository;
import com.librolog.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Vincula una cuenta de Google con un usuario de LibroLog.
 *
 * <p>Si el correo ya existe como cuenta local, se vincula a Google conservando
 * la contraseña: la persona puede seguir entrando de las dos formas. Si el
 * correo pertenece a otra cuenta de Google con un identificador distinto, se
 * rechaza para no apropiarse de una cuenta ajena.</p>
 */
@Service
public class GoogleAccountService {

    private static final int MAX_USERNAME_LENGTH = 50;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    /**
     * Crea el servicio con sus dependencias.
     *
     * @param userRepository repositorio de usuarios
     * @param roleRepository repositorio de roles
     */
    public GoogleAccountService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    /**
     * Devuelve el usuario asociado a la cuenta de Google, creándolo si hace
     * falta.
     *
     * @param account datos verificados de Google
     * @return usuario utilizable
     * @throws OAuthLoginException si los datos no son válidos o la cuenta no
     *                             puede iniciar sesión
     */
    @Transactional
    public User resolve(GoogleAccount account) {
        if (account.subject() == null || account.subject().isBlank()) {
            throw new OAuthLoginException("Google no devolvió el identificador de la cuenta.");
        }
        if (account.email() == null || account.email().isBlank()) {
            throw new OAuthLoginException("Google no devolvió un correo electrónico.");
        }
        if (!account.emailVerified()) {
            throw new OAuthLoginException("Google no ha verificado el correo electrónico.");
        }

        String email = account.email().trim().toLowerCase(Locale.ROOT);
        String subject = account.subject().trim();

        User byProvider = userRepository
                .findByAuthProviderAndProviderId(AuthProvider.GOOGLE, subject)
                .orElse(null);
        if (byProvider != null) {
            return refresh(byProvider, account);
        }

        User byEmail = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (byEmail != null) {
            if (!byEmail.isEnabled()) {
                throw new OAuthLoginException("La cuenta está deshabilitada.");
            }
            if (byEmail.getAuthProvider() == AuthProvider.GOOGLE
                    && byEmail.getProviderId() != null
                    && !byEmail.getProviderId().equals(subject)) {
                throw new OAuthLoginException("El correo ya está vinculado a otra cuenta de Google.");
            }

            byEmail.setAuthProvider(AuthProvider.GOOGLE);
            byEmail.setProviderId(subject);
            if ((byEmail.getFullName() == null || byEmail.getFullName().isBlank())
                    && account.name() != null && !account.name().isBlank()) {
                byEmail.setFullName(truncate(account.name().trim(), 120));
            }
            return refresh(byEmail, account);
        }

        User created = new User();
        created.setUsername(uniqueUsername(email));
        created.setEmail(email);
        created.setPassword(null);
        created.setFullName(account.name() == null || account.name().isBlank()
                ? null
                : truncate(account.name().trim(), 120));
        created.setLanguage(normalizeLanguage(account.locale()));
        created.setEnabled(true);
        created.setAuthProvider(AuthProvider.GOOGLE);
        created.setProviderId(subject);
        created.setFailedAttempts((short) 0);

        Role userRole = roleRepository.findByName(RoleName.USER)
                .orElseThrow(() -> new IllegalStateException(
                        "Falta el rol USER en la base de datos. Ejecuta las migraciones de Flyway."));
        created.addRole(userRole);

        return userRepository.save(created);
    }

    private User refresh(User user, GoogleAccount account) {
        if (!user.isEnabled()) {
            throw new OAuthLoginException("La cuenta está deshabilitada.");
        }

        // Google ya demostró la identidad: se limpia un bloqueo por intentos
        // fallidos de contraseña para no dejar a la persona fuera de su cuenta.
        user.setFailedAttempts((short) 0);
        user.setLockedUntil(null);

        if ((user.getFullName() == null || user.getFullName().isBlank())
                && account.name() != null && !account.name().isBlank()) {
            user.setFullName(truncate(account.name().trim(), 120));
        }

        return user;
    }

    private String uniqueUsername(String email) {
        int at = email.indexOf('@');
        String local = at > 0 ? email.substring(0, at) : email;
        String base = local.replaceAll("[^A-Za-z0-9._-]", "");
        if (base.length() < 3) {
            base = "lector";
        }
        base = truncate(base, MAX_USERNAME_LENGTH);

        String candidate = base;
        int suffix = 2;
        while (userRepository.existsByUsernameIgnoreCase(candidate)) {
            String ending = "_" + suffix++;
            candidate = truncate(base, MAX_USERNAME_LENGTH - ending.length()) + ending;
        }
        return candidate;
    }

    private static String normalizeLanguage(String locale) {
        if (locale == null || locale.length() < 2) {
            return "es";
        }
        String language = locale.substring(0, 2).toLowerCase(Locale.ROOT);
        return switch (language) {
            case "en", "de" -> language;
            default -> "es";
        };
    }

    private static String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
