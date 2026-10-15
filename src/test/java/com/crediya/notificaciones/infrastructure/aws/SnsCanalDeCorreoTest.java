package com.crediya.notificaciones.infrastructure.aws;

import com.crediya.notificaciones.domain.model.Correo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SnsCanalDeCorreo (adaptador, con SNS simulado)")
class SnsCanalDeCorreoTest {

    private static final String TOPIC = "arn:aws:sns:us-east-1:000000000000:crediya-correos";

    @Mock
    private SnsClient sns;

    @Test
    @DisplayName("debe publicar el cuerpo en el topico con el asunto y los atributos de enrutamiento")
    void debePublicarConAtributos() {
        new SnsCanalDeCorreo(sns, TOPIC).enviar(new Correo("maria@example.com", "CrediYa - Aprobada", "Hola Maria"));

        var capturado = ArgumentCaptor.forClass(PublishRequest.class);
        verify(sns).publish(capturado.capture());
        PublishRequest peticion = capturado.getValue();
        assertThat(peticion.topicArn()).isEqualTo(TOPIC);
        assertThat(peticion.subject()).isEqualTo("CrediYa - Aprobada");
        assertThat(peticion.message()).isEqualTo("Hola Maria");
        assertThat(peticion.messageAttributes().get("destinatario").stringValue()).isEqualTo("maria@example.com");
        assertThat(peticion.messageAttributes().get("asunto").stringValue()).isEqualTo("CrediYa - Aprobada");
    }

    @Test
    @DisplayName("cuando SNS falla debe propagar el error para que el mensaje se reintente")
    void cuandoSnsFalla_debePropagar() {
        when(sns.publish(any(PublishRequest.class))).thenThrow(SdkClientException.create("sin red"));

        assertThatThrownBy(() -> new SnsCanalDeCorreo(sns, TOPIC).enviar(new Correo("a@b.co", "Asunto", "Cuerpo")))
                .isInstanceOf(SdkClientException.class);
    }

    @Test
    @DisplayName("el asunto debe quedar en ASCII, de una linea y de menos de 100 caracteres")
    void asunto_debeSerSeguro() {
        assertThat(SnsCanalDeCorreo.asuntoSeguro("  Tu   solicitud\n fue\taprobada ")).isEqualTo("Tu solicitud fue aprobada");
        assertThat(SnsCanalDeCorreo.asuntoSeguro("Credito aprobado: ñandú ✓")).isEqualTo("Credito aprobado: ?and? ?");
        assertThat(SnsCanalDeCorreo.asuntoSeguro("x".repeat(300))).hasSize(99);
    }
}
