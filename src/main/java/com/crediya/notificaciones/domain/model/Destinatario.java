package com.crediya.notificaciones.domain.model;

import com.crediya.notificaciones.domain.exception.EventoInvalidoException;

import java.util.regex.Pattern;

public record Destinatario(String email, String nombre) {

    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    public Destinatario {
        if (email == null || !EMAIL.matcher(email.trim()).matches()) {
            throw new EventoInvalidoException("El correo del destinatario no es valido");
        }
        email = email.trim();
        nombre = nombre == null || nombre.isBlank() ? "cliente" : nombre.trim();
    }
}
