package br.com.software.sign.api.service.partner.clicksign;

import br.com.software.sign.api.dto.EnvelopeCreateRequestDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
public class EnvelopeService {

    private static final Logger log = LoggerFactory.getLogger(EnvelopeService.class);
    private final ClickSignService clickSignService;

    public EnvelopeService(ClickSignService clickSignService) {
        this.clickSignService = clickSignService;
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

    public Map<String, Object> createEnvelope(EnvelopeCreateRequestDTO dto) {

        Map<String, Object> attributes = new HashMap<>();
        if (dto.getName() != null)              attributes.put("name", dto.getName());
        if (dto.getDeadlineAt() != null)        attributes.put("deadline_at", dto.getDeadlineAt());
        if (dto.getLocale() != null)            attributes.put("locale", dto.getLocale());
        if (dto.getAutoClose() != null)         attributes.put("auto_close", dto.getAutoClose());
        if (dto.getRemindInterval() != null)    attributes.put("remind_interval", dto.getRemindInterval());
        if (dto.getBlockAfterRefusal() != null) attributes.put("block_after_refusal", dto.getBlockAfterRefusal());
        if (dto.getDefaultSubject() != null)    attributes.put("default_subject", dto.getDefaultSubject());
        if (dto.getDefaultMessage() != null)    attributes.put("default_message", dto.getDefaultMessage());

        Map<String, Object> data = new HashMap<>();
        data.put("type", "envelopes");
        data.put("attributes", attributes);

        if (dto.getFolderId() != null && !dto.getFolderId().isEmpty()) {
            Map<String, Object> relationships = Map.of(
                    "folder", Map.of(
                            "data", Map.of("type", "folders", "id", dto.getFolderId())
                    )
            );
            data.put("relationships", relationships);
        }

        Map<String, Object> body = Map.of("data", data);
        log.debug("[EnvelopeService] POST /envelopes body => {}", body);

        return clickSignService.postEnvelope(body);
    }

    public Map<String, Object> activateEnvelope(String envelopeId)
            throws HttpServerErrorException, HttpClientErrorException {
        Map<String, Object> body = Map.of(
                "data", Map.of(
                        "type", "envelopes",
                        "id", envelopeId,
                        "attributes", Map.of("status", "running")
                )
        );
        return clickSignService.patchEnvelope(envelopeId, body);
    }

}
