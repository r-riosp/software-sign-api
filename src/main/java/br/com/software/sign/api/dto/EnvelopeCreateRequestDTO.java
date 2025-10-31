package br.com.software.sign.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EnvelopeCreateRequestDTO {

    @NotBlank(message = "name é obrigatório")
    @Size(max = 255, message = "name deve ter no máximo 255 caracteres")
    private String name;

    @Pattern(
            regexp = "^$|^(\\d{4}-\\d{2}-\\d{2}([T ]\\d{2}:\\d{2}(:\\d{2})?(\\.\\d+)?(Z|[+-]\\d{2}:?\\d{2})?)?)$",
            message = "deadlineAt deve estar em ISO-8601 (ex: 2025-12-31T23:59:59Z)"
    )
    private String deadlineAt; // Default definido para D+30 ( 1 mês de limite )

    private String  locale = "pt-BR";

    private Boolean autoClose = true;

    @Pattern(regexp = "^$|^(1|2|3|7|14)$", message = "remindInterval deve ser um de: 1,2,3,7,14")
    private String  remindInterval = "1";

    private Boolean blockAfterRefusal;
    private String  defaultSubject;
    private String  defaultMessage;
    private String  folderId;

    public String  getName()              { return name; }
    public String  getDeadlineAt()        { return deadlineAt; }
    public String  getLocale()            { return locale; }
    public Boolean getAutoClose()         { return autoClose; }
    public String  getRemindInterval()    { return remindInterval; }
    public Boolean getBlockAfterRefusal() { return blockAfterRefusal; }
    public String  getDefaultSubject()    { return defaultSubject; }
    public String  getDefaultMessage()    { return defaultMessage; }
    public String  getFolderId()          { return folderId; }
}