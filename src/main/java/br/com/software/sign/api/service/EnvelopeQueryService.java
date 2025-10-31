package br.com.software.sign.api.service;

import br.com.software.sign.api.repository.ClicksignEnvelopeRepository;
import br.com.software.sign.api.model.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EnvelopeQueryService {

    private final ClicksignEnvelopeRepository repo;
    private final ObjectMapper om = new ObjectMapper();

    public EnvelopeQueryService(ClicksignEnvelopeRepository repo) {
        this.repo = repo;
    }

    public static class EnvelopeDbOutDTO {
        public String envelopeId;
        public String tenant;
        public short status;
        public short notified;
        public LocalDateTime createdAt;
        public LocalDateTime modifiedAt;
        public List<String> signersEmails = List.of();
        public List<String> documentsIds = List.of();
        public List<String> documentsOriginal = List.of();
        public List<String> documentsSignedB64 = List.of();
    }

    public static class SearchResultDTO {
        public long total;
        public List<EnvelopeDbOutDTO> items;
    }

    public EnvelopeDbOutDTO getByEnvelopeId(String envelopeId, boolean includeBase64) {
        ClicksignEnvelopeEntity e = repo.findByEnvelopeId(envelopeId)
                .orElseThrow(() -> new NoSuchElementException("Envelope não encontrado: " + envelopeId));
        return map(e, includeBase64);
    }

    public SearchResultDTO search(
            String tenant, Integer status, String email,
            LocalDateTime fromTs, LocalDateTime toTs,
            boolean includeBase64, int page, int size
    ) {
        int limit = Math.max(1, Math.min(size, 200));
        int offset = Math.max(0, page) * limit;

        List<ClicksignEnvelopeEntity> rows = repo.searchSimple(
                nullIfBlank(tenant), status, nullIfBlank(email), fromTs, toTs, limit, offset
        );
        long total = repo.countSearchSimple(nullIfBlank(tenant), status, nullIfBlank(email), fromTs, toTs);

        SearchResultDTO out = new SearchResultDTO();
        out.total = total;
        out.items = rows.stream().map(r -> map(r, includeBase64)).collect(Collectors.toList());
        return out;
    }

    private EnvelopeDbOutDTO map(ClicksignEnvelopeEntity e, boolean includeBase64) {
        EnvelopeDbOutDTO dto = new EnvelopeDbOutDTO();
        dto.envelopeId = e.getEnvelopeId();
        dto.tenant = e.getTenant();
        dto.status = e.getStatus();
        dto.notified = e.getNotified();
        dto.createdAt = e.getCreatedAt();
        dto.modifiedAt = e.getModifiedAt();

        dto.signersEmails = parseList(e.getSignersEmails());
        dto.documentsIds = parseList(e.getDocumentsIds());
        dto.documentsOriginal = parseList(e.getDocumentsOriginal());

        if (includeBase64) {
            dto.documentsSignedB64 = parseList(e.getDocumentsSignedB64());
        } else {
            dto.documentsSignedB64 = List.of();
        }
        return dto;
    }

    private List<String> parseList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return om.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception ignore) {
            return List.of();
        }
    }

    public static LocalDateTime parseDateParam(String input, boolean endOfDayIfDateOnly) {
        if (input == null || input.isBlank()) return null;
        try {
            return OffsetDateTime.parse(input).toLocalDateTime();
        } catch (Exception ignored) { }
        try {
            return LocalDateTime.parse(input);
        } catch (Exception ignored) { }
        try {
            LocalDate d = LocalDate.parse(input);
            return endOfDayIfDateOnly ? d.atTime(23, 59, 59) : d.atStartOfDay();
        } catch (Exception ignored) { }
        return null;
    }

    private String nullIfBlank(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    public static String filenameFromUrl(String url) {
        if (url == null || url.isBlank()) return null;
        try {
            String path = URI.create(url).getPath();
            if (path == null) return null;
            int slash = path.lastIndexOf('/');
            return (slash >= 0 && slash + 1 < path.length()) ? path.substring(slash + 1) : path;
        } catch (Exception e) {
            return null;
        }
    }
}