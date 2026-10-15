package com.crediya.notificaciones.domain.model;

import com.crediya.notificaciones.domain.exception.EventoInvalidoException;

import java.util.Locale;

/**
 * Decision final que se le comunica al solicitante.
 */
public enum EstadoDecision {
    APROBADA,
    RECHAZADA;

    public static EstadoDecision desde(String texto) {
        if (texto == null) {
            throw new EventoInvalidoException("El estado de la solicitud es obligatorio");
        }
        try {
            return valueOf(texto.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new EventoInvalidoException("Estado no notificable: " + texto);
        }
    }
}
