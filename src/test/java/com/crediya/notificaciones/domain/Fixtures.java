package com.crediya.notificaciones.domain;

import com.crediya.notificaciones.domain.model.CuotaPlanDePago;
import com.crediya.notificaciones.domain.model.Destinatario;
import com.crediya.notificaciones.domain.model.EstadoDecision;
import com.crediya.notificaciones.domain.model.SolicitudDecidida;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class Fixtures {

    public static final UUID ID_SOLICITUD = UUID.fromString("6f1c1c52-1d5e-4a45-9b52-8b1f3f1a0a11");

    private Fixtures() {
    }

    public static Destinatario maria() {
        return new Destinatario("maria@example.com", "Maria Gomez");
    }

    public static List<CuotaPlanDePago> plan() {
        return List.of(
                new CuotaPlanDePago(1, new BigDecimal("500.00"), new BigDecimal("410.00"), new BigDecimal("90.00"), new BigDecimal("590.00")),
                new CuotaPlanDePago(2, new BigDecimal("500.00"), new BigDecimal("420.00"), new BigDecimal("80.00"), new BigDecimal("170.00")));
    }

    public static SolicitudDecidida aprobadaConPlan() {
        return new SolicitudDecidida(ID_SOLICITUD, EstadoDecision.APROBADA, new BigDecimal("1000000"), 2, "PERSONAL", maria(), plan());
    }

    public static SolicitudDecidida rechazada() {
        return new SolicitudDecidida(ID_SOLICITUD, EstadoDecision.RECHAZADA, new BigDecimal("1000000"), 12, "VEHICULO", maria(), null);
    }
}
