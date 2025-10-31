package br.com.software.sign.api.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "clicksign_envelopes")
public class ClicksignEnvelopeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "envelope_id", nullable = false, unique = true, length = 80)
    private String envelopeId;

    @Column(name = "tenant", nullable = false, length = 32)
    private String tenant = "software";

    @Column(name = "documents_ids", columnDefinition = "TEXT")
    private String documentsIds;               // ex.: ["docId1","docId2"]

    @Column(name = "documents_signed_b64", columnDefinition = "LONGTEXT")
    private String documentsSignedB64;        // ex.: ["<b64 doc1>","<b64 doc2>"]

    @Column(name = "documents_original", columnDefinition = "MEDIUMTEXT")
    private String documentsOriginal;         // ex.: ["https://.../doc1.pdf","https://..."]

    @Column(name = "signers_ids", columnDefinition = "TEXT")
    private String signersIds;                 // ex.: ["signerId1","signerId2"]

    @Column(name = "signers_emails", columnDefinition = "TEXT")
    private String signersEmails;              // ex.: ["a@x.com","b@y.com"]

    @Column(name = "authenticated", columnDefinition = "TEXT")
    private String authenticated;              // ex.: ["aut1","aut2"]

    /** 0=pendente, 1=ativo, 2=fechado */
    @Column(name = "status", nullable = false, columnDefinition = "TINYINT")
    private short status = 0;

    /** 0=não, 1=sim */
    @Column(name = "notified", nullable = false, columnDefinition = "TINYINT")
    private short notified = 0;

    @Column(name = "errors", columnDefinition = "MEDIUMTEXT")
    private String errors;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "modified_at", nullable = false)
    private LocalDateTime modifiedAt;

    @PrePersist
    protected void onCreate() {
        if (tenant == null || tenant.isBlank()) {
            tenant = "software";
        }
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (modifiedAt == null) modifiedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        modifiedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }

    public String getEnvelopeId() { return envelopeId; }
    public void setEnvelopeId(String envelopeId) { this.envelopeId = envelopeId; }

    public String getTenant() { return tenant; }
    public void setTenant(String tenant) { this.tenant = tenant; }

    public String getDocumentsIds() { return documentsIds; }
    public void setDocumentsIds(String documentsIds) { this.documentsIds = documentsIds; }

    public String getDocumentsSignedB64() { return documentsSignedB64; }
    public void setDocumentsSignedB64(String documentsSignedB64) { this.documentsSignedB64 = documentsSignedB64; }

    public String getDocumentsOriginal() { return documentsOriginal; }
    public void setDocumentsOriginal(String documentsOriginal) { this.documentsOriginal = documentsOriginal; }

    public String getSignersIds() { return signersIds; }
    public void setSignersIds(String signersIds) { this.signersIds = signersIds; }

    public String getSignersEmails() { return signersEmails; }
    public void setSignersEmails(String signersEmails) { this.signersEmails = signersEmails; }

    public String getAuthenticated() { return authenticated; }
    public void setAuthenticated(String authenticated) { this.authenticated = authenticated; }

    public short getStatus() { return status; }
    public void setStatus(short status) { this.status = status; }

    public short getNotified() { return notified; }
    public void setNotified(short notified) { this.notified = notified; }

    public String getErrors() { return errors; }
    public void setErrors(String errors) { this.errors = errors; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getModifiedAt() { return modifiedAt; }
    public void setModifiedAt(LocalDateTime modifiedAt) { this.modifiedAt = modifiedAt; }
}
