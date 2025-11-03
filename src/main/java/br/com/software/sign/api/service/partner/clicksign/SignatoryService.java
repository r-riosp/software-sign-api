package br.com.software.sign.api.service.partner.clicksign;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.util.*;

@Service
public class SignatoryService {

    private final ClickSignService clickSignService;
    private final DefaultOptionService defaultOptionService;
    private final ObjectMapper om = new ObjectMapper();


    public SignatoryService(ClickSignService clickSignService, DefaultOptionService defaultOptionService) {
        this.clickSignService = clickSignService;
        this.defaultOptionService = defaultOptionService;
    }

    @SuppressWarnings("unchecked")
    private Map<String,Object> toJsonApi(String defaultType, Map<String,Object> body) {
        if (body == null || body.isEmpty()) {
            return Map.of("data", Map.of("type", defaultType, "attributes", Collections.emptyMap()));
        }

        Object dataObj = body.get("data");
        if (dataObj instanceof Map) return body;

        Map<String,Object> attributes =
                (body.get("attributes") instanceof Map)
                        ? (Map<String,Object>) body.get("attributes")
                        : body;

        Map<String,Object> data = new HashMap<>();
        data.put("type", defaultType);
        data.put("attributes", attributes);
        return Map.of("data", data);
    }

    /** Adiciona um signatário a um envelope */
    /** Defaults exigidos pelo usuário */

    private static final Map<String, Object> DEFAULT_COMM_EVENTS = Map.of(
            "signature_request", "email",
            "signature_reminder", "email",
            "document_signed", "email"
    );

    /** Eventos de comunicação para emails internos (Software/Boxware) */
    private static final Map<String, Object> INTERNAL_COMM_EVENTS = Map.of(
            "signature_request", "email",
            "signature_reminder", "email",
            "document_signed", "email"
    );

    /** Domínios internos que não precisam assinar */
    private static final List<String> INTERNAL_DOMAINS = List.of(
            "@software.com.br",
            "@boxware.com.br"
    );

    /** Verifica se o email é interno */
    private boolean isInternalEmail(String email) {
        if (email == null || email.isBlank()) return false;
        String emailLower = email.toLowerCase().trim();
        return INTERNAL_DOMAINS.stream().anyMatch(emailLower::endsWith);
    }

    /** Adiciona 1 signatário (monta JSON:API + injeta defaults) */

    public Map<String, Object> addSigner(String envelopeId, Map<String, Object> attributes) {
        if (envelopeId == null || envelopeId.isBlank()) {
            throw new IllegalArgumentException("envelopeId é obrigatório");
        }

        Map<String, Object> attrs = new HashMap<>(attributes != null ? attributes : Map.of());

        attrs.putIfAbsent("refusable", false);
        attrs.putIfAbsent("location_required_enabled", true);

        // Define sign_as default se não informado
        if (!attrs.containsKey("sign_as")) {
            String defaultSignAs = defaultOptionService.getSignAs(null);
            attrs.put("sign_as", defaultSignAs);
        }

        // Define communicate_events baseado no domínio do email
        if (!attrs.containsKey("communicate_events")) {
            String email = (String) attrs.get("email");
            if (isInternalEmail(email)) {
                attrs.put("communicate_events", INTERNAL_COMM_EVENTS);
            } else {
                attrs.put("communicate_events", DEFAULT_COMM_EVENTS);
            }
        }

        Map<String, Object> payload = Map.of(
                "data", Map.of(
                        "type", "signers",
                        "attributes", attrs
                )
        );

        return clickSignService.postEnvelopeSigners(envelopeId, payload);
    }

    /** Recebe 1 ou N signatários em JsonNode. Se N, envia 1-a-1 e agrega 207-style. */

    public Object addSignersFlexible(String envelopeId, JsonNode body) {
        if (body == null || body.isNull()) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Body vazio");
        }

        if (body.isArray()) {
            int ok = 0, fail = 0;
            List<Map<String, Object>> items = new ArrayList<>();

            for (int i = 0; i < body.size(); i++) {
                Map<String, Object> attrs = parseAttributes(body.get(i));
                try {
                    Map<String, Object> resp = addSigner(envelopeId, attrs);
                    ok++;
                    items.add(Map.of(
                            "index", i,
                            "status", "SUCCESS",
                            "httpStatus", 200,
                            "input", attrs,
                            "response", resp
                    ));
                } catch (HttpClientErrorException | HttpServerErrorException ex) {
                    fail++;
                    items.add(Map.of(
                            "index", i,
                            "status", "ERROR",
                            "httpStatus", ex.getStatusCode().value(),
                            "input", attrs,
                            "errorBody", ex.getResponseBodyAsString()
                    ));
                } catch (Exception ex) {
                    fail++;
                    items.add(Map.of(
                            "index", i,
                            "status", "ERROR",
                            "httpStatus", 500,
                            "input", attrs,
                            "errorBody", ex.getMessage()
                    ));
                }
            }
            return Map.of("total", body.size(), "success", ok, "fail", fail, "items", items);
        }

        Map<String, Object> attrs = parseAttributes(body);
        return addSigner(envelopeId, attrs);
    }

    /** Aceita:
     * 1) { "name": "...", ... } (attributes direto)
     * 2) { "data": { "type":"signers", "attributes": {...} } }
     */
    private Map<String, Object> parseAttributes(JsonNode node) {
        if (node.hasNonNull("data") && node.get("data").hasNonNull("attributes")) {
            return om.convertValue(node.get("data").get("attributes"),
                    new TypeReference<Map<String, Object>>() {});
        }
        return om.convertValue(node, new TypeReference<Map<String, Object>>() {});
    }


    /** Notifica TODOS os signatários de um envelope */

    public Map<String, Object> notifyAllSigners(String envelopeId, Map<String, Object> body) {
        Map<String,Object> payload = toJsonApi("notifications", body);
        return clickSignService.postEnvelopeNotifications(envelopeId, payload);
    }

    /** Notifica UM signatário específico do envelope */

    public Map<String, Object> notifySigner(String envelopeId, String signerId, Map<String, Object> body) {
        Map<String,Object> payload = toJsonApi("notifications", body);
        return clickSignService.postSignerNotification(envelopeId, signerId, payload);
    }
}
