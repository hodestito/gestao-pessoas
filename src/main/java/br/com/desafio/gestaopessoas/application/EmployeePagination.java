package br.com.desafio.gestaopessoas.application;

/** Parâmetros e limites de paginação das consultas de colaboradores. */
public record EmployeePagination(int page, int size) {
    public static final int MAX_SIZE = 100;

    public EmployeePagination {
        if (page < 0) throw new IllegalArgumentException("page deve ser maior ou igual a zero");
        if (size < 1 || size > MAX_SIZE) throw new IllegalArgumentException("size deve estar entre 1 e 100");
        if ((long) page * size > Integer.MAX_VALUE) throw new IllegalArgumentException("deslocamento de página excede o limite suportado");
    }

    public static EmployeePagination create(int page, int size) {
        return new EmployeePagination(page, size);
    }
}
