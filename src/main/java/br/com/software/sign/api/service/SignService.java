package br.com.software.sign.api.service;

import br.com.software.sign.api.dto.*;
import br.com.software.sign.api.dto.SignSendRequest.*;
import br.com.software.sign.api.dto.SignerRequirementsDTO;
import br.com.software.sign.api.service.partner.clicksign.ClicksignAuthContext;
import br.com.software.sign.api.service.partner.clicksign.ConsultEnvelopsService;
import br.com.software.sign.api.service.partner.clicksign.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.Base64;

@Service
public class SignService {

    private static final Logger log = LoggerFactory.getLogger(SignService.class);
    private static final ObjectMapper OM = new ObjectMapper();

    /** Domínios internos que não precisam assinar */
    private static final List<String> INTERNAL_DOMAINS = List.of(
            "@software.com.br",
            "@boxware.com.br"
    );

    /** Verifica se o email é interno (não precisa assinar) */
    private static boolean isInternalEmail(String email) {
        if (email == null || email.isBlank()) return false;
        String emailLower = email.toLowerCase().trim();
        return INTERNAL_DOMAINS.stream().anyMatch(emailLower::endsWith);
    }

    private final EnvelopeService envelopeService;
    private final DocumentsService documentsService;
    private final SignatoryService signatoryService;
    private final RequirementsService requirementsService;
    private final DefaultOptionService defaultOptionService;
    private final PdfStampService pdfStampService;
    private final ConsultEnvelopsService consultEnvelopsService;
    private final EnvelopePersistenceService envelopePersistenceService;
    private final EnvelopeQueryService envelopeQueryService;
    private final ClicksignAuthContext authContext;

    public SignService(EnvelopeService envelopeService,
                       DocumentsService documentsService,
                       SignatoryService signatoryService,
                       RequirementsService requirementsService,
                       DefaultOptionService defaultOptionService,
                       PdfStampService pdfStampService,
                       ConsultEnvelopsService consultEnvelopsService,
                       EnvelopePersistenceService envelopePersistenceService,
                       EnvelopeQueryService envelopeQueryService,
                       ClicksignAuthContext authContext) {
        this.envelopeService = envelopeService;
        this.documentsService = documentsService;
        this.signatoryService = signatoryService;
        this.requirementsService = requirementsService;
        this.defaultOptionService = defaultOptionService;
        this.pdfStampService = pdfStampService;
        this.consultEnvelopsService = consultEnvelopsService;
        this.envelopePersistenceService = envelopePersistenceService;
        this.envelopeQueryService = envelopeQueryService;
        this.authContext = authContext;
    }

    @Value("${CLICKSIGN_API_KEY_SOFTWARE}") private String tokenSoftware;
    @Value("${CLICKSIGN_API_KEY_OTHER}")    private String tokenOther;

    /**
     * Fluxo completo de envio (orquestração).
     */
    // Dentro de SignService

