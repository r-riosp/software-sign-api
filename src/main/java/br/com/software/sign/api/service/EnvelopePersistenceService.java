package br.com.software.sign.api.service;

import br.com.software.sign.api.dto.DocumentOutDTO;
import br.com.software.sign.api.model.ClicksignEnvelopeEntity;
import br.com.software.sign.api.repository.ClicksignEnvelopeRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EnvelopePersistenceService {

    private final ClicksignEnvelopeRepository repo;
    private final ObjectMapper om = new ObjectMapper();

    public EnvelopePersistenceService(ClicksignEnvelopeRepository repo) {
        this.repo = repo;
    }

    /**
     * Chame ao iniciar a orquestração (logo após criar/enviar envelope).
     */
    @Transactional
    public void initRecord(String envelopeId,
                           String tenant,
                           List<String> signersIds,
                           List<String> signersEmails,
                           List<String> documentIds) {

        ClicksignEnvelopeEntity e = repo.findByEnvelopeId(envelopeId)
                .orElseGet(ClicksignEnvelopeEntity::new);

        e.setEnvelopeId(envelopeId);
        e.setTenant(tenant != null ? tenant : "software");
        e.setStatus((short) 1); // ativo/running
        e.setNotified((short) 0);
        if (e.getCreatedAt() == null) e.setCreatedAt(LocalDateTime.now());
        e.setModifiedAt(LocalDateTime.now());

        e.setSignersIds(toJson(signersIds));
        e.setSignersEmails(toJson(signersEmails));
        e.setDocumentsIds(toJson(documentIds));
        e.setAuthenticated(toJson(Collections.emptyList()));  // preenche depois se quiser
        e.setDocumentsSignedB64(toJson(Collections.emptyList()));


        repo.save(e);
    }

    /**
     * MERGE apenas dos IDs de documentos (sem URLs originais).
     */
    @Transactional
    public void mergeDocumentIds(String envelopeId, List<String> newDocIds) {
        repo.findByEnvelopeId(envelopeId).ifPresent(e -> {
            List<String> ids = mergeDistinct(parseList(e.getDocumentsIds()), nullSafe(newDocIds));
            e.setDocumentsIds(toJson(ids));

            e.setModifiedAt(LocalDateTime.now());
            repo.save(e);
        });
    }

    /**
     * MERGE de signers (ids + emails).
     */
    @Transactional
    public void mergeSigners(String envelopeId, List<String> newSignerIds, List<String> newEmails) {
        repo.findByEnvelopeId(envelopeId).ifPresent(e -> {
            List<String> ids = mergeDistinct(parseList(e.getSignersIds()), nullSafe(newSignerIds));
            List<String> emails = mergeDistinct(parseList(e.getSignersEmails()), nullSafe(newEmails));
            e.setSignersIds(toJson(ids));
            e.setSignersEmails(toJson(emails));
            e.setModifiedAt(LocalDateTime.now());
            repo.save(e);
        });
    }

    /**
     * MERGE dos tipos de autenticação (ex.: ["email","handwritten","sms"]).
     */
    @Transactional
    public void mergeAuthenticated(String envelopeId, List<String> newAuths) {
        repo.findByEnvelopeId(envelopeId).ifPresent(e -> {
            List<String> auths = mergeDistinct(parseList(e.getAuthenticated()), nullSafe(newAuths));
            e.setAuthenticated(toJson(auths));
            e.setModifiedAt(LocalDateTime.now());
            repo.save(e);
        });
    }

    /**
     * Atualiza com base no GET /documents (fecha e salva B64 se "closed").
     */
    @Transactional
    public void updateFromDocumentsFetch(String envelopeId, String tenant, List<DocumentOutDTO> docs) {
        ClicksignEnvelopeEntity e = repo.findByEnvelopeId(envelopeId)
                .orElseGet(() -> {
                    ClicksignEnvelopeEntity ne = new ClicksignEnvelopeEntity();
                    ne.setEnvelopeId(envelopeId);
                    ne.setTenant(tenant != null ? tenant : "software");
                    ne.setStatus((short) 0);
                    ne.setCreatedAt(LocalDateTime.now());
                    return ne;
                });

        List<String> docIds = docs.stream()
                .map(DocumentOutDTO::id)
                .filter(Objects::nonNull)
                .toList();
        e.setDocumentsIds(toJson(docIds));

        List<String> originals = docs.stream()
                .map(DocumentOutDTO::original)
                .filter(s -> s != null && !s.isBlank())
                .toList();
        e.setDocumentsOriginal(toJson(originals)); // mantém como JSON na coluna (TEXT/MEDIUMTEXT)

        boolean anyClosed = docs.stream().anyMatch(d -> {
            var a = d.attributes();
            return a != null && a.status() != null && "closed".equalsIgnoreCase(a.status());
        });
        e.setStatus(anyClosed ? (short) 2 : (short) 1);

        // signed base64 em "[b64],[b64]"
        List<String> signedList = docs.stream()
                .map(DocumentOutDTO::signed)
                .filter(s -> s != null && !s.isBlank())
                .toList();
        e.setDocumentsSignedB64(signedList.isEmpty()
                ? null
                : signedList.stream().map(s -> "[" + s + "]").collect(Collectors.joining(","))
        );

        LocalDateTime bestModified = extractMaxModified(docs).orElse(LocalDateTime.now());
        e.setModifiedAt(bestModified);

        repo.save(e);
    }

    /**
     * Marca notified.
     */
    @Transactional
    public void setNotified(String envelopeId, boolean notified) {
        repo.findByEnvelopeId(envelopeId).ifPresent(e -> {
            e.setNotified((short) (notified ? 1 : 0));
            e.setModifiedAt(LocalDateTime.now());
            repo.save(e);
        });
    }

    /**
     * Acrescenta texto de erro (append).
     */
    @Transactional
    public void appendError(String envelopeId, String errorMessage) {
        repo.findByEnvelopeId(envelopeId).ifPresent(e -> {
            String current = e.getErrors();
            e.setErrors((current == null || current.isBlank())
                    ? errorMessage
                    : current + "\n" + errorMessage);
            e.setModifiedAt(LocalDateTime.now());
            repo.save(e);
        });
    }

    private String toJson(Object value) {
        try {
            return om.writeValueAsString(value == null ? Collections.emptyList() : value);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<String> parseList(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return om.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception ignore) {
            return new ArrayList<>();
        }
    }

    private List<String> nullSafe(List<String> in) {
        return (in == null) ? Collections.emptyList() : in;
    }

    private List<String> mergeDistinct(List<String> base, List<String> add) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        if (base != null)
            set.addAll(base.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty()).toList());
        if (add != null)
            set.addAll(add.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty()).toList());
        return new ArrayList<>(set);
    }

    private Optional<LocalDateTime> extractMaxModified(List<DocumentOutDTO> docs) {
        List<LocalDateTime> times = new ArrayList<>();

        for (DocumentOutDTO d : docs) {
            DocumentOutDTO.Attributes attrs = d.attributes();
            if (attrs != null) {

                if ("closed".equalsIgnoreCase(attrs.status())) {
                }

                String modified = attrs.modified();
                if (modified != null && !modified.isBlank()) {
                    try {
                        LocalDateTime ts = OffsetDateTime.parse(modified).toLocalDateTime();
                        times.add(ts);
                    } catch (Exception ignored) {

                    }
                }
            }
        }

        return times.stream().max(LocalDateTime::compareTo);
    }
}

