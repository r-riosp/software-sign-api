package br.com.software.sign.api.dto;

import lombok.Data;

@Data
public class FooterDTO {
    private String inscricaoEstadual;
    private String cnpj;
    private String dataCorte;
    private String razaoSocial;
    private String responsavelLicenca;
    private String emailNf;

    public FooterDTO() {}

    public FooterDTO(String inscricaoEstadual, String cnpj, String dataCorte,
                     String razaoSocial, String responsavelLicenca, String emailNf) {
        this.inscricaoEstadual = inscricaoEstadual;
        this.cnpj = cnpj;
        this.dataCorte = dataCorte;
        this.razaoSocial = razaoSocial;
        this.responsavelLicenca = responsavelLicenca;
        this.emailNf = emailNf;
    }

    public String getInscricaoEstadual() {
        return inscricaoEstadual;
    }

    public void setInscricaoEstadual(String inscricaoEstadual) {
        this.inscricaoEstadual = inscricaoEstadual;
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public String getDataCorte() {
        return dataCorte;
    }

    public void setDataCorte(String dataCorte) {
        this.dataCorte = dataCorte;
    }

    public String getRazaoSocial() {
        return razaoSocial;
    }

    public void setRazaoSocial(String razaoSocial) {
        this.razaoSocial = razaoSocial;
    }

    public String getResponsavelLicenca() {
        return responsavelLicenca;
    }

    public void setResponsavelLicenca(String responsavelLicenca) {
        this.responsavelLicenca = responsavelLicenca;
    }

    public String getEmailNf() {
        return emailNf;
    }

    public void setEmailNf(String emailNf) {
        this.emailNf = emailNf;
    }
}