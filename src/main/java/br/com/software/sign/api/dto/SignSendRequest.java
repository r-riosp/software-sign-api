package br.com.software.sign.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public class SignSendRequest {

    private EnvelopeCreateRequestDTO envelope;
    private List<DocumentUpload> documents;
    /** Corpo flexível repassado para o endpoint de signers (array ou objeto) */
    private JsonNode signers;
    /** Template de requisitos aplicado a cada par (signer × document) */
    private RequirementTemplate requirements;

    /** Opcional: ativar envelope ao final (null = não informado) */
    private Boolean activate;
    /** Opcional: notificar todos ao final (após ativação) (null = não informado) */
    private Boolean notifyAll;
    /** Opcional: pular signatários internos (@software.com.br, @boxware.com.br) (default: false) */
    private Boolean skipInternalSigners;

    public EnvelopeCreateRequestDTO getEnvelope() { return envelope; }
    public void setEnvelope(EnvelopeCreateRequestDTO envelope) { this.envelope = envelope; }

    public List<DocumentUpload> getDocuments() { return documents; }
    public void setDocuments(List<DocumentUpload> documents) { this.documents = documents; }

    public JsonNode getSigners() { return signers; }
    public void setSigners(JsonNode signers) { this.signers = signers; }

    public RequirementTemplate getRequirements() { return requirements; }
    public void setRequirements(RequirementTemplate requirements) { this.requirements = requirements; }

    public Boolean getActivate() { return activate; }
    public void setActivate(Boolean activate) { this.activate = activate; }

    public Boolean getNotifyAll() { return notifyAll; }
    public void setNotifyAll(Boolean notifyAll) { this.notifyAll = notifyAll; }

    public Boolean getSkipInternalSigners() { return skipInternalSigners; }
    public void setSkipInternalSigners(Boolean skipInternalSigners) { this.skipInternalSigners = skipInternalSigners; }

    public static class DocumentUpload {

        @JsonAlias({"filename","fileName"})
        private String filename;

        @JsonAlias({"contentBase64","fileBase64","content_base64"})
        private String contentBase64;

        // dados do rodapé
        private FooterDTO footer;

        private String contentType;

        public String getFilename() { return filename; }
        public void setFilename(String filename) { this.filename = filename; }

        public String getContentBase64() { return contentBase64; }
        public void setContentBase64(String contentBase64) { this.contentBase64 = contentBase64; }

        public FooterDTO getFooter() { return footer; }
        public void setFooter(FooterDTO footer) { this.footer = footer; }

        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }
    }

    public static class RequirementTemplate {
        private Requirement qualification;
        private Requirement authentication;
        private Requirement rubric;

        public Requirement getQualification() { return qualification; }
        public void setQualification(Requirement qualification) { this.qualification = qualification; }

        public Requirement getAuthentication() { return authentication; }
        public void setAuthentication(Requirement authentication) { this.authentication = authentication; }

        public Requirement getRubric() { return rubric; }
        public void setRubric(Requirement rubric) { this.rubric = rubric; }

        public static class Requirement {
            private boolean enable = true;
            private String action; // para qualificação (sign, approve, witness)
            private String role;   // para qualificação
            private String auth;   // para autenticação (CSV: "email,handwritten")
            private String pages;  // para rubrica ("1,2,5" ou "all")

            public boolean isEnable() { return enable; }
            public void setEnable(boolean enable) { this.enable = enable; }

            public String getAction() { return action; }
            public void setAction(String action) { this.action = action; }

            public String getRole() { return role; }
            public void setRole(String role) { this.role = role; }

            public String getAuth() { return auth; }
            public void setAuth(String auth) { this.auth = auth; }

            public String getPages() { return pages; }
            public void setPages(String pages) { this.pages = pages; }
        }
    }
}
