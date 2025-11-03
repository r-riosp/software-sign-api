package br.com.software.sign.api.service.partner.clicksign;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DefaultOptionService {

    // valores default
    private static final boolean DEFAULT_ACTIVATE  = true;
    private static final boolean DEFAULT_NOTIFYALL = true;
    private static final String  DEFAULT_AUTH      = "email,handwritten";
    private static final String  DEFAULT_ROLE      = "party";
    private static final String  DEFAULT_RUB_PAGES = "all";
    private static final String  DEFAULT_SIGN_AS   = "sign";

    public Boolean getActivate(Boolean value) {
        return value != null ? value : DEFAULT_ACTIVATE;
    }

    public Boolean getNotifyAll(Boolean value) {
        return value != null ? value : DEFAULT_NOTIFYALL;
    }

    public String getAuth(String auth) {
        return (auth != null && !auth.isBlank()) ? auth : DEFAULT_AUTH;
    }

    public String getRole(String role) {
        return (role != null && !role.isBlank()) ? role : DEFAULT_ROLE;
    }

    public String getRubricPages(String pages) {
        return (pages != null && !pages.isBlank()) ? pages : DEFAULT_RUB_PAGES;
    }

    public String getSignAs(String signAs) {
        return (signAs != null && !signAs.isBlank()) ? signAs : DEFAULT_SIGN_AS;
    }
}

