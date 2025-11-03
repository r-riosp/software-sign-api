package br.com.software.sign.api.service.partner.clicksign;

import br.com.software.sign.api.dto.AddRequirementsRequestDTO;
import br.com.software.sign.api.dto.SignerRequirementsDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.util.*;

@Service
public class RequirementsService {

    private static final Logger log = LoggerFactory.getLogger(RequirementsService.class);

    private static final Set<String> TOKEN_SET = Set.of("email", "sms", "whatsapp");

    private final ClickSignService clickSignService;

    public RequirementsService(ClickSignService clickSignService) {
        this.clickSignService = clickSignService;
    }

    /** Dispara Qualificação, Autenticação(1..N) e Rubrica (quando enable=true) para cada signatário. */
    public BatchResult addAll(String envelopeId, AddRequirementsRequestDTO req) {
        if (req == null || req.getSigners() == null || req.getSigners().isEmpty()) {
            throw new IllegalArgumentException("Nenhum signatário informado.");
        }

        BatchResult result = new BatchResult();

        for (SignerRequirementsDTO s : req.getSigners()) {
            String signerId   = s.getSignerId();
            String documentId = s.getDocumentId();

            // ---- Qualificação
            if (s.getQualification() != null && s.getQualification().isEnable()) {
                Map<String, Object> payload =
                        buildQualificationPayload(documentId, signerId, 
                                s.getQualification().getAction(), 
                                s.getQualification().getRole());
                call(envelopeId, "qualification", signerId, result, payload);
            }

            // ---- Autenticação (suporta N tipos)
            if (s.getAuthentication() != null && s.getAuthentication().isEnable()) {
                // (opcional) regra de negócio: apenas 1 token (email|sms|whatsapp)
                try {
                    s.getAuthentication().validateSingleTokenOrThrow();
                } catch (IllegalArgumentException ex) {
                    result.addFailure("authentication", signerId, HttpStatus.BAD_REQUEST.value(), ex.getMessage());
                    // segue para os próximos signers/documents sem interromper todo o batch
                }

                // Itera e cria 1 requisito por auth
                List<String> auths = s.getAuthentication().effectiveAuths();
                if (auths == null || auths.isEmpty()) {
                    // nada a enviar; registra como falha de parâmetro
                    result.addFailure("authentication", signerId, HttpStatus.BAD_REQUEST.value(),
                            "Nenhum tipo de autenticação informado (auth/auths vazio).");
                } else {
                    for (String auth : auths) {
                        Map<String, Object> payload =
                                buildAuthenticationPayload(documentId, signerId, auth);
                        String actionName = TOKEN_SET.contains(auth) ? ("authentication:" + auth) : ("authentication:" + auth);
                        call(envelopeId, actionName, signerId, result, payload);
                    }
                }
            }

            // ---- Rubrica
            if (s.getRubric() != null && s.getRubric().isEnable()) {
                Map<String, Object> payload =
                        buildRubricPayload(documentId, signerId, s.getRubric().getPages());
                call(envelopeId, "rubric", signerId, result, payload);
            }
        }

        return result;
    }

    private void call(String envelopeId, String actionName, String signerId,
                      BatchResult result, Map<String, Object> payload) {
        try {
            Map<String, Object> resp = clickSignService.postRequirimentEnvelope(envelopeId, payload);
            result.addSuccess(actionName, signerId, resp);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            result.addFailure(actionName, signerId, e.getStatusCode().value(), e.getResponseBodyAsString());
        } catch (Exception e) {
            result.addFailure(actionName, signerId, HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
        }
    }

    // Qualificação: action e role enviados em attributes
    private Map<String, Object> buildQualificationPayload(String documentId, String signerId, 
                                                          String action, String role) {
        Map<String, Object> attributes = new HashMap<>();
        if (action != null && !action.isBlank()) {
            attributes.put("action", action);
        } else {
            attributes.put("action", "sign"); // default fallback
        }
        if (role != null && !role.isBlank()) attributes.put("role", role);
        return requirementJsonApi(attributes, documentId, signerId);
    }

    // Autenticação: action = "provide_evidence" (default), auth enviado em attributes.auth
    private Map<String, Object> buildAuthenticationPayload(String documentId, String signerId, String auth) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("action", "provide_evidence");
        if (auth != null && !auth.isBlank()) attributes.put("auth", auth);
        return requirementJsonApi(attributes, documentId, signerId);
    }

    // Rubrica: action = "rubricate" (default), pages enviado em attributes.pages (string "1,2,5" ou "all")
    private Map<String, Object> buildRubricPayload(String documentId, String signerId, String pages) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("action", "rubricate");
        if (pages != null && !pages.isBlank()) attributes.put("pages", pages);
        return requirementJsonApi(attributes, documentId, signerId);
    }

    /**
     * Envelopa no formato JSON:API:
     * {
     *   "data": {
     *     "type": "requirements",
     *     "attributes": {...},
     *     "relationships": {
     *       "document": {"data":{"type":"documents","id":"..." }},
     *       "signer":   {"data":{"type":"signers","id":"..."   }}
     *     }
     *   }
     * }
     */
    private Map<String, Object> requirementJsonApi(Map<String, Object> attributes,
                                                   String documentId, String signerId) {
        Map<String, Object> documentData = Map.of("type", "documents", "id", documentId);
        Map<String, Object> signerData   = Map.of("type", "signers",   "id", signerId);

        Map<String, Object> relationships = Map.of(
                "document", Map.of("data", documentData),
                "signer",   Map.of("data", signerData)
        );

        Map<String, Object> data = new HashMap<>();
        data.put("type", "requirements");
        data.put("attributes", attributes);
        data.put("relationships", relationships);

        return Map.of("data", data);
    }

    /** Resultado agregado para decidir 201/207/400 etc. */
    public static class BatchResult {
        private final List<Map<String, Object>> successes = new ArrayList<>();
        private final List<Map<String, Object>> failures  = new ArrayList<>();

        public List<Map<String, Object>> getSuccesses() { return successes; }
        public List<Map<String, Object>> getFailures()  { return failures; }

        public int successCount() { return successes.size(); }
        public int failureCount() { return failures.size(); }

        void addSuccess(String action, String signerId, Map<String, Object> resp) {
            successes.add(Map.of(
                    "action", action,
                    "signerId", signerId,
                    "response", resp == null ? Map.of() : resp
            ));
        }

        void addFailure(String action, String signerId, int status, String detail) {
            failures.add(Map.of(
                    "action", action,
                    "signerId", signerId,
                    "status", status,
                    "detail", detail == null ? "" : detail
            ));
        }

        public boolean allSucceeded() { return failures.isEmpty() && !successes.isEmpty(); }
        public boolean allFailed()    { return successes.isEmpty() && !failures.isEmpty(); }
    }
}