    public SignSendResponse send(String name, SignSendRequest req) {
        long __totalStart = System.currentTimeMillis();
        log.info("[send] BEGIN name={}", name);
        try {
            // Resolve tenant e autenticação
            long t1 = System.currentTimeMillis();
            log.debug("[send] -> resolveTenantByName");
            String effectiveName = (name != null && !name.isBlank())
                    ? name
                    : (req.getEnvelope() != null ? req.getEnvelope().getName() : null);
            String tenant = resolveTenantByName(effectiveName);
            log.debug("[send] <- resolveTenantByName ({} ms) tenant={}", (System.currentTimeMillis() - t1), tenant);

            t1 = System.currentTimeMillis();
            log.debug("[send] -> resolveToken");
            String token = resolveToken(tenant);
            log.debug("[send] <- resolveToken ({} ms)", (System.currentTimeMillis() - t1));

            t1 = System.currentTimeMillis();
            log.debug("[send] -> authContext.setApiKeyOverride");
            authContext.setApiKeyOverride(token);
            log.debug("[send] <- authContext.setApiKeyOverride ({} ms)", (System.currentTimeMillis() - t1));

            log.info("[send] tenant={} name={}", tenant, effectiveName);

            // Estado da orquestração
            SignSendResponse resp = new SignSendResponse();
            List<SignSendResponse.ErrorItem> errors = new ArrayList<>();
            resp.setErrors(errors);

            List<String> documentIds = new ArrayList<>();
            List<String> signerIds = new ArrayList<>();
            List<String> signerEmailsFromReq = new ArrayList<>();
            LinkedHashSet<String> authsApplied = new LinkedHashSet<>();

            // 1) Envelope
            String envelopeId = null;
            t1 = System.currentTimeMillis();
            log.debug("[send] -> createEnvelopeOrThrow");
            try {
                envelopeId = createEnvelopeOrThrow(req, resp, errors);
                log.debug("[send] <- createEnvelopeOrThrow ({} ms) envelopeId={}", (System.currentTimeMillis() - t1), envelopeId);
            } catch (RuntimeException e) {
                log.error("[send] !! createEnvelopeOrThrow failed ({} ms): {}", (System.currentTimeMillis() - t1), e.toString());
                throw e;
            }

            // 2) Documentos
            t1 = System.currentTimeMillis();
            log.debug("[send] -> processDocuments");
            List<SignSendResponse.DocumentResult> docResults = null;
            try {
                docResults = processDocuments(envelopeId, req, errors, documentIds);
            } catch (RuntimeException e) {
                log.error("[send] !! processDocuments failed ({} ms): {}", (System.currentTimeMillis() - t1), e.toString());
                throw e;
            } finally {
                log.debug("[send] <- processDocuments ({} ms) documents={}", (System.currentTimeMillis() - t1), (docResults!=null?docResults.size():0));
            }
            resp.setDocuments(docResults);

            // 3) Signatários
            t1 = System.currentTimeMillis();
            log.debug("[send] -> handleSigners");
            List<SignSendResponse.SignerResult> signerResults = null;
            try {
                signerResults = handleSigners(envelopeId, req, errors, signerIds, signerEmailsFromReq);
            } catch (RuntimeException e) {
                log.error("[send] !! handleSigners failed ({} ms): {}", (System.currentTimeMillis() - t1), e.toString());
                throw e;
            } finally {
                log.debug("[send] <- handleSigners ({} ms) signers={}", (System.currentTimeMillis() - t1), (signerResults!=null?signerResults.size():0));
            }
            resp.setSigners(signerResults);

            // 4) Defaults + Requirements
            t1 = System.currentTimeMillis();
            log.debug("[send] -> applyDefaultRequirementsIfMissing");
            try {
                applyDefaultRequirementsIfMissing(req);
            } catch (RuntimeException e) {
                log.error("[send] !! applyDefaultRequirementsIfMissing failed ({} ms): {}", (System.currentTimeMillis() - t1), e.toString());
                throw e;
            } finally {
                log.debug("[send] <- applyDefaultRequirementsIfMissing ({} ms)", (System.currentTimeMillis() - t1));
            }

            t1 = System.currentTimeMillis();
            log.debug("[send] -> buildAndApplyRequirements");
            boolean templateEnabled = false;
            try {
                templateEnabled = buildAndApplyRequirements(envelopeId, req, signerIds, documentIds, errors, resp, authsApplied);
            } catch (RuntimeException e) {
                log.error("[send] !! buildAndApplyRequirements failed ({} ms): {}", (System.currentTimeMillis() - t1), e.toString());
                throw e;
            } finally {
                log.debug("[send] <- buildAndApplyRequirements ({} ms) templateEnabled={}", (System.currentTimeMillis() - t1), templateEnabled);
            }

            // 5) Ativar
            t1 = System.currentTimeMillis();
            log.debug("[send] -> defaultOptionService.getActivate");
            boolean activate = defaultOptionService.getActivate(req.getActivate());
            log.debug("[send] <- defaultOptionService.getActivate ({} ms) activate={}", (System.currentTimeMillis() - t1), activate);

            t1 = System.currentTimeMillis();
            log.debug("[send] -> maybeActivate");
            try {
                maybeActivate(envelopeId, activate, templateEnabled, resp, errors);
            } catch (RuntimeException e) {
                log.error("[send] !! maybeActivate failed ({} ms): {}", (System.currentTimeMillis() - t1), e.toString());
                throw e;
            } finally {
                log.debug("[send] <- maybeActivate ({} ms) activated={}", (System.currentTimeMillis() - t1), resp.isActivated());
            }

            // 6) Notificar
            t1 = System.currentTimeMillis();
            log.debug("[send] -> defaultOptionService.getNotifyAll");
            boolean notify = defaultOptionService.getNotifyAll(req.getNotifyAll());
            log.debug("[send] <- defaultOptionService.getNotifyAll ({} ms) notify={}", (System.currentTimeMillis() - t1), notify);

            t1 = System.currentTimeMillis();
            log.debug("[send] -> maybeNotify");
            try {
                maybeNotify(envelopeId, notify, resp, errors);
            } catch (RuntimeException e) {
                log.error("[send] !! maybeNotify failed ({} ms): {}", (System.currentTimeMillis() - t1), e.toString());
                throw e;
            } finally {
                Object notifications = resp.getNotifications();
                int notifCount = 0;
                try {
                    if (notifications instanceof Map) {
                        Object r = ((Map<?,?>) notifications).get("results");
                        if (r instanceof List<?>) notifCount = ((List<?>) r).size();
                    }
                } catch (Throwable ignored) {}
                log.debug("[send] <- maybeNotify ({} ms) notified={} count={}", (System.currentTimeMillis() - t1), (notifications!=null), notifCount);
            }

            // 7) Persistência
            t1 = System.currentTimeMillis();
            log.debug("[send] -> finalizePersistence");
            try {
                finalizePersistence(envelopeId, tenant, signerEmailsFromReq, signerIds, documentIds, authsApplied, resp);
            } catch (RuntimeException e) {
                log.error("[send] !! finalizePersistence failed ({} ms): {}", (System.currentTimeMillis() - t1), e.toString());
                throw e;
            } finally {
                log.debug("[send] <- finalizePersistence ({} ms) docIds={} signerIds={} auths={} ", (System.currentTimeMillis() - t1), documentIds.size(), signerIds.size(), authsApplied.size());
            }

            return resp;
        } finally {
            log.info("[send] END ({} ms)", (System.currentTimeMillis() - __totalStart));
        }
    }

