package br.com.desafio.gestaopessoas.domain.department;

import br.com.desafio.gestaopessoas.domain.RequiredText;

public record CostCenterCode(String value) {
    public CostCenterCode { value = RequiredText.normalize(value, "costCenterCode", 50); }
}
