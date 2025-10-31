package br.com.software.sign.api.service.partner.clicksign;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class ClickSignService {

    public static final Logger log = LoggerFactory.getLogger(ClickSignService.class);

    private final RestTemplate restTemplate;
    private final ClicksignAuthContext authContext;

    @Value("${CLICKSIGN_BASE_URL}")
    private String baseUrl;

    public ClickSignService(RestTemplate restTemplate, ClicksignAuthContext authContext) {
        this.restTemplate = restTemplate;
        this.authContext = authContext;
    }

    private HttpHeaders jsonApiHeaders() {
        MediaType JSON_API = MediaType.valueOf("application/vnd.api+json");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(JSON_API);
        headers.setAccept(List.of(JSON_API));

        headers.set("Authorization", authContext.resolve(null));
        return headers;
    }

    public Map<String, Object> postEnvelope(Map<String, Object> body)
            throws HttpServerErrorException, HttpClientErrorException {
        String endpoint = (baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length()-1) : baseUrl) + "/envelopes";
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, jsonApiHeaders());
        try {
            ResponseEntity<Map> resp = restTemplate.exchange(endpoint, HttpMethod.POST, entity, Map.class);
            return (Map<String, Object>) resp.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Erro Clicksign {} {}: {}", e.getStatusCode().value(), e.getStatusText(), e.getResponseBodyAsString());
            throw e;
        }
    }

    public Map<String, Object> postUploadDocument(String envelopeId, Map<String, Object> body)
            throws HttpServerErrorException, HttpClientErrorException {
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String endpoint = base + "/envelopes/" + envelopeId + "/documents";
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, jsonApiHeaders());
        try {
            ResponseEntity<Map> resp = restTemplate.exchange(endpoint, HttpMethod.POST, entity, Map.class);
            return resp.getBody() != null ? (Map<String, Object>) resp.getBody() : Map.of();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Erro Clicksign {} {}: {}", e.getStatusCode().value(), e.getStatusText(), e.getResponseBodyAsString());
            throw e;
        }
    }

    public Map<String, Object> postEnvelopeSigners(String envelopeId, Map<String, Object> payload)
            throws HttpServerErrorException, HttpClientErrorException {
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String endpoint = base + "/envelopes/" + envelopeId + "/signers";
        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(payload != null ? payload : Collections.emptyMap(), jsonApiHeaders());
        try {
            ResponseEntity<Map> resp = restTemplate.exchange(endpoint, HttpMethod.POST, entity, Map.class);
            return resp.getBody() != null ? (Map<String, Object>) resp.getBody() : Map.of();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Erro Clicksign {} {}: {}", e.getStatusCode().value(), e.getStatusText(), e.getResponseBodyAsString());
            throw e;
        }
    }

    public Map<String, Object> postRequirimentEnvelope(String envelopeId, Map<String, Object> body)
            throws HttpServerErrorException, HttpClientErrorException {
        String base = (baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length()-1) : baseUrl);
        String endpoint = base + "/envelopes/" + envelopeId + "/requirements";
        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(body != null ? body : Collections.emptyMap(), jsonApiHeaders());
        try {
            ResponseEntity<Map> resp = restTemplate.exchange(endpoint, HttpMethod.POST, entity, Map.class);
            return (Map<String, Object>) resp.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Erro Clicksign {} {}: {}", e.getStatusCode().value(), e.getStatusText(), e.getResponseBodyAsString());
            throw e;
        }
    }

    public Map<String, Object> patchEnvelope(String envelopeId, Map<String, Object> body)
            throws HttpServerErrorException, HttpClientErrorException {
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String endpoint = base + "/envelopes/" + envelopeId;
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, jsonApiHeaders());
        ResponseEntity<Map> resp = restTemplate.exchange(endpoint, HttpMethod.PATCH, entity, Map.class);
        return resp.getBody() != null ? (Map<String, Object>) resp.getBody() : Map.of();
    }

    public Map<String, Object> postEnvelopeNotifications(String envelopeId, Map<String, Object> body)
            throws HttpServerErrorException, HttpClientErrorException {
        String base = (baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length()-1) : baseUrl);
        String endpoint = base + "/envelopes/" + envelopeId + "/notifications";
        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(body != null ? body : Collections.emptyMap(), jsonApiHeaders());
        try {
            ResponseEntity<Map> resp = restTemplate.exchange(endpoint, HttpMethod.POST, entity, Map.class);
            return (Map<String, Object>) resp.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Erro Clicksign {} {}: {}", e.getStatusCode().value(), e.getStatusText(), e.getResponseBodyAsString());
            throw e;
        }
    }

    public Map<String, Object> postSignerNotification(String envelopeId, String signerId, Map<String, Object> body)
            throws HttpServerErrorException, HttpClientErrorException {
        String base = (baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length()-1) : baseUrl);
        String endpoint = base + "/envelopes/" + envelopeId + "/signers/" + signerId + "/notifications";
        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(body != null ? body : Collections.emptyMap(), jsonApiHeaders());
        try {
            ResponseEntity<Map> resp = restTemplate.exchange(endpoint, HttpMethod.POST, entity, Map.class);
            return (Map<String, Object>) resp.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Erro Clicksign {} {}: {}", e.getStatusCode().value(), e.getStatusText(), e.getResponseBodyAsString());
            throw e;
        }
    }

    public List<Map<String, Object>> getEnvelopeDocuments(String envelopeId, Integer page, Integer size)
            throws HttpClientErrorException, HttpServerErrorException {

        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;

        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromHttpUrl(base + "/envelopes/" + envelopeId + "/documents");

        if (page != null && page > 0) {
            uriBuilder.queryParam("page[number]", page);
        }
        if (size != null && size > 0) {
            uriBuilder.queryParam("page[size]", size);
        }

        String endpoint = uriBuilder.toUriString();

        HttpHeaders headers = jsonApiHeaders();
        headers.set(HttpHeaders.USER_AGENT, "software-sign-api/1.0");

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> resp = restTemplate.exchange(endpoint, HttpMethod.GET, entity, Map.class);
            Map<String, Object> body = resp.getBody();
            if (body == null) return List.of();

            Object data = body.get("data");
            if (data instanceof List) {
                return (List<Map<String, Object>>) data;
            } else if (data instanceof Map) {
                return List.of((Map<String, Object>) data);
            } else {
                return List.of();
            }
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Erro Clicksign {} {}: {}", e.getStatusCode().value(), e.getStatusText(), e.getResponseBodyAsString());
            throw e;
        }
    }
}