    private String createEnvelopeOrThrow(SignSendRequest req,
                                         SignSendResponse resp,
                                         List<SignSendResponse.ErrorItem> errors) {
        try {
            Map<String, Object> created = envelopeService.createEnvelope(req.getEnvelope());
            String envelopeId = extractDataId(created);
            resp.setEnvelopeId(envelopeId);
            return envelopeId;
        } catch (Exception e) {
            errors.add(err("envelope", e));
            throw runtime(e);
        }
    }

    private List<SignSendResponse.DocumentResult> processDocuments(String envelopeId,
                                                                   SignSendRequest req,
                                                                   List<SignSendResponse.ErrorItem> errors,
                                                                   List<String> documentIdsOut) {
        List<SignSendResponse.DocumentResult> resultsOut = new ArrayList<>();
        try {
            Map<String, Object> body = new HashMap<>();
            List<Map<String, Object>> docs = new ArrayList<>();

            List<DocumentUpload> uploads = Optional.ofNullable(req.getDocuments()).orElse(List.of());
            for (DocumentUpload d : uploads) {
                String b64 = sanitizeBase64(d.getContentBase64());
                String filename = d.getFilename();

                if (b64 == null || b64.isBlank()) {
                    errors.add(err("documents", "Documento sem base64: " + filename));
                    continue;
                }

                if (d.getFooter() != null) {
                    try {
                        byte[] original = Base64.getDecoder().decode(b64);
                        byte[] stamped  = pdfStampService.addFooterLeftLastPage(original, d.getFooter());
                        b64 = Base64.getEncoder().encodeToString(stamped);
                    } catch (IOException ioe) {
                        errors.add(err("documents", "Falha ao carimbar PDF: " + filename));
                        continue;
                    } catch (Exception ex) {
                        errors.add(err("documents", "Erro inesperado no carimbo: " + filename));
                        continue;
                    }
                }

                Map<String, Object> item = new HashMap<>();
                item.put("filename", filename);
                item.put("content_base64", b64);
                item.put("content_type", Optional.ofNullable(d.getContentType()).orElse("application/pdf"));
                docs.add(item);
            }

            body.put("documents", docs);

            Map<String, Object> upload = documentsService.uploadDocuments(envelopeId, body);
            List<Map<String, Object>> results = safeList(upload.get("results"));
            for (Map<String, Object> it : results) {
                String id = extractDataId(it);
                if (id != null) {
                    documentIdsOut.add(id);
                    var dr = new SignSendResponse.DocumentResult();
                    dr.setId(id);
                    dr.setFilename(guessFilename(it));
                    resultsOut.add(dr);
                }
            }
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            errors.add(err("documents", e.getResponseBodyAsString(), e.getStatusCode().value()));
        } catch (Exception e) {
            errors.add(err("documents", e));
        }
        return resultsOut;
    }

