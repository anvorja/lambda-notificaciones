package com.crediya.notificaciones.domain.service;

import com.crediya.notificaciones.domain.Fixtures;
import com.crediya.notificaciones.domain.model.Correo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ComponerCorreo")
class ComponerCorreoTest {

    @Test
    @DisplayName("una aprobacion con plan debe decir APROBADA, el detalle y cada cuota con capital e intereses")
    void aprobacionConPlan_debeIncluirElPlan() {
        Correo correo = ComponerCorreo.componer(Fixtures.aprobadaConPlan());

        assertThat(correo.destinatario()).isEqualTo("maria@example.com");
        assertThat(correo.asunto()).isEqualTo("CrediYa - Tu solicitud de credito fue aprobada");
        assertThat(correo.cuerpo())
                .contains("Hola Maria Gomez,")
                .contains("fue APROBADA")
                .contains("Referencia: " + Fixtures.ID_SOLICITUD)
                .contains("Tipo: PERSONAL | Monto: $1,000,000.00 | Plazo: 2 meses")
                .contains("Plan de pago")
                .contains("Cuota 1: $500.00  (capital $410.00 + intereses $90.00)  saldo $590.00")
                .contains("Cuota 2: $500.00  (capital $420.00 + intereses $80.00)  saldo $170.00")
                .contains("Gracias por confiar en CrediYa.");
    }

    @Test
    @DisplayName("un rechazo debe comunicarse con tacto y sin plan de pago")
    void rechazo_noDebeIncluirPlan() {
        Correo correo = ComponerCorreo.componer(Fixtures.rechazada());

        assertThat(correo.asunto()).isEqualTo("CrediYa - Respuesta a tu solicitud de credito");
        assertThat(correo.cuerpo()).contains("fue RECHAZADA").contains("Tipo: VEHICULO").doesNotContain("Plan de pago");
    }

    @Test
    @DisplayName("el asunto debe ser ASCII de una sola linea (lo exige el canal de correo)")
    void asunto_debeSerAsciiDeUnaLinea() {
        assertThat(ComponerCorreo.componer(Fixtures.aprobadaConPlan()).asunto()).matches("^[\\x20-\\x7E]+$").doesNotContain("\n");
    }

    @Test
    @DisplayName("el formato de dinero debe ser independiente del idioma del servidor")
    void dinero_debeSerIndependienteDelIdioma() {
        assertThat(ComponerCorreo.dinero(new BigDecimal("1234567.5"))).isEqualTo("$1,234,567.50");
    }

}
