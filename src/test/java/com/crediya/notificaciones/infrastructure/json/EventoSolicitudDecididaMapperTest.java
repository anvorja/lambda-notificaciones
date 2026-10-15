package com.crediya.notificaciones.infrastructure.json;

import com.crediya.notificaciones.domain.exception.EventoInvalidoException;
import com.crediya.notificaciones.domain.model.EstadoDecision;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("EventoSolicitudDecididaMapper")
class EventoSolicitudDecididaMapperTest {

    private final EventoSolicitudDecididaMapper mapper = new EventoSolicitudDecididaMapper();

    @Test
    @DisplayName("debe traducir un evento valido con plan de pago e ignorar campos desconocidos")
    void debeTraducirEventoValido() {
        var evento = mapper.aDominio(Eventos.APROBADO);

        assertThat(evento.estado()).isEqualTo(EstadoDecision.APROBADA);
        assertThat(evento.monto()).isEqualByComparingTo("1000000");
        assertThat(evento.plazo()).isEqualTo(2);
        assertThat(evento.tipoPrestamo()).isEqualTo("PERSONAL");
        assertThat(evento.destinatario().nombre()).isEqualTo("Maria Gomez");
        assertThat(evento.planDePago()).hasSize(2);
        assertThat(evento.planDePago().get(1).interes()).isEqualByComparingTo("80.00");
    }

    @Test
    @DisplayName("debe traducir un rechazo sin plan de pago")
    void debeTraducirRechazoSinPlan() {
        var evento = mapper.aDominio("""
                {"version":1,"tipo":"SOLICITUD_DECIDIDA","destinatario":{"email":"a@b.co"},
                 "solicitud":{"id_solicitud":"6f1c1c52-1d5e-4a45-9b52-8b1f3f1a0a11","estado":"rechazada","monto":5,"plazo":1}}""");

        assertThat(evento.estado()).isEqualTo(EstadoDecision.RECHAZADA);
        assertThat(evento.planDePago()).isEmpty();
        assertThat(evento.destinatario().nombre()).isEqualTo("cliente");
    }

    @ParameterizedTest(name = "cuerpo = [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "no es json", "{", "[1,2]"})
    @DisplayName("debe rechazar mensajes vacios o que no son JSON")
    void debeRechazarMensajesVaciosOMalFormados(String cuerpo) {
        assertThatThrownBy(() -> mapper.aDominio(cuerpo)).isInstanceOf(EventoInvalidoException.class);
    }

    @Test
    @DisplayName("el error de un JSON malformado no debe incluir el contenido del mensaje (datos personales)")
    void errorNoDebeFiltrarElContenido() {
        assertThatThrownBy(() -> mapper.aDominio("{\"destinatario\":{\"email\":\"secreto@x.co\" esto-rompe"))
                .isInstanceOf(EventoInvalidoException.class)
                .hasMessageNotContaining("secreto@x.co");
    }

    @Test
    @DisplayName("debe rechazar un tipo de evento o una version no soportados")
    void debeRechazarTipoOVersionNoSoportados() {
        assertThatThrownBy(() -> mapper.aDominio("{\"version\":1,\"tipo\":\"OTRO\"}"))
                .isInstanceOf(EventoInvalidoException.class).hasMessageContaining("Tipo de evento");
        assertThatThrownBy(() -> mapper.aDominio("{\"version\":2,\"tipo\":\"SOLICITUD_DECIDIDA\"}"))
                .isInstanceOf(EventoInvalidoException.class).hasMessageContaining("Version");
    }

    @Test
    @DisplayName("debe rechazar eventos sin solicitud o sin destinatario")
    void debeRechazarEventosIncompletos() {
        assertThatThrownBy(() -> mapper.aDominio("{\"version\":1,\"tipo\":\"SOLICITUD_DECIDIDA\"}"))
                .isInstanceOf(EventoInvalidoException.class);
        assertThatThrownBy(() -> mapper.aDominio("{\"version\":1,\"tipo\":\"SOLICITUD_DECIDIDA\",\"destinatario\":{\"email\":\"a@b.co\"}}"))
                .isInstanceOf(EventoInvalidoException.class);
    }

    @Test
    @DisplayName("debe rechazar un identificador de solicitud que no es UUID o falta")
    void debeRechazarIdInvalido() {
        String base = "{\"version\":1,\"tipo\":\"SOLICITUD_DECIDIDA\",\"destinatario\":{\"email\":\"a@b.co\"},"
                + "\"solicitud\":{%s\"estado\":\"APROBADA\",\"monto\":5,\"plazo\":1}}";

        assertThatThrownBy(() -> mapper.aDominio(base.formatted("\"id_solicitud\":\"no-es-uuid\",")))
                .isInstanceOf(EventoInvalidoException.class).hasMessageContaining("UUID");
        assertThatThrownBy(() -> mapper.aDominio(base.formatted("")))
                .isInstanceOf(EventoInvalidoException.class);
    }

    @Test
    @DisplayName("debe rechazar estados que no se notifican (p. ej. REVISION_MANUAL) y plazos o cuotas faltantes")
    void debeRechazarEstadoNoNotificableYDatosFaltantes() {
        String id = "\"id_solicitud\":\"6f1c1c52-1d5e-4a45-9b52-8b1f3f1a0a11\",";
        String cabecera = "{\"version\":1,\"tipo\":\"SOLICITUD_DECIDIDA\",\"destinatario\":{\"email\":\"a@b.co\"},\"solicitud\":{" + id;

        assertThatThrownBy(() -> mapper.aDominio(cabecera + "\"estado\":\"REVISION_MANUAL\",\"monto\":5,\"plazo\":1}}"))
                .isInstanceOf(EventoInvalidoException.class);
        assertThatThrownBy(() -> mapper.aDominio(cabecera + "\"estado\":\"APROBADA\",\"monto\":5}}"))
                .isInstanceOf(EventoInvalidoException.class);
        assertThatThrownBy(() -> mapper.aDominio(cabecera + "\"estado\":\"APROBADA\",\"monto\":5,\"plazo\":1},"
                + "\"plan_de_pago\":[{\"cuota\":1,\"capital\":1,\"interes\":1,\"saldo\":1}]}"))
                .isInstanceOf(EventoInvalidoException.class);
    }
}
