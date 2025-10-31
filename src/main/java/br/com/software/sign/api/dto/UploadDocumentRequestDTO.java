package br.com.software.sign.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

public class UploadDocumentRequestDTO {

    @NotBlank
    @JsonAlias({"filename"})
    private String fileName;

    @NotBlank
    @JsonAlias({"contentBase64","content_base64"}) // aceita "contentBase64"
    private String fileBase64;

    @Valid
    private FooterDTO footer;

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileBase64() { return fileBase64; }
    public void setFileBase64(String fileBase64) { this.fileBase64 = fileBase64; }

    public FooterDTO getFooter() { return footer; }
    public void setFooter(FooterDTO footer) { this.footer = footer; }
}
