package br.com.software.sign.api.dto;

import java.util.List;

public class AddRequirementsRequestDTO {

    private List<SignerRequirementsDTO> signers;

    public List<SignerRequirementsDTO> getSigners() {
        return signers;
    }

    public void setSigners(List<SignerRequirementsDTO> signers) {
        this.signers = signers;
    }
}
