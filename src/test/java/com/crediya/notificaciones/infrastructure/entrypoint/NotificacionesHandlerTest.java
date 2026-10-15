package com.crediya.notificaciones.infrastructure.entrypoint;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.crediya.notificaciones.application.NotificarSolicitudDecididaUseCase;
import com.crediya.notificaciones.domain.model.Correo;
import com.crediya.notificaciones.domain.port.CanalDeCorreo;
import com.crediya.notificaciones.infrastructure.json.EventoSolicitudDecididaMapper;
import com.crediya.notificaciones.infrastructure.json.Eventos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("NotificacionesHandler (entrada SQS -> caso de uso)")
class NotificacionesHandlerTest {

    private final List<Correo> enviados = new ArrayList<>();
    private boolean canalFalla;

    private final CanalDeCorreo canal = correo -> {
        if (canalFalla) {
            throw new IllegalStateException("sns caido");
        }
        enviados.add(correo);
    };

    private NotificacionesHandler handler() {
        return new NotificacionesHandler(new Dependencias(new EventoSolicitudDecididaMapper(), new NotificarSolicitudDecididaUseCase(canal)));
    }

    private static SQSEvent evento(Map<String, String> mensajes) {
        var evento = new SQSEvent();
        var registros = new ArrayList<SQSEvent.SQSMessage>();
        mensajes.forEach((id, cuerpo) -> {
            var mensaje = new SQSEvent.SQSMessage();
            mensaje.setMessageId(id);
            mensaje.setBody(cuerpo);
            registros.add(mensaje);
        });
        evento.setRecords(registros);
        return evento;
    }

    @Test
    @DisplayName("cuando todos los mensajes son validos, debe enviar un correo por mensaje y no reportar fallos")
    void todosValidos_noDebeReportarFallos() {
        var respuesta = handler().handleRequest(evento(Map.of("m1", Eventos.APROBADO)), null);

        assertThat(respuesta.getBatchItemFailures()).isEmpty();
        assertThat(enviados).hasSize(1);
        assertThat(enviados.get(0).destinatario()).isEqualTo("maria@example.com");
    }

    @Test
    @DisplayName("en un lote mixto debe reportar solo el mensaje invalido y entregar los demas")
    void loteMixto_debeReportarSoloElInvalido() {
        var mensajes = new java.util.LinkedHashMap<String, String>();
        mensajes.put("bueno-1", Eventos.APROBADO);
        mensajes.put("roto", "{esto no es json");
        mensajes.put("bueno-2", Eventos.APROBADO);

        var respuesta = handler().handleRequest(evento(mensajes), null);

        assertThat(respuesta.getBatchItemFailures()).extracting(f -> f.getItemIdentifier()).containsExactly("roto");
        assertThat(enviados).hasSize(2);
    }

    @Test
    @DisplayName("cuando el canal falla debe reportar el mensaje para que se reintente")
    void cuandoElCanalFalla_debeReportarElMensaje() {
        canalFalla = true;

        var respuesta = handler().handleRequest(evento(Map.of("m1", Eventos.APROBADO)), null);

        assertThat(respuesta.getBatchItemFailures()).extracting(f -> f.getItemIdentifier()).containsExactly("m1");
        assertThat(enviados).isEmpty();
    }

    @Test
    @DisplayName("un lote vacio no debe fallar")
    void loteVacio_noDebeFallar() {
        assertThat(handler().handleRequest(evento(Map.of()), null).getBatchItemFailures()).isEmpty();
    }

    @Test
    @DisplayName("sin CORREOS_TOPIC_ARN la configuracion debe fallar rapido con un mensaje claro")
    void sinTopico_debeFallarRapido() {
        assertThatThrownBy(() -> Dependencias.desde(Map.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CORREOS_TOPIC_ARN");
        assertThatThrownBy(() -> Dependencias.desde(Map.of("CORREOS_TOPIC_ARN", "  ")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("con el topico configurado debe armar las dependencias sin llamar a AWS")
    void conTopico_debeArmarDependencias() {
        System.setProperty("aws.region", "us-east-1");
        System.setProperty("aws.accessKeyId", "test");
        System.setProperty("aws.secretAccessKey", "test");
        try {
            var dependencias = Dependencias.desde(Map.of("CORREOS_TOPIC_ARN", "arn:aws:sns:us-east-1:000000000000:t"));

            assertThat(dependencias.useCase()).isNotNull();
            assertThat(dependencias.mapper()).isNotNull();
        } finally {
            System.clearProperty("aws.region");
            System.clearProperty("aws.accessKeyId");
            System.clearProperty("aws.secretAccessKey");
        }
    }
}
