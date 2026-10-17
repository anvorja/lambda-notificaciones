package com.crediya.notificaciones.domain.exception;

/**
 * El mensaje recibido no cumple el contrato del evento. No tiene sentido reintentarlo tal cual: va a la cola de
 * mensajes fallidos para que alguien lo revise.
 */
public class EventoInvalidoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public EventoInvalidoException(String mensaje) {
        super(mensaje);
    }

    public EventoInvalidoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
