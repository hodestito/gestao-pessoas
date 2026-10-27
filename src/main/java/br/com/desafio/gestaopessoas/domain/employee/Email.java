package br.com.desafio.gestaopessoas.domain.employee;

import java.util.Locale;
import java.util.regex.Pattern;

/** Endereço normalizado segundo a política desta aplicação, não um validador completo de RFC. */
public record Email(String value) {
    private static final int MAX_LENGTH = 254;
    private static final Pattern FORMAT = Pattern.compile(
            "^[a-z0-9]+(?:[._%+\\-][a-z0-9]+)*@"
                    + "[a-z0-9](?:[a-z0-9-]*[a-z0-9])?"
                    + "(?:\\.[a-z0-9](?:[a-z0-9-]*[a-z0-9])?)+$");

    public Email {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("email é obrigatório");
        }
        value = value.strip().toLowerCase(Locale.ROOT);
        if (value.length() > MAX_LENGTH || !FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("email inválido");
        }
    }
}
