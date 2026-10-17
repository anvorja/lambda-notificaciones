package com.crediya.notificaciones.domain.service;

import com.crediya.notificaciones.domain.model.Correo;
import com.crediya.notificaciones.domain.model.CuotaPlanDePago;
import com.crediya.notificaciones.domain.model.EstadoDecision;
import com.crediya.notificaciones.domain.model.SolicitudDecidida;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Redacta el correo que se le envia al solicitante. Es una funcion pura: mismo evento, mismo correo.
 */
public final class ComponerCorreo {

    private static final String SEPARADOR = System.lineSeparator();

    private ComponerCorreo() {
    }

    public static Correo componer(SolicitudDecidida evento) {
        String asunto = evento.estado() == EstadoDecision.APROBADA
                ? "CrediYa - Tu solicitud de credito fue aprobada"
                : "CrediYa - Respuesta a tu solicitud de credito";
        return new Correo(evento.destinatario().email(), asunto, cuerpo(evento));
    }

    private static String cuerpo(SolicitudDecidida evento) {
        StringBuilder texto = new StringBuilder();
        texto.append("Hola ").append(evento.destinatario().nombre()).append(',').append(SEPARADOR).append(SEPARADOR);
        if (evento.estado() == EstadoDecision.APROBADA) {
            texto.append("Tu solicitud de credito fue APROBADA.").append(SEPARADOR);
        } else {
            texto.append("Lamentamos informarte que tu solicitud de credito fue RECHAZADA.").append(SEPARADOR);
        }
        texto.append("Referencia: ").append(evento.idSolicitud()).append(SEPARADOR);
        texto.append("Tipo: ").append(evento.tipoPrestamo()).append(" | Monto: ").append(dinero(evento.monto()))
                .append(" | Plazo: ").append(evento.plazo()).append(" meses").append(SEPARADOR);
        if (evento.tienePlanDePago()) {
            planDePago(texto, evento);
        }
        texto.append(SEPARADOR).append("Gracias por confiar en CrediYa.");
        return texto.toString();
    }

    private static void planDePago(StringBuilder texto, SolicitudDecidida evento) {
        texto.append(SEPARADOR).append("Plan de pago (abono a capital e intereses de cada cuota):").append(SEPARADOR);
        for (CuotaPlanDePago cuota : evento.planDePago()) {
            texto.append(String.format(Locale.ROOT, "  Cuota %d: %s  (capital %s + intereses %s)  saldo %s",
                            cuota.numero(), dinero(cuota.cuota()), dinero(cuota.capital()), dinero(cuota.interes()), dinero(cuota.saldo())))
                    .append(SEPARADOR);
        }
    }

    /**
     * Formato independiente del idioma del servidor: $1,234.50
     */
    static String dinero(BigDecimal valor) {
        return String.format(Locale.ROOT, "$%,.2f", valor);
    }
}
