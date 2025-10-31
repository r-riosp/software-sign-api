package br.com.software.sign.api.service.partner.clicksign;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.util.*;

@Service
public class DocumentsService {

    private final ClickSignService clickSignService;

    public DocumentsService(ClickSignService clickSignService) {
        this.clickSignService = clickSignService;
    }

    /**
     * Aceita dois formatos de body:
     *
     * 1) ÚNICO documento (compatível com o que você já usa):
     * {
     *   "filename": "contrato.pdf",
     *   "content_base64": "<BASE64 cru, sem prefixo>",
     *   "type_doc_config": "pdf",      // opcional (deduz pelo filename se ausente)
     *   "metadata": { ... }            // opcional
     * }
     *
     * 2) VÁRIOS documentos:
     * {
     *   "documents": [
     *     { "filename": "...", "content_base64": "...", "type_doc_config": "pdf", "metadata": {...} },
     *     { "filename": "...", "content_base64": "...", "type_doc_config": "png" }
     *   ]
     * }
     *
     * Retorna: { "results": [ <respostaClicksignDoc1>, <respostaClicksignDoc2>, ... ] }
     */
    public Map<String, Object> uploadDocuments(String envelopeId, Map<String, Object> body)
            throws HttpServerErrorException, HttpClientErrorException {

        if (body.containsKey("documents") && body.get("documents") instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> docs = (List<Map<String, Object>>) body.get("documents");
            if (docs == null || docs.isEmpty()) {
                throw new IllegalArgumentException("A lista 'documents' está vazia.");
            }

            List<Map<String, Object>> results = new ArrayList<>(docs.size());

            for (Map<String, Object> doc : docs) {
                Map<String, Object> clicksignBody = buildClicksignPayloadFromSimpleDoc(doc);
                Map<String, Object> resp = clickSignService.postUploadDocument(envelopeId, clicksignBody);
                results.add(resp != null ? resp : Map.of());
            }
            return Map.of("results", results);
        }

        Map<String, Object> clicksignBody = buildClicksignPayloadFromSimpleDoc(body);
        Map<String, Object> singleResp = clickSignService.postUploadDocument(envelopeId, clicksignBody);
        return Map.of("results", List.of(singleResp != null ? singleResp : Map.of()));
    }

    private Map<String, Object> buildClicksignPayloadFromSimpleDoc(Map<String, Object> simple) {
        String filename = getString(simple, "filename");
        String base64Raw = getString(simple, "content_base64");
        String typeDoc = getString(simple, "type_doc_config");
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) simple.getOrDefault("metadata", Map.of());

        if (StringUtils.isBlank(filename)) {
            throw new IllegalArgumentException("filename é obrigatório");
        }
        if (StringUtils.isBlank(base64Raw)) {
            throw new IllegalArgumentException("content_base64 (sem prefixo) é obrigatório");
        }

        if (StringUtils.isBlank(typeDoc)) {
            typeDoc = guessTypeFromFilename(filename);
        }
        String mime = resolveMime(typeDoc, filename);
        String contentWithPrefix = "data:" + mime + ";base64," + base64Raw;

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("filename", filename);
        attributes.put("content_base64", contentWithPrefix);
        if (metadata != null && !metadata.isEmpty()) {
            attributes.put("metadata", metadata);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("type", "documents");
        data.put("attributes", attributes);

        return Map.of("data", data);
    }

    private static String getString(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v == null ? null : String.valueOf(v).trim();
    }

    private static String guessTypeFromFilename(String filename) {
        int i = filename.lastIndexOf('.');
        if (i > -1 && i < filename.length() - 1) {
            return filename.substring(i + 1);
        }
        return null;
    }

    private static String resolveMime(String typeDocConfig, String filename) {
        String t = (typeDocConfig == null ? "" : typeDocConfig).toLowerCase(Locale.ROOT);

        switch (t) {
            case "pdf":  return "application/pdf";
            case "doc":  return "application/msword";
            case "docx": return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls":  return "application/vnd.ms-excel";
            case "xlsx": return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt":  return "application/vnd.ms-powerpoint";
            case "pptx": return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "png":  return "image/png";
            case "jpg":
            case "jpeg": return "image/jpeg";
            case "gif":  return "image/gif";
            case "txt":  return "text/plain";
            case "csv":  return "text/csv";
            case "json": return "application/json";
            case "xml":  return "application/xml";
        }

        String ext = (filename != null && filename.contains("."))
                ? filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT)
                : "";

        switch (ext) {
            case "pdf":  return "application/pdf";
            case "doc":  return "application/msword";
            case "docx": return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls":  return "application/vnd.ms-excel";
            case "xlsx": return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt":  return "application/vnd.ms-powerpoint";
            case "pptx": return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "png":  return "image/png";
            case "jpg":
            case "jpeg": return "image/jpeg";
            case "gif":  return "image/gif";
            case "txt":  return "text/plain";
            case "csv":  return "text/csv";
            case "json": return "application/json";
            case "xml":  return "application/xml";
            default:
                return "application/octet-stream";
        }
    }
}
