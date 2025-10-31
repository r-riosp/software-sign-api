package br.com.software.sign.api.controller;

import br.com.software.sign.api.service.SignService;
import br.com.software.sign.api.service.SignatureNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller para receber webhooks do ClickSign
 */
@RestController
@RequestMapping("/api/v1/webhook")
public class ClickSignWebhookController {

    private static final Logger log = LoggerFactory.getLogger(ClickSignWebhookController.class);

    @Autowired
    private SignService signService;

    @Autowired
    private SignatureNotificationService notificationService;

    /**
     * Endpoint para receber webhooks do ClickSign
     * Configurar no painel ClickSign: https://sua-api.com/api/v1/webhook/clicksign
     */
    @PostMapping("/clicksign")
    public ResponseEntity<String> handleClickSignWebhook(@RequestBody Map<String, Object> payload) {
        try {
            String event = (String) payload.get("event");
            String envelopeId = extractEnvelopeId(payload);

            log.info("[webhook] Evento recebido: {} para envelope: {}", event, envelopeId);

            switch (event) {
                case "envelope.finished":
                    handleEnvelopeFinished(envelopeId, payload);
                    break;
                case "envelope.signed":
                    handleEnvelopeSigned(envelopeId, payload);
                    break;
                case "signer.signed":
                    handleSignerSigned(envelopeId, payload);
                    break;
                default:
                    log.info("[webhook] Evento {} não tratado", event);
            }

            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("[webhook] Erro processando webhook", e);
            return ResponseEntity.status(500).body("Error");
        }
    }

    /**
     * Trata evento de envelope concluído (todos assinaram)
     */
    private void handleEnvelopeFinished(String envelopeId, Map<String, Object> payload) {
        try {
            log.info("[webhook] 🎉 Envelope {} CONCLUÍDO. Cancelando lembretes pendentes.", envelopeId);

            // Cancela lembretes agendados já que todos assinaram
            notificationService.cancelReminder(envelopeId);

            // O ClickSign já envia notificação final automática para todos
            log.info("[webhook] Todos os signatários receberam notificação final automática do ClickSign.");

        } catch (Exception e) {
            log.error("[webhook] Erro ao processar envelope finalizado: {}", envelopeId, e);
        }
    }

    /**
     * Trata evento de envelope assinado (pode ser parcial)
     */
    private void handleEnvelopeSigned(String envelopeId, Map<String, Object> payload) {
        try {
            log.info("[webhook] Envelope {} teve nova assinatura", envelopeId);

            // Verificar se há vendedores internos que precisam ser notificados
            // sobre o progresso da assinatura

        } catch (Exception e) {
            log.error("[webhook] Erro ao processar assinatura do envelope: {}", envelopeId, e);
        }
    }

    /**
     * Trata evento de signatário específico que assinou
     */
    private void handleSignerSigned(String envelopeId, Map<String, Object> payload) {
        try {
            String signerId = extractSignerId(payload);
            String signerEmail = extractSignerEmail(payload);
            String signerName = extractSignerName(payload);

            log.info("[webhook] ✅ Signatário {} ({}) assinou no envelope {}", signerId, signerEmail, envelopeId);

            // NOTIFICAR VENDEDORES INTERNOS SOBRE PROGRESSO
            if (signerEmail != null && !isInternalEmail(signerEmail)) {
                // Se foi um cliente externo que assinou, notifica vendedores internos
                notificationService.notifyInternalSignerProgress(envelopeId, signerEmail, signerName);
                log.info("[webhook] Vendedores internos notificados sobre assinatura do cliente: {}", signerEmail);
            }

        } catch (Exception e) {
            log.error("[webhook] Erro ao processar assinatura do signatário no envelope: {}", envelopeId, e);
        }
    }

    /**
     * Verifica se email é interno (vendedor)
     */
    private boolean isInternalEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        String emailLower = email.toLowerCase().trim();
        return emailLower.endsWith("@software.com.br") || emailLower.endsWith("@boxware.com.br");
    }

    /**
     * Extrai envelope ID do payload do webhook
     */
    private String extractEnvelopeId(Map<String, Object> payload) {
        try {
            // Estrutura típica do webhook ClickSign
            Map<String, Object> data = (Map<String, Object>) payload.get("data");
            if (data != null) {
                Object envelopeObj = data.get("envelope");
                if (envelopeObj instanceof Map) {
                    return (String) ((Map<?, ?>) envelopeObj).get("id");
                }
                // Fallback direto
                return (String) data.get("id");
            }
            return null;
        } catch (Exception e) {
            log.warn("[webhook] Falha ao extrair envelope ID do payload", e);
            return null;
        }
    }

    /**
     * Extrai signer ID do payload do webhook
     */
    private String extractSignerId(Map<String, Object> payload) {
        try {
            Map<String, Object> data = (Map<String, Object>) payload.get("data");
            if (data != null) {
                Object signerObj = data.get("signer");
                if (signerObj instanceof Map) {
                    return (String) ((Map<?, ?>) signerObj).get("id");
                }
            }
            return null;
        } catch (Exception e) {
            log.warn("[webhook] Falha ao extrair signer ID do payload", e);
            return null;
        }
    }

    /**
     * Extrai email do signatário do payload
     */
    private String extractSignerEmail(Map<String, Object> payload) {
        try {
            Map<String, Object> data = (Map<String, Object>) payload.get("data");
            if (data != null) {
                Object signerObj = data.get("signer");
                if (signerObj instanceof Map) {
                    return (String) ((Map<?, ?>) signerObj).get("email");
                }
            }
            return null;
        } catch (Exception e) {
            log.warn("[webhook] Falha ao extrair email do signatário", e);
            return null;
        }
    }

    /**
     * Extrai nome do signatário do payload
     */
    private String extractSignerName(Map<String, Object> payload) {
        try {
            Map<String, Object> data = (Map<String, Object>) payload.get("data");
            if (data != null) {
                Object signerObj = data.get("signer");
                if (signerObj instanceof Map) {
                    return (String) ((Map<?, ?>) signerObj).get("name");
                }
            }
            return null;
        } catch (Exception e) {
            log.warn("[webhook] Falha ao extrair nome do signatário", e);
            return null;
        }
    }

    /**
     * Endpoint de teste para verificar se o webhook está funcionando
     */
    @GetMapping("/clicksign/test")
    public ResponseEntity<String> testWebhook() {
        return ResponseEntity.ok("Webhook ClickSign está funcionando!");
    }

    /**
     * Endpoint para debug - lista lembretes ativos
     */
    @GetMapping("/clicksign/debug/reminders")
    public ResponseEntity<String> listActiveReminders() {
        notificationService.listActiveReminders();
        return ResponseEntity.ok("Verifique os logs para ver lembretes ativos");
    }

    /**
     * Endpoint para debug - simula assinatura de cliente
     */
    @PostMapping("/clicksign/debug/simulate-signature")
    public ResponseEntity<String> simulateSignature(@RequestParam String envelopeId,
                                                    @RequestParam String clientEmail,
                                                    @RequestParam(required = false) String clientName) {
        try {
            notificationService.notifyInternalSignerProgress(envelopeId, clientEmail, clientName);
            return ResponseEntity.ok("Simulação de assinatura executada para " + clientEmail);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erro: " + e.getMessage());
        }
    }
}
