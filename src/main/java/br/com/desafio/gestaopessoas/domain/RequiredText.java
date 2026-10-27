package br.com.desafio.gestaopessoas.domain;

public final class RequiredText {
    private RequiredText() {}

    public static String normalize(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " é obrigatório");
        String normalized = value.strip();
        if (normalized.length() > maxLength) throw new IllegalArgumentException(field + " excede " + maxLength + " caracteres");
        return normalized;
    }
}