    private List<SignSendResponse.SignerResult> handleSigners(String envelopeId,
                                                              SignSendRequest req,
                                                              List<SignSendResponse.ErrorItem> errors,
                                                              List<String> signerIdsOut,
                                                              List<String> emailsOut) {
        List<SignSendResponse.SignerResult> signerResults = new ArrayList<>();
        boolean skipInternalSigners = Boolean.TRUE.equals(req.getSkipInternalSigners());

        try {
            var filtered = OM.createArrayNode();

            if (req.getSigners() != null && req.getSigners().isArray()) {
                for (JsonNode n : req.getSigners()) {
                    String email = extractEmailFromSignerNode(n);
                    if (email != null && !email.isBlank()) {
                        emailsOut.add(email);
                        if (skipInternalSigners && isInternalEmail(email)) {
                            log.debug("[send] Email interno {} não será adicionado como signatário (skipInternalSigners=true)", email);
                        } else {
                            filtered.add(n);
                        }
                    }
                }
            }

            Object result = signatoryService.addSignersFlexible(envelopeId, filtered);

            List<String> extractedIds = extractSignerIds(result);
            signerIdsOut.addAll(extractedIds);

            int idx = 0;
            for (JsonNode n : filtered) {
                if (idx < extractedIds.size()) {
                    var sr = new SignSendResponse.SignerResult();
                    sr.setId(extractedIds.get(idx));
                    sr.setLabel(extractedIds.get(idx));
                    signerResults.add(sr);
                    idx++;
                }
            }
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            errors.add(err("signers", e.getResponseBodyAsString(), e.getStatusCode().value()));
        } catch (Exception e) {
            errors.add(err("signers", e));
        }
        return signerResults;
    }

    private static String extractEmailFromSignerNode(JsonNode n) {
        if (n == null || n.isNull()) return null;
        if (n.hasNonNull("email")) return n.get("email").asText(null);
        JsonNode signer = n.get("signer");
        if (signer != null && signer.hasNonNull("email")) return signer.get("email").asText(null);
        return null;
    }

    private void applyDefaultRequirementsIfMissing(SignSendRequest req) {
        if (req.getRequirements() != null) return;

        var tpl0 = new RequirementTemplate();

        var a = new RequirementTemplate.Requirement();
        a.setEnable(true);
        a.setAuth(defaultOptionService.getAuth(null)); // "email,handwritten"
        tpl0.setAuthentication(a);

        var r = new RequirementTemplate.Requirement();
        r.setEnable(true);
        r.setPages(defaultOptionService.getRubricPages(null)); // "all"
        tpl0.setRubric(r);

        var q0 = new RequirementTemplate.Requirement();
        q0.setEnable(true);
        q0.setAction(defaultOptionService.getAction(null)); // "sign"
        q0.setRole(defaultOptionService.getRole(null)); // "party"
        tpl0.setQualification(q0);

        req.setRequirements(tpl0);
    }

    private boolean buildAndApplyRequirements(String envelopeId,
                                              SignSendRequest req,
                                              List<String> signerIds,
                                              List<String> documentIds,
                                              List<SignSendResponse.ErrorItem> errors,
                                              SignSendResponse resp,
                                              LinkedHashSet<String> authsApplied) {
        var tpl = req.getRequirements();
        boolean templateEnabled = hasTemplateEnabled(tpl);
        if (!templateEnabled) return false;

        String action  = defaultOptionService.getAction(tpl.getQualification()==null ? null : tpl.getQualification().getAction());
        String role    = defaultOptionService.getRole(tpl.getQualification()==null ? null : tpl.getQualification().getRole());
        String authCsv = defaultOptionService.getAuth(tpl.getAuthentication()==null ? null : tpl.getAuthentication().getAuth());
        String pages   = defaultOptionService.getRubricPages(tpl.getRubric()==null ? null : tpl.getRubric().getPages());

        List<String> auths = Arrays.stream(authCsv.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).distinct().toList();
        authsApplied.addAll(auths);

        if (signerIds.isEmpty() || documentIds.isEmpty()) {
            errors.add(err("requirements",
                    "Requisitos não executados: faltam signerIds ou documentIds (ver inclusão de signatários e upload de documentos)."));
            return true;
        }

        try {
            AddRequirementsRequestDTO ar = new AddRequirementsRequestDTO();
            List<SignerRequirementsDTO> items = new ArrayList<>();

            for (String sid : signerIds) {
                for (String did : documentIds) {
                    if (tpl.getQualification()!=null && tpl.getQualification().isEnable()) {
                        var s = new SignerRequirementsDTO();
                        s.setSignerId(sid); s.setDocumentId(did);
                        var q = new SignerRequirementsDTO.QualificationDTO();
                        q.setEnable(true); 
                        q.setAction(action);
                        q.setRole(role);
                        s.setQualification(q);
                        items.add(s);
                    }
                    if (tpl.getAuthentication()!=null && tpl.getAuthentication().isEnable()) {
                        for (String aType : auths) {
                            var s = new SignerRequirementsDTO();
                            s.setSignerId(sid); s.setDocumentId(did);
                            var a = new SignerRequirementsDTO.AuthenticationDTO();
                            a.setEnable(true); a.setAuth(aType);
                            s.setAuthentication(a);
                            items.add(s);
                        }
                    }
                    if (tpl.getRubric()!=null && tpl.getRubric().isEnable()) {
                        var s = new SignerRequirementsDTO();
                        s.setSignerId(sid); s.setDocumentId(did);
                        var r = new SignerRequirementsDTO.RubricDTO();
                        r.setEnable(true); r.setPages(pages);
                        s.setRubric(r);
                        items.add(s);
                    }
                }
            }

            ar.setSigners(items);
            RequirementsService.BatchResult br = requirementsService.addAll(envelopeId, ar);

            var sum = new SignSendResponse.RequirementSummary();
            sum.setTotalSuccess(br.successCount());
            sum.setTotalFail(br.failureCount());
            sum.setSuccess(br.getSuccesses());
            sum.setFail(br.getFailures());
            resp.setRequirements(sum);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            errors.add(err("requirements", e.getResponseBodyAsString(), e.getStatusCode().value()));
        } catch (Exception e) {
            errors.add(err("requirements", e));
        }
        return true;
    }

