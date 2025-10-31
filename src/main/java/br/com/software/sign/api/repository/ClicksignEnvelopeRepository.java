package br.com.software.sign.api.repository;

import br.com.software.sign.api.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ClicksignEnvelopeRepository extends JpaRepository<ClicksignEnvelopeEntity, Long> {

    Optional<ClicksignEnvelopeEntity> findByEnvelopeId(String envelopeId);

    @Query(value = """
        SELECT * FROM clicksign_envelopes
        WHERE (:tenant IS NULL OR tenant = :tenant)
          AND (:status IS NULL OR status = :status)
          AND (:email IS NULL OR signers_emails LIKE CONCAT('%', :email, '%'))
          AND (:fromTs IS NULL OR modified_at >= :fromTs)
          AND (:toTs   IS NULL OR modified_at <  :toTs)
        ORDER BY modified_at DESC
        LIMIT :limit OFFSET :offset
        """, nativeQuery = true)
    List<ClicksignEnvelopeEntity> searchSimple(
            @Param("tenant") String tenant,
            @Param("status") Integer status,
            @Param("email") String email,
            @Param("fromTs") LocalDateTime fromTs,
            @Param("toTs") LocalDateTime toTs,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Query(value = """
        SELECT COUNT(*) FROM clicksign_envelopes
        WHERE (:tenant IS NULL OR tenant = :tenant)
          AND (:status IS NULL OR status = :status)
          AND (:email IS NULL OR signers_emails LIKE CONCAT('%', :email, '%'))
          AND (:fromTs IS NULL OR modified_at >= :fromTs)
          AND (:toTs   IS NULL OR modified_at <  :toTs)
        """, nativeQuery = true)
    long countSearchSimple(
            @Param("tenant") String tenant,
            @Param("status") Integer status,
            @Param("email") String email,
            @Param("fromTs") LocalDateTime fromTs,
            @Param("toTs") LocalDateTime toTs
    );
}
