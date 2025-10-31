package br.com.software.sign.api.dto;

import java.util.List;
import java.util.Map;

public class SignSendResponse {

    private String envelopeId;

    private List<DocumentResult> documents;
    private List<SignerResult> signers;

    private RequirementSummary requirements;

    private boolean activated;
    private Map<String, Object> notifications;

    private List<ErrorItem> errors;

    public String getEnvelopeId() { return envelopeId; }
    public void setEnvelopeId(String envelopeId) { this.envelopeId = envelopeId; }

    public List<DocumentResult> getDocuments() { return documents; }
    public void setDocuments(List<DocumentResult> documents) { this.documents = documents; }

    public List<SignerResult> getSigners() { return signers; }
    public void setSigners(List<SignerResult> signers) { this.signers = signers; }

    public RequirementSummary getRequirements() { return requirements; }
    public void setRequirements(RequirementSummary requirements) { this.requirements = requirements; }

    public boolean isActivated() { return activated; }
    public void setActivated(boolean activated) { this.activated = activated; }

    public Map<String, Object> getNotifications() { return notifications; }
    public void setNotifications(Map<String, Object> notifications) { this.notifications = notifications; }

    public List<ErrorItem> getErrors() { return errors; }
    public void setErrors(List<ErrorItem> errors) { this.errors = errors; }

    public static class DocumentResult {
        private String id;
        private String filename;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getFilename() { return filename; }
        public void setFilename(String filename) { this.filename = filename; }
    }

    public static class SignerResult {
        private String id;
        private String label;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
    }

    public static class RequirementSummary {
        private int totalSuccess;
        private int totalFail;
        private List<Map<String, Object>> success;
        private List<Map<String, Object>> fail;

        public int getTotalSuccess() { return totalSuccess; }
        public void setTotalSuccess(int totalSuccess) { this.totalSuccess = totalSuccess; }

        public int getTotalFail() { return totalFail; }
        public void setTotalFail(int totalFail) { this.totalFail = totalFail; }

        public List<Map<String, Object>> getSuccess() { return success; }
        public void setSuccess(List<Map<String, Object>> success) { this.success = success; }

        public List<Map<String, Object>> getFail() { return fail; }
        public void setFail(List<Map<String, Object>> fail) { this.fail = fail; }
    }

    public static class ErrorItem {
        private String step;            // envelope|documents|signers|requirements|activate|notify
        private String detail;          // mensagem
        private Integer http;           // status HTTP da upstream (opcional)
        private Object upstream;        // corpo de erro da upstream (opcional)

        public String getStep() { return step; }
        public void setStep(String step) { this.step = step; }

        public String getDetail() { return detail; }
        public void setDetail(String detail) { this.detail = detail; }

        public Integer getHttp() { return http; }
        public void setHttp(Integer http) { this.http = http; }

        public Object getUpstream() { return upstream; }
        public void setUpstream(Object upstream) { this.upstream = upstream; }
    }
}