    private void maybeActivate(String envelopeId,
                               boolean activate,
                               boolean templateEnabled,
                               SignSendResponse resp,
                               List<SignSendResponse.ErrorItem> errors) {
        if (!activate) return;

        boolean reqExecuted = resp.getRequirements() != null
                && (resp.getRequirements().getTotalSuccess() + resp.getRequirements().getTotalFail() > 0);

        if (templateEnabled && !reqExecuted) {
            errors.add(err("activate",
                    "Envelope não ativado: existem requisitos pendentes (adicione qualificações/autenticações/rubricas antes da ativação)."));
            return;
        }

        try {
            envelopeService.activateEnvelope(envelopeId);
            resp.setActivated(true);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            errors.add(err("activate", e.getResponseBodyAsString(), e.getStatusCode().value()));
        } catch (Exception e) {
            errors.add(err("activate", e));
        }
    }

    private void maybeNotify(String envelopeId,
                             boolean notify,
                             SignSendResponse resp,
                             List<SignSendResponse.ErrorItem> errors) {
        if (!notify) return;

        if (!resp.isActivated()) {
            errors.add(err("notify", "Notificação não enviada: envelope não está ativo."));
            return;
        }

        try {
            Map<String, Object> notifyResp = signatoryService.notifyAllSigners(envelopeId, null);
            resp.setNotifications(notifyResp);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            errors.add(err("notify", e.getResponseBodyAsString(), e.getStatusCode().value()));
        } catch (Exception e) {
            errors.add(err("notify", e));
        }
    }

    private void finalizePersistence(String envelopeId,
                                     String tenant,
                                     List<String> signerEmailsFromReq,
                                     List<String> signerIds,
                                     List<String> documentIds,
                                     LinkedHashSet<String> authsApplied,
                                     SignSendResponse resp) {
        if (envelopeId == null || envelopeId.isBlank()) {
            envelopeId = extractEnvelopeId(resp);
        }
        if (envelopeId != null && !envelopeId.isBlank()) {
            tryInitAndMerge(envelopeId, tenant, signerEmailsFromReq, signerIds, documentIds, authsApplied, resp);
        } else {
            log.warn("[send] envelopeId não extraído. Resp: {}", safeJson(resp));
        }
    }


    /** Criar envelope (isolado) — usa header X-Tenant */
    public Object createEnvelope(EnvelopeCreateRequestDTO body) {
        applyAuthFromHeaderOrThrow();
        return envelopeService.createEnvelope(body);
    }

    /** Upload de documento (isolado) — usa header X-Tenant */
    public Object uploadDocument(String envelopeId, UploadDocumentRequestDTO req) throws IOException {
        applyAuthFromHeaderOrThrow();

        if (req.getFileBase64() != null) {
            req.setFileBase64(sanitizeBase64(req.getFileBase64()));
        }
        if (req.getFooter() != null) {
            if (req.getFileBase64() == null || req.getFileBase64().isBlank()) {
                throw http400("fileBase64 vazio para carimbar");
            }
            byte[] original = decodeB64(req.getFileBase64());
            byte[] stamped  = pdfStampService.addFooterLeftLastPage(original, req.getFooter());
            req.setFileBase64(encodeB64(stamped));
        }

        Map<String,Object> body = new LinkedHashMap<>();
        body.put("fileName", (req.getFileName()!=null && !req.getFileName().isBlank()) ? req.getFileName() : "documento.pdf");
        body.put("fileBase64", req.getFileBase64());
        body.put("contentType", "application/pdf");

        Object result = documentsService.uploadDocuments(envelopeId, body);
        tryMergeDocIds(envelopeId, result);
        return result;
    }

    /** Adicionar signatários (isolado) — usa header X-Tenant */
    public Object addSigners(String envelopeId, JsonNode body) {
        applyAuthFromHeaderOrThrow();

        Object result = signatoryService.addSignersFlexible(envelopeId, body);
        tryMergeSigners(envelopeId, body);
        return normalizeMultiStatus(result);
    }

