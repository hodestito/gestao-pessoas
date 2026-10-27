package br.com.desafio.gestaopessoas.application;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/** Regras comuns de paginação das consultas de colaboradores. */
public final class EmployeePagination {
    public static final int MAX_SIZE = 100;
    private EmployeePagination() {}

    public static PageRequest create(int page, int size) {
        if (page < 0) throw new IllegalArgumentException("page deve ser maior ou igual a zero");
        if (size < 1 || size > MAX_SIZE) throw new IllegalArgumentException("size deve estar entre 1 e 100");
        if ((long) page * size > Integer.MAX_VALUE) throw new IllegalArgumentException("deslocamento de página excede o limite suportado");
        return PageRequest.of(page, size, Sort.by("createdAt").ascending().and(Sort.by("id").ascending()));
    }
}
