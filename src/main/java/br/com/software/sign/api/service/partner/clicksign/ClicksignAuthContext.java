package br.com.software.sign.api.service.partner.clicksign;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
public class ClicksignAuthContext {

    private String apiKeyOverride;

    public void setApiKeyOverride(String apiKeyOverride) {
        this.apiKeyOverride = apiKeyOverride;
    }

    public String resolve(String defaultApiKey) {
        if (apiKeyOverride != null && !apiKeyOverride.isBlank()) return apiKeyOverride;
        if (defaultApiKey != null && !defaultApiKey.isBlank())   return defaultApiKey;
        throw new IllegalStateException("Nenhuma Clicksign API key definida para este request. Informe ?name=software (ou outro) no endpoint.");
    }
}