    /** Adicionar requirements (isolado) — usa header X-Tenant */
    public Object addRequirements(String envelopeId, AddRequirementsRequestDTO dto) {
        applyAuthFromHeaderOrThrow();

        var batch = requirementsService.addAll(envelopeId, dto);
        return Map.of(
                "total_success", batch.successCount(),
                "total_fail", batch.failureCount(),
                "success", batch.getSuccesses(),
                "fail", batch.getFailures()
        );
    }

    /** Notificar todos (isolado) — usa header X-Tenant */
    public Object notifyAll(String envelopeId, Map<String,Object> body) {
        applyAuthFromHeaderOrThrow();

        var result = signatoryService.notifyAllSigners(envelopeId, body);
        envelopePersistenceService.setNotified(envelopeId, true);
        return result;
    }

    /** Notificar um (isolado) — usa header X-Tenant */
    public Object notifyOne(String envelopeId, String signerId, Map<String,Object> body) {
        applyAuthFromHeaderOrThrow();

        var result = signatoryService.notifySigner(envelopeId, signerId, body);
        envelopePersistenceService.setNotified(envelopeId, true);
        return result;
    }

    /**
     * Listagem de documentos + sincronização local — usa header X-Tenant.
     * Não retorna "original" quando o doc estiver "closed", mas persiste no banco.
     */
    public Object listEnvelopeDocuments(String envelopeId, Integer page, Integer size, String tenantParam, boolean signedBase64) {
        applyAuthFromHeaderOrThrow();
        String tenant = readTenantHeaderOrThrow();

        var docs = consultEnvelopsService.listEnvelopeDocumentsProcessed(envelopeId, page, size, signedBase64);
        envelopePersistenceService.updateFromDocumentsFetch(envelopeId, tenant, docs);

        var redacted = new ArrayList<Map<String,Object>>();
        IntStream.range(0, docs.size()).forEach(i -> {
            var d = docs.get(i);
            var a = d.attributes();
            boolean closed = (a != null && "closed".equalsIgnoreCase(a.status()));
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("index", i+1);
            m.put("id", d.id());
            if (!closed) {
                m.put("original", d.original());
            }
            m.put("signed", d.signed());
            m.put("attributes", d.attributes());
            redacted.add(m);
        });
        return redacted;
    }

    /** Ativar envelope (isolado) — usa header X-Tenant */
    public Object activateEnvelope(String envelopeId) {
        applyAuthFromHeaderOrThrow();
        return envelopeService.activateEnvelope(envelopeId);
    }

    private String resolveTenantByName(String name) {
        if (name == null) return "software";
        return name.equalsIgnoreCase("Boxware") ? "boxware" : "software";
    }
    private String resolveTenantFromParam(String t) {
        return (t == null || t.isBlank()) ? "software" : t.toLowerCase();
    }
    private String resolveToken(String tenant) {
        return ("boxware".equals(tenant) || "other".equals(tenant)) ? tokenOther : tokenSoftware;
    }

    /** Remove "data:*;base64," e espaços/quebras de linha do Base64 */
    private static String sanitizeBase64(String b64) {
        if (b64 == null) return null;
        int comma = b64.indexOf(',');
        String raw = (comma >= 0) ? b64.substring(comma + 1) : b64;
        return raw.replaceAll("\\s+", "");
    }
    private static byte[] decodeB64(String b64) {
        try { return Base64.getDecoder().decode(b64); }
        catch (IllegalArgumentException iae) { throw http400("Base64 inválido: " + iae.getMessage()); }
    }
    private static String encodeB64(byte[] b) { return Base64.getEncoder().encodeToString(b); }

    private static RuntimeException http400(String msg) {
        return new HttpClientErrorException(HttpStatus.BAD_REQUEST, msg);
    }
    private static RuntimeException runtime(Exception e) {
        return (e instanceof RuntimeException re) ? re : new RuntimeException(e.getMessage(), e);
    }
    private static String safeJson(Object o) {
        try { return OM.writeValueAsString(o); } catch (Exception e) { return String.valueOf(o); }
    }

