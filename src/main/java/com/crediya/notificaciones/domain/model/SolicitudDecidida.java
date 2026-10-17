package com.crediya.notificaciones.domain.model;

import com.crediya.notificaciones.domain.exception.EventoInvalidoException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Evento: una solicitud de credito recibio su decision final. El plan de pago solo acompana a las aprobaciones
 * automaticas.
 */
public record SolicitudDecidida(
        UUID idSolicitud,
        EstadoDecision estado,
        BigDecimal monto,
        int plazo,
        String tipoPrestamo,
        Destinatario destinatario,
        List<CuotaPlanDePago> planDePago
) {

    public SolicitudDecidida {
        if (idSolicitud == null || estado == null || destinatario == null) {
            throw new EventoInvalidoException("El evento no trae identificador, estado o destinatario");
        }
        if (monto == null || monto.signum() <= 0 || plazo < 1) {
            throw new EventoInvalidoException("El evento no trae un monto y un plazo validos");
        }
        tipoPrestamo = tipoPrestamo == null || tipoPrestamo.isBlank() ? "credito" : tipoPrestamo.trim();
        planDePago = planDePago == null ? List.of() : List.copyOf(planDePago);
    }

    public boolean tienePlanDePago() {
        return !planDePago.isEmpty();
    }
}
