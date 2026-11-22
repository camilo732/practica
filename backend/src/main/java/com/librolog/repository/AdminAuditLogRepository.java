package com.librolog.repository;

import com.librolog.entity.AdminAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio de la auditoría administrativa.
 */
public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {

    /**
     * Lista los registros de auditoría del más reciente al más antiguo.
     *
     * @param pageable paginación
     * @return página de registros
     */
    Page<AdminAuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