    private static List<Map<String, Object>> safeList(Object o) {
        if (o instanceof List<?> l) {
            return (List<Map<String, Object>>) l.stream()
                    .filter(Map.class::isInstance)
                    .collect(Collectors.toList());
        }
        return List.of();
    }
    private static String extractDataId(Map<String, Object> m) {
        if (m == null) return null;
        Object data = m.get("data");
        if (data instanceof Map<?, ?> dm) {
            Object id = ((Map<?, ?>) dm).get("id");
            return id != null ? id.toString() : null;
        }
        Object id = m.get("id");
        return id != null ? id.toString() : null;
    }
    private static String guessFilename(Map<String, Object> item) {
        Object data = item.get("data");
        if (data instanceof Map<?, ?> dm) {
            Object attrs = ((Map<?, ?>) dm).get("attributes");
            if (attrs instanceof Map<?, ?> am) {
                Object fn = am.get("filename");
                if (fn != null) return fn.toString();
            }
        }
        return null;
    }
    private static boolean hasTemplateEnabled(RequirementTemplate tpl) {
        if (tpl == null) return false;
        return (tpl.getQualification() != null && tpl.getQualification().isEnable())
                || (tpl.getAuthentication() != null && tpl.getAuthentication().isEnable())
                || (tpl.getRubric() != null && tpl.getRubric().isEnable());
    }
    private static List<String> extractSignerIds(Object result) {
        List<String> out = new ArrayList<>();
        if (result == null) return out;

        if (result instanceof Map<?, ?> m) {
            Object items = m.get("items");
            if (items instanceof List<?> lst) {
                for (Object it : lst) {
                    String id = findIdDeep(it);
                    if (id != null) out.add(id);
                }
                if (!out.isEmpty()) return out;
            }
            Object results = m.get("results");
            if (results instanceof List<?> lst) {
                for (Object it : lst) {
                    String id = findIdDeep(it);
                    if (id != null) out.add(id);
                }
                if (!out.isEmpty()) return out;
            }
            String id = findIdDeep(m);
            if (id != null) out.add(id);
            return out;
        }

        if (result instanceof List<?> lst) {
            for (Object it : lst) {
                String id = findIdDeep(it);
                if (id != null) out.add(id);
            }
        }
        return out;
    }
    private static String findIdDeep(Object node) {
        if (node == null) return null;
        if (node instanceof Map<?, ?> m) {
            Object id = m.get("id");
            if (id != null && !(id instanceof Map<?, ?>)) return id.toString();

            Object data = m.get("data");
            String v = findIdDeep(data);
            if (v != null) return v;

            for (String k : new String[] { "response", "result", "signer", "body", "payload" }) {
                Object child = m.get(k);
                v = findIdDeep(child);
                if (v != null) return v;
            }
        }
        return null;
    }

    /** Extração robusta do envelopeId a partir do DTO de resposta */
    private String extractEnvelopeId(SignSendResponse resp) {
        if (resp == null) return null;
        try {
            if (resp.getEnvelopeId()!=null && !resp.getEnvelopeId().isBlank()) return resp.getEnvelopeId();
        } catch (Throwable ignored) {}
        try {
            var root = OM.valueToTree(resp);
            String v;
            v = text(root.at("/envelopeId")); if (n(v)) return v;
            v = text(root.at("/envelope/id")); if (n(v)) return v;
            v = text(root.at("/data/envelope/id")); if (n(v)) return v;
            String type = text(root.at("/data/type"));
            if ("envelope".equalsIgnoreCase(type)) {
                v = text(root.at("/data/id")); if (n(v)) return v;
            }
            return findIdRecursive("root", root);
        } catch (Exception e) {
            log.warn("[extractEnvelopeId] falhou: {}", e.getMessage());
            return null;
        }
    }
    private static boolean n(String s) { return s!=null && !s.isBlank(); }
    private static String text(JsonNode n) { return (n==null || n.isNull()||n.isMissingNode()) ? null : n.asText(null); }
    private static String findIdRecursive(String parent, JsonNode node) {
        if (node == null || node.isNull()) return null;
        if (node.isObject()) {
            var it = node.fields();
            String typeValue = null, idValue = null;
            while (it.hasNext()) {
                var e = it.next();
                String k = e.getKey();
                var v = e.getValue();
                if ("envelopeId".equalsIgnoreCase(k) || "envelope_id".equalsIgnoreCase(k)) {
                    String t = v.asText(null); if (n(t)) return t.trim();
                }
                if ("type".equalsIgnoreCase(k)) typeValue = v.asText(null);
                if ("id".equalsIgnoreCase(k))   idValue  = v.asText(null);
            }
            if ("envelope".equalsIgnoreCase(typeValue) && n(idValue)) return idValue.trim();
            if (parent.toLowerCase().contains("envelope") && n(idValue)) return idValue.trim();

            it = node.fields();
            while (it.hasNext()) {
                var e = it.next();
                String found = findIdRecursive(e.getKey(), e.getValue());
                if (found != null) return found;
            }
        } else if (node.isArray()) {
            for (var v: node) {
                String found = findIdRecursive(parent, v);
                if (found != null) return found;
            }
        }
        return null;
    }

