package br.com.software.sign.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DocumentOutDTO(
        String id,
        String original,
        String signed,
        Attributes attributes
) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Attributes(
            String status,
            String filename,
            String created,
            String modified
    ) {}
}
