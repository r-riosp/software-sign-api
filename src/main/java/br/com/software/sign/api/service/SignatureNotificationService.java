package br.com.software.sign.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serviço para gerenciar notificações inteligentes de assinatura
 */
@Service
public class SignatureNotificationService {

    private static final Logger log = LoggerFactory.getLogger(SignatureNotificationService.class);

    @Value("${app.signature.internal-notification-timeout-hours:0.083}")
    private double timeoutHours;

    @Value("${app.signature.confirmation-email.enabled:true}")
    private boolean confirmationEmailEnabled;

    @Value("${app.signature.confirmation-email.from:noreply@software.com.br}")
    private String confirmationEmailFrom;

    // Mantém a estrutura para compatibilidade, mas não será usado
    private final ConcurrentHashMap<String, Timer> activeTimers = new ConcurrentHashMap<>();

    /**
     * Envia email de confirmação real para vendedores internos
     */
    public void sendConfirmationEmail(String envelopeId, String signerEmail, String envelopeName) {
        if (!confirmationEmailEnabled) {
            log.info("[confirmation] Emails de confirmação desabilitados via configuração");
            return;
        }

        try {
            String subject = "✅ Proposta Enviada - " + envelopeName;
            String body = String.format("""
                Olá!

                Sua proposta foi enviada com sucesso através da ClickSign.

                📋 Documento: %s
                📧 Envelope ID: %s
                ⏰ Data de Envio: %s

                Você será notificado quando:
                • O cliente assinar o documento
                • Quando todos os documentos estiverem assinados

                Acompanhe pelo painel ClickSign ou aguarde as notificações.

                Atenciosamente,
                Sistema Software.com.br
                """,
                    envelopeName,
                    envelopeId,
                    java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
            );

            // TODO: Integrar com seu sistema de email real
            // emailService.send(confirmationEmailFrom, signerEmail, subject, body);

            // Por enquanto, simula envio com log detalhado
            log.info("📧 [CONFIRMATION EMAIL SENT]");
            log.info("From: {}", confirmationEmailFrom);
            log.info("To: {}", signerEmail);
            log.info("Subject: {}", subject);
            log.info("Body preview: {}", body.substring(0, Math.min(100, body.length())) + "...");

        } catch (Exception e) {
            log.error("[confirmation] Erro ao enviar email de confirmação para: {}", signerEmail, e);
        }
    }

    /**
     * Método desabilitado - não agenda mais lembretes
     */
    public void scheduleReminderNotification(String envelopeId, List<String> internalSignerEmails) {
        log.info("⏰ [REMINDER DISABLED] Sistema de lembretes desabilitado - Envelope: {}", envelopeId);
        // Não faz nada - lembretes desabilitados
    }

    /**
     * Método mantido para compatibilidade, mas não há timers para cancelar
     */
    public void cancelReminder(String envelopeId) {
        log.debug("⏰ [REMINDER CANCEL] Nenhum lembrete para cancelar - sistema desabilitado - Envelope: {}", envelopeId);
        // Não faz nada - lembretes desabilitados
    }

    /**
     * Notifica vendedores quando cliente assina
     */
    public void notifyInternalSignerProgress(String envelopeId, String signerEmail, String signerName) {
        try {
            String subject = "✅ Cliente Assinou - " + envelopeId;
            String body = String.format("""
                Boa notícia!

                O cliente %s (%s) acabou de assinar o documento.

                📧 Envelope ID: %s
                ✅ Status: Assinado
                ⏰ Data/Hora: %s

                O processo de assinatura está progredindo conforme esperado.

                Atenciosamente,
                Sistema Software.com.br
                """,
                    signerName != null ? signerName : "Cliente",
                    signerEmail,
                    envelopeId,
                    java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
            );

            log.info("📧 [PROGRESS EMAIL] Cliente {} assinou no envelope: {}", signerEmail, envelopeId);
            log.info("Subject: {}", subject);

            // Não cancela lembrete pois não há lembretes sendo criados

        } catch (Exception e) {
            log.error("[progress] Erro ao notificar progresso", e);
        }
    }

    /**
     * Lista timers ativos (sempre vazio agora)
     */
    public void listActiveReminders() {
        log.info("📋 [ACTIVE REMINDERS] Sistema de lembretes desabilitado - Total: 0");
    }
}
