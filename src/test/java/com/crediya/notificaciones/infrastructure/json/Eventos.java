package com.crediya.notificaciones.infrastructure.json;

/**
 * Mensajes de ejemplo con el contrato que publica SOLICITUDES.
 */
public final class Eventos {

    public static final String APROBADO = """
            {"version":1,"id_evento":"b1","tipo":"SOLICITUD_DECIDIDA","ocurrido_en":"2026-10-15T10:00:00Z",
             "campo_futuro":{"x":1},
             "destinatario":{"email":"maria@example.com","nombre":"Maria Gomez"},
             "solicitud":{"id_solicitud":"6f1c1c52-1d5e-4a45-9b52-8b1f3f1a0a11","estado":"APROBADA","monto":1000000.00,
                          "plazo":2,"tipo_prestamo":"PERSONAL"},
             "plan_de_pago":[{"numero":1,"cuota":500.00,"capital":410.00,"interes":90.00,"saldo":590.00},
                             {"numero":2,"cuota":500.00,"capital":420.00,"interes":80.00,"saldo":170.00}]}""";

    private Eventos() {
    }
}
