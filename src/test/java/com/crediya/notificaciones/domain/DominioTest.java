package com.crediya.notificaciones.domain;

import com.crediya.notificaciones.domain.exception.EventoInvalidoException;
import com.crediya.notificaciones.domain.model.CuotaPlanDePago;
import com.crediya.notificaciones.domain.model.Destinatario;
import com.crediya.notificaciones.domain.model.EstadoDecision;
import com.crediya.notificaciones.domain.model.SolicitudDecidida;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Dominio de notificaciones")
class DominioTest {

    // ---------- SolicitudDecidida ----------

    @Test
    @DisplayName("sin plan de pago debe quedar una lista vacia (no nula) y reportarlo")
    void sinPlan_debeSerListaVacia() {
        assertThat(Fixtures.rechazada().planDePago()).isEmpty();
        assertThat(Fixtures.rechazada().tienePlanDePago()).isFalse();
        assertThat(Fixtures.aprobadaConPlan().tienePlanDePago()).isTrue();
    }

    @Test
    @DisplayName("el plan debe ser inmutable respecto de la lista original")
    void plan_debeSerInmutable() {
        var evento = Fixtures.aprobadaConPlan();

        assertThatThrownBy(() -> evento.planDePago().add(Fixtures.plan().get(0))).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("sin tipo de prestamo debe usar un texto neutro")
    void sinTipo_debeUsarTextoNeutro() {
        var evento = new SolicitudDecidida(Fixtures.ID_SOLICITUD, EstadoDecision.APROBADA, BigDecimal.TEN, 1, "  ", Fixtures.maria(), null);

        assertThat(evento.tipoPrestamo()).isEqualTo("credito");
    }

    @Test
    @DisplayName("debe rechazar eventos sin identificador, estado o destinatario")
    void debeRechazarEventosIncompletos() {
        assertThatThrownBy(() -> new SolicitudDecidida(null, EstadoDecision.APROBADA, BigDecimal.TEN, 1, "P", Fixtures.maria(), null))
                .isInstanceOf(EventoInvalidoException.class);
        assertThatThrownBy(() -> new SolicitudDecidida(Fixtures.ID_SOLICITUD, null, BigDecimal.TEN, 1, "P", Fixtures.maria(), null))
                .isInstanceOf(EventoInvalidoException.class);
        assertThatThrownBy(() -> new SolicitudDecidida(Fixtures.ID_SOLICITUD, EstadoDecision.APROBADA, BigDecimal.TEN, 1, "P", null, null))
                .isInstanceOf(EventoInvalidoException.class);
    }

    @Test
    @DisplayName("debe rechazar eventos con monto o plazo invalidos")
    void debeRechazarMontoOPlazoInvalidos() {
        assertThatThrownBy(() -> new SolicitudDecidida(Fixtures.ID_SOLICITUD, EstadoDecision.APROBADA, null, 1, "P", Fixtures.maria(), null))
                .isInstanceOf(EventoInvalidoException.class);
        assertThatThrownBy(() -> new SolicitudDecidida(Fixtures.ID_SOLICITUD, EstadoDecision.APROBADA, BigDecimal.ZERO, 1, "P", Fixtures.maria(), null))
                .isInstanceOf(EventoInvalidoException.class);
        assertThatThrownBy(() -> new SolicitudDecidida(Fixtures.ID_SOLICITUD, EstadoDecision.APROBADA, BigDecimal.TEN, 0, "P", Fixtures.maria(), null))
                .isInstanceOf(EventoInvalidoException.class);
    }

    // ---------- Destinatario, EstadoDecision, CuotaPlanDePago ----------

    @ParameterizedTest(name = "correo = [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {"sin-arroba", "a@b", "dos@@x.co", "con espacio@x.co"})
    @DisplayName("el destinatario debe rechazar correos invalidos")
    void destinatario_debeRechazarCorreosInvalidos(String correo) {
        assertThatThrownBy(() -> new Destinatario(correo, "Maria")).isInstanceOf(EventoInvalidoException.class);
    }

    @Test
    @DisplayName("el destinatario debe normalizar espacios y usar un nombre por defecto si falta")
    void destinatario_debeNormalizar() {
        assertThat(new Destinatario("  a@b.co ", null)).isEqualTo(new Destinatario("a@b.co", "cliente"));
        assertThat(new Destinatario("a@b.co", "   ").nombre()).isEqualTo("cliente");
        assertThat(new Destinatario("a@b.co", " Ana ").nombre()).isEqualTo("Ana");
    }

    @Test
    @DisplayName("EstadoDecision.desde debe ignorar mayusculas y rechazar estados no notificables")
    void estadoDecision_debeValidar() {
        assertThat(EstadoDecision.desde(" aprobada ")).isEqualTo(EstadoDecision.APROBADA);
        assertThat(EstadoDecision.desde("RECHAZADA")).isEqualTo(EstadoDecision.RECHAZADA);
        assertThatThrownBy(() -> EstadoDecision.desde("REVISION_MANUAL")).isInstanceOf(EventoInvalidoException.class);
        assertThatThrownBy(() -> EstadoDecision.desde(null)).isInstanceOf(EventoInvalidoException.class);
    }

    @Test
    @DisplayName("la cuota debe tener numero positivo y todos sus importes")
    void cuota_debeValidarse() {
        assertThatThrownBy(() -> new CuotaPlanDePago(0, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE))
                .isInstanceOf(EventoInvalidoException.class);
        assertThatThrownBy(() -> new CuotaPlanDePago(1, null, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CuotaPlanDePago(1, BigDecimal.ONE, null, BigDecimal.ONE, BigDecimal.ONE))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CuotaPlanDePago(1, BigDecimal.ONE, BigDecimal.ONE, null, BigDecimal.ONE))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CuotaPlanDePago(1, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("EventoInvalidoException con causa debe conservarla")
    void excepcion_conCausa() {
        var causa = new IllegalStateException("x");

        assertThat(new EventoInvalidoException("m", causa)).hasCause(causa);
    }
}
