package com.crediya.notificaciones.infrastructure.aws;

import com.crediya.notificaciones.domain.model.Correo;
import com.crediya.notificaciones.domain.port.CanalDeCorreo;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.util.Map;

/**
 * Entrega el correo publicandolo en un topico de SNS. Los datos de entrega (destinatario y asunto) viajan como atributos
 * del mensaje para que el suscriptor (SES, otra Lambda, una cola) pueda enrutarlo.
 * <p>
 * Nota: SNS no envia correos a destinatarios arbitrarios por si solo (el protocolo email exige que cada destino confirme
 * su suscripcion). Para entregar al solicitante, el topico debe alimentar a SES o a un suscriptor que lo haga.
 */
public class SnsCanalDeCorreo implements CanalDeCorreo {

    static final String ATRIBUTO_DESTINATARIO = "destinatario";
    static final String ATRIBUTO_ASUNTO = "asunto";
    private static final int LARGO_MAXIMO_ASUNTO = 99;

    private final SnsClient sns;
    private final String topicArn;

    public SnsCanalDeCorreo(SnsClient sns, String topicArn) {
        this.sns = sns;
        this.topicArn = topicArn;
    }

    @Override
    public void enviar(Correo correo) {
        sns.publish(PublishRequest.builder()
                .topicArn(topicArn)
                .subject(asuntoSeguro(correo.asunto()))
                .message(correo.cuerpo())
                .messageAttributes(Map.of(
                        ATRIBUTO_DESTINATARIO, atributo(correo.destinatario()),
                        ATRIBUTO_ASUNTO, atributo(correo.asunto())))
                .build());
    }

    private static MessageAttributeValue atributo(String valor) {
        return MessageAttributeValue.builder().dataType("String").stringValue(valor).build();
    }

    /**
     * SNS exige un asunto ASCII de una sola linea y de menos de 100 caracteres.
     */
    static String asuntoSeguro(String asunto) {
        String simple = asunto.replaceAll("\\s+", " ").trim().replaceAll("[^\\x20-\\x7E]", "?");
        return simple.length() > LARGO_MAXIMO_ASUNTO ? simple.substring(0, LARGO_MAXIMO_ASUNTO) : simple;
    }
}
