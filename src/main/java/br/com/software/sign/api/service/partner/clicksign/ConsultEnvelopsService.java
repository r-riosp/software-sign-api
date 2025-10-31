package br.com.software.sign.api.service.partner.clicksign;

import br.com.software.sign.api.dto.DocumentOutDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Base64;

@Service
public class ConsultEnvelopsService {

    private static final Logger log = LoggerFactory.getLogger(ConsultEnvelopsService.class);

    private final ClickSignService clickSignService;
    private final RestTemplate restTemplate;

    public ConsultEnvelopsService(ClickSignService clickSignService, RestTemplate restTemplate) {
        this.clickSignService = clickSignService;
        this.restTemplate = restTemplate;
    }

    @SuppressWarnings("unchecked")
    public List<DocumentOutDTO> listEnvelopeDocumentsProcessed(
            String envelopeId, Integer page, Integer size, boolean signedAsBase64
    ) throws HttpClientErrorException, HttpServerErrorException {

        List<Map<String, Object>> raw = clickSignService.getEnvelopeDocuments(envelopeId, page, size);
        List<DocumentOutDTO> out = new ArrayList<>();

        for (Map<String, Object> doc : raw) {
            String id = Objects.toString(doc.get("id"), null);

            String originalUrl = null;
            String signedUrl = null;

            Object linksObj = doc.get("links");
            if (linksObj instanceof Map<?, ?> links) {
                Object filesObj = links.get("files");
                if (filesObj instanceof Map<?, ?> files) {
                    Object o = files.get("original");
                    if (o != null) originalUrl = o.toString();
                    Object s = files.get("signed");
                    if (s != null) signedUrl = s.toString();
                }
            }

            Map<String, Object> attributes = Map.of();
            Object attrsObj = doc.get("attributes");
            if (attrsObj instanceof Map<?, ?>) {
                attributes = (Map<String, Object>) attrsObj;
            }

            String signedB64 = null;
            String signedError = null;

            if (signedAsBase64 && signedUrl != null && !signedUrl.isBlank()) {
                try {
                    signedB64 = fetchSignedAsBase64PreservingQuery(signedUrl);
                    if (signedB64 == null) {
                        signedError = "EMPTY_RESPONSE_OR_NON_2XX";
                    }
                } catch (Exception ex) {
                    signedError = ex.getClass().getSimpleName() + ": " + ex.getMessage();
                    log.warn("Falha ao baixar/encodar signed para doc {}: {}", id, signedError);
                }
            }

            DocumentOutDTO.Attributes attrs = null;
            if (attributes != null) {
                attrs = new DocumentOutDTO.Attributes(
                        (String) attributes.get("status"),
                        (String) attributes.get("filename"),
                        (String) attributes.get("created"),
                        (String) attributes.get("modified")
                );
            }

            out.add(new DocumentOutDTO(
                    id,
                    originalUrl,
                    signedB64,
                    attrs
            ));
        }

        return out;
    }

    /**
     * Faz download do PDF assinado preservando exatamente a URL pré-assinada (sem re-encode).
     * Evita erros do tipo "AuthorizationQueryParametersError" no S3.
     */
    private String fetchSignedAsBase64PreservingQuery(String url) {
        java.net.HttpURLConnection conn = null;
        try {
            java.net.URL u = new java.net.URL(url);
            conn = (java.net.HttpURLConnection) u.openConnection();
            conn.setRequestMethod("GET");
            conn.setInstanceFollowRedirects(true);
            conn.setConnectTimeout(15_000);
            conn.setReadTimeout(60_000);

            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                try (java.io.InputStream in = conn.getInputStream();
                     java.io.ByteArrayOutputStream bout = new java.io.ByteArrayOutputStream(64 * 1024)) {
                    byte[] buf = new byte[8192];
                    int r;
                    while ((r = in.read(buf)) != -1) {
                        bout.write(buf, 0, r);
                    }
                    byte[] bytes = bout.toByteArray();
                    return bytes.length > 0 ? Base64.getEncoder().encodeToString(bytes) : null;
                }
            } else {
                String errBody = null;
                try (java.io.InputStream err = conn.getErrorStream()) {
                    if (err != null) {
                        errBody = new String(err.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    }
                } catch (Exception ignored) { /* noop */ }
                log.warn("S3 presigned GET retornou {}. Body: {}", code, errBody);
                return null;
            }
        } catch (Exception ex) {
            log.warn("Falha no GET do signed (preserving query): {}", ex.getMessage());
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