    private void tryInitAndMerge(String envelopeId,
                                 String tenant,
                                 List<String> signerEmailsFromReq,
                                 List<String> signerIds,
                                 List<String> documentIds,
                                 LinkedHashSet<String> authsApplied,
                                 SignSendResponse resp) {
        try {
            envelopePersistenceService.initRecord(envelopeId, tenant, List.of(), List.of(), List.of());
        } catch (Exception e) {
            log.error("[send] initRecord falhou: {}", e.getMessage(), e);
        }

        try {
            if (!documentIds.isEmpty()) {
                envelopePersistenceService.mergeDocumentIds(envelopeId, documentIds);
            }
            if (!signerIds.isEmpty() || !signerEmailsFromReq.isEmpty()) {
                envelopePersistenceService.mergeSigners(envelopeId, signerIds, signerEmailsFromReq);
            }
            if (!authsApplied.isEmpty()) {
                envelopePersistenceService.mergeAuthenticated(envelopeId, new ArrayList<>(authsApplied));
            }
        } catch (Exception e) {
            log.error("[send] merge falhou: {}", e.getMessage(), e);
        }
    }

    /** Helper para extrair docId de um POST /documents e mesclar no DB */
    private void tryMergeDocIds(String envelopeId, Object result) {
        try {
            JsonNode root = OM.valueToTree(result);
            String docId = null;
            JsonNode idNode = root.at("/data/id");
            if (idNode.isMissingNode() || idNode.isNull()) idNode = root.at("/id");
            if (!idNode.isMissingNode() && !idNode.isNull()) docId = idNode.asText();
            if (n(docId)) envelopePersistenceService.mergeDocumentIds(envelopeId, List.of(docId));
        } catch (Exception ignored) { /* noop */ }
    }

    /** Helper para coletar emails/ids do body de signers e mesclar no DB */
    private void tryMergeSigners(String envelopeId, JsonNode body) {
        try {
            List<String> emails = new ArrayList<>(), ids = new ArrayList<>();
            if (body!=null && body.isArray()) {
                for (JsonNode n : body) {
                    String email = n.hasNonNull("email") ? n.get("email").asText(null) : null;
                    String id    = n.hasNonNull("id")    ? n.get("id").asText(null)    : null;
                    JsonNode signer = n.get("signer");
                    if (email==null && signer!=null && signer.hasNonNull("email")) email = signer.get("email").asText(null);
                    if (id==null && signer!=null && signer.hasNonNull("id")) id = signer.get("id").asText(null);
                    if (n(email)) emails.add(email);
                    if (n(id)) ids.add(id);
                }
            }
            if (!emails.isEmpty() || !ids.isEmpty())
                envelopePersistenceService.mergeSigners(envelopeId, ids, emails);
        } catch (Exception ignored) { /* noop */ }
    }

    private Object normalizeMultiStatus(Object result) {
        // Se quiser mapear 200/207/400 conforme agregação, faça aqui
        return result;
    }

    private static SignSendResponse.ErrorItem err(String step, Exception e) {
        SignSendResponse.ErrorItem it = new SignSendResponse.ErrorItem();
        it.setStep(step);
        it.setDetail(e != null ? String.valueOf(e.getMessage()) : null);
        return it;
    }
    private static SignSendResponse.ErrorItem err(String step, String upstream, Integer http) {
        SignSendResponse.ErrorItem it = new SignSendResponse.ErrorItem();
        it.setStep(step);
        it.setDetail("Upstream error");
        it.setHttp(http);
        it.setUpstream(upstream);
        return it;
    }
    private static SignSendResponse.ErrorItem err(String step, String message) {
        SignSendResponse.ErrorItem it = new SignSendResponse.ErrorItem();
        it.setStep(step);
        it.setDetail(message);
        return it;
    }

    private void applyAuthFromHeaderOrThrow() {
        String tenant = readTenantHeaderOrThrow();
        authContext.setApiKeyOverride(resolveToken(tenant));
        try { MDC.put("tenant", tenant); } catch (Throwable ignored) {}
    }

    private String readTenantHeaderOrThrow() {
        var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            throw new IllegalStateException("Contexto HTTP ausente para ler X-Tenant.");
        }
        HttpServletRequest req = attrs.getRequest();
        String raw = req.getHeader("X-Tenant");

        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Header X-Tenant obrigatório: use 'software' ou 'other'.");
        }
        String t = raw.trim().toLowerCase();
        if ("boxware".equals(t)) t = "other";
        if (!"software".equals(t) && !"other".equals(t)) {
            throw new IllegalArgumentException("Valor inválido para X-Tenant. Aceitos: 'software' ou 'other'.");
        }
        return t;
    }
}
