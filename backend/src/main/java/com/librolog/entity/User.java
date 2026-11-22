package com.librolog.entity;

import com.librolog.entity.enums.AuthProvider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Usuario de LibroLog.
 *
 * <p>Puede autenticarse con correo y contraseña ({@link AuthProvider#LOCAL}) o
 * mediante Google ({@link AuthProvider#GOOGLE}). Los roles se almacenan en la
 * tabla intermedia {@code user_roles}.</p>
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(length = 100)
    private String password;

    @Column(name = "full_name", length = 120)
    private String fullName;

    @Column(nullable = false, length = 5)
    private String language = "es";

    @Column(nullable = false)
    private boolean enabled = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false, length = 20)
    private AuthProvider authProvider = AuthProvider.LOCAL;

    @Column(name = "provider_id", length = 255)
    private String providerId;

    @Column(name = "failed_attempts", nullable = false)
    private short failedAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    /**
     * Inicializa las fechas antes de insertar.
     */
    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    /**
     * Actualiza la fecha antes de modificar.
     */
    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Agrega un rol al usuario sin duplicarlos.
     *
     * @param role rol a agregar
     */
    public void addRole(Role role) {
        roles.add(role);
    }

    /**

     * Identificador del usuario

     *

     * @return identificador del usuario

     */
    public Long getId() { return id; }

    /**

     * Establece Identificador del usuario

     *

     * @param id identificador del usuario

     */
    public void setId(Long id) { this.id = id; }

    /**

     * Nombre de usuario

     *

     * @return nombre de usuario

     */
    public String getUsername() { return username; }

    /**

     * Establece Nombre de usuario

     *

     * @param username nombre de usuario

     */
    public void setUsername(String username) { this.username = username; }

    /**

     * Correo electrónico

     *

     * @return correo electrónico

     */
    public String getEmail() { return email; }

    /**

     * Establece Correo electrónico

     *

     * @param email correo electrónico

     */
    public void setEmail(String email) { this.email = email; }

    /**

     * Hash BCrypt de la contraseña

     *

     * @return hash BCrypt de la contraseña

     */
    public String getPassword() { return password; }

    /**

     * Establece Hash BCrypt de la contraseña

     *

     * @param password hash BCrypt de la contraseña

     */
    public void setPassword(String password) { this.password = password; }

    /**

     * Nombre completo

     *

     * @return nombre completo

     */
    public String getFullName() { return fullName; }

    /**

     * Establece Nombre completo

     *

     * @param fullName nombre completo

     */
    public void setFullName(String fullName) { this.fullName = fullName; }

    /**

     * Idioma preferido

     *

     * @return idioma preferido

     */
    public String getLanguage() { return language; }

    /**

     * Establece Idioma preferido

     *

     * @param language idioma preferido

     */
    public void setLanguage(String language) { this.language = language; }

    /**

     * Verdadero si la cuenta está habilitada

     *

     * @return verdadero si la cuenta está habilitada

     */
    public boolean isEnabled() { return enabled; }

    /**

     * Establece Estado de la cuenta

     *

     * @param enabled estado de la cuenta

     */
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    /**

     * Proveedor de autenticación

     *

     * @return proveedor de autenticación

     */
    public AuthProvider getAuthProvider() { return authProvider; }

    /**

     * Establece Proveedor de autenticación

     *

     * @param authProvider proveedor de autenticación

     */
    public void setAuthProvider(AuthProvider authProvider) { this.authProvider = authProvider; }

    /**

     * Identificador del proveedor externo

     *

     * @return identificador del proveedor externo

     */
    public String getProviderId() { return providerId; }

    /**

     * Establece Identificador del proveedor externo

     *

     * @param providerId identificador del proveedor externo

     */
    public void setProviderId(String providerId) { this.providerId = providerId; }

    /**
     * Devuelve el número de intentos de acceso fallidos consecutivos.
     *
     * @return intentos fallidos
     */
    public short getFailedAttempts() { return failedAttempts; }

    /**
     * Establece el número de intentos de acceso fallidos consecutivos.
     *
     * @param failedAttempts intentos fallidos
     */
    public void setFailedAttempts(short failedAttempts) { this.failedAttempts = failedAttempts; }

    /**
     * Devuelve la fecha hasta la que la cuenta está bloqueada.
     *
     * @return fin del bloqueo, o {@code null} si no está bloqueada
     */
    public Instant getLockedUntil() { return lockedUntil; }

    /**
     * Establece la fecha hasta la que la cuenta está bloqueada.
     *
     * @param lockedUntil fin del bloqueo, o {@code null} para desbloquear
     */
    public void setLockedUntil(Instant lockedUntil) { this.lockedUntil = lockedUntil; }

    /**

     * Fecha de creación

     *

     * @return fecha de creación

     */
    public Instant getCreatedAt() { return createdAt; }

    /**

     * Fecha de última modificación

     *

     * @return fecha de última modificación

     */
    public Instant getUpdatedAt() { return updatedAt; }

    /**

     * Roles del usuario

     *

     * @return roles del usuario

     */
    public Set<Role> getRoles() { return roles; }

    /**

     * Establece Roles del usuario

     *

     * @param roles roles del usuario

     */
    public void setRoles(Set<Role> roles) { this.roles = roles; }
}
