package br.com.software.sign.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.*;
import java.util.stream.Collectors;

public class SignerRequirementsDTO {

    private String signerId;
    private String documentId;

    private QualificationDTO qualification;
    private AuthenticationDTO authentication;
    private RubricDTO rubric;

    public String getSignerId() { return signerId; }
    public void setSignerId(String signerId) { this.signerId = signerId; }

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public QualificationDTO getQualification() { return qualification; }
    public void setQualification(QualificationDTO qualification) { this.qualification = qualification; }

    public AuthenticationDTO getAuthentication() { return authentication; }
    public void setAuthentication(AuthenticationDTO authentication) { this.authentication = authentication; }

    public RubricDTO getRubric() { return rubric; }
    public void setRubric(RubricDTO rubric) { this.rubric = rubric; }

    public static class QualificationDTO {
        private boolean enable = true;
        private String action;
        private String role;

        public boolean isEnable() { return enable; }
        public void setEnable(boolean enable) { this.enable = enable; }

        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }

    public static class AuthenticationDTO {
        private static final Set<String> TOKEN_SET = Set.of("email", "sms", "whatsapp");

        private boolean enable = true;

        /** Novo: permite N autenticações (ex.: ["handwritten","email"]) */
        private List<String> auths;

        /** Retrocompat: aceita string única (ou lista via vírgulas) */
        private String auth;

        public boolean isEnable() { return enable; }
        public void setEnable(boolean enable) { this.enable = enable; }

        public List<String> getAuths() { return auths; }
        public void setAuths(List<String> auths) { this.auths = auths; }

        public String getAuth() { return auth; }
        public void setAuth(String auth) { this.auth = auth; }

        /** Lista efetiva de autenticações normalizada */
        @JsonIgnore
        public List<String> effectiveAuths() {
            if (auths != null && !auths.isEmpty()) {
                return normalize(auths);
            }
            if (auth != null && !auth.isBlank()) {
                // permite "email, handwritten" também
                List<String> split = Arrays.stream(auth.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());
                return normalize(split);
            }
            return List.of();
        }

        /** Validação: garante no máx. 1 token (email/sms/whatsapp) */
        @JsonIgnore
        public void validateSingleTokenOrThrow() {
            long tokens = effectiveAuths().stream()
                    .filter(TOKEN_SET::contains)
                    .count();
            if (tokens > 1) {
                throw new IllegalArgumentException(
                        "Apenas um token de autenticação é permitido por signatário (email | sms | whatsapp)."
                );
            }
        }

        private static List<String> normalize(List<String> in) {
            LinkedHashSet<String> set = new LinkedHashSet<>();
            for (String s : in) {
                if (s != null) {
                    String v = s.trim().toLowerCase(Locale.ROOT);
                    if (!v.isEmpty()) set.add(v);
                }
            }
            return new ArrayList<>(set);
        }
    }

    public static class RubricDTO {
        private boolean enable = true;
        private String pages; // "1,2,5" ou "all"

        public boolean isEnable() { return enable; }
        public void setEnable(boolean enable) { this.enable = enable; }

        public String getPages() { return pages; }
        public void setPages(String pages) { this.pages = pages; }
    }
}
