package br.com.desafio.gestaopessoas.application;

import java.util.List;

/** Resultado de consulta independente da biblioteca de persistência. */
public record PageResult<T>(List<T> content, int page, int size, long totalElements) {
    public PageResult {
        content = List.copyOf(content);
        if (page < 0) throw new IllegalArgumentException("page deve ser maior ou igual a zero");
        if (size < 1) throw new IllegalArgumentException("size deve ser maior ou igual a um");
        if (totalElements < 0) throw new IllegalArgumentException("totalElements deve ser maior ou igual a zero");
        if (content.size() > size) throw new IllegalArgumentException("content excede o tamanho da página");
    }

    public int totalPages() {
        long pages = totalElements / size + (totalElements % size == 0 ? 0 : 1);
        return (int) Math.min(Integer.MAX_VALUE, pages);
    }
}
