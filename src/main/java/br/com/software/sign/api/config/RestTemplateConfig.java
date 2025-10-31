package br.com.software.sign.api.config;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        // Pool de conexões HTTP para reduzir handshakes/TCP
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(100);
        cm.setDefaultMaxPerRoute(20);

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(2_000)
                .setSocketTimeout(10_000)
                .setConnectionRequestTimeout(2_000)
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultRequestConfig(requestConfig)
                .disableCookieManagement()
                .build();

        HttpComponentsClientHttpRequestFactory rf = new HttpComponentsClientHttpRequestFactory(httpClient);
        rf.setConnectTimeout(2_000);
        rf.setReadTimeout(10_000);
        rf.setConnectionRequestTimeout(2_000);

        RestTemplate rt = new RestTemplate(rf);

        // garante suporte a PDF/binário
        ByteArrayHttpMessageConverter bin = new ByteArrayHttpMessageConverter();
        bin.setSupportedMediaTypes(List.of(
                MediaType.APPLICATION_PDF,
                MediaType.APPLICATION_OCTET_STREAM
        ));

        rt.getMessageConverters().removeIf(c -> c instanceof ByteArrayHttpMessageConverter);
        rt.getMessageConverters().add(0, bin);

        return rt;
    }
}
