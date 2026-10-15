package com.crediya.notificaciones.infrastructure.entrypoint;

import com.crediya.notificaciones.application.NotificarSolicitudDecididaUseCase;
import com.crediya.notificaciones.infrastructure.aws.SnsCanalDeCorreo;
import com.crediya.notificaciones.infrastructure.json.EventoSolicitudDecididaMapper;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.sns.SnsClient;

import java.util.Map;

/**
 * Raiz de composicion: arma el caso de uso y sus adaptadores. Se crea una sola vez por contenedor de Lambda (arranque en
 * frio) y se reutiliza en las invocaciones siguientes.
 * <p>
 * Configuracion por variables de entorno: {@code CORREOS_TOPIC_ARN} (obligatoria). La region y las credenciales las aporta
 * Lambda; el SDK tambien respeta {@code AWS_ENDPOINT_URL} (LocalStack).
 */
public record Dependencias(EventoSolicitudDecididaMapper mapper, NotificarSolicitudDecididaUseCase useCase) {

    static final String VARIABLE_TOPICO = "CORREOS_TOPIC_ARN";

    public static Dependencias desdeEntorno() {
        return desde(System.getenv());
    }

    static Dependencias desde(Map<String, String> entorno) {
        String topicArn = entorno.get(VARIABLE_TOPICO);
        if (topicArn == null || topicArn.isBlank()) {
            throw new IllegalStateException("Falta la variable de entorno " + VARIABLE_TOPICO);
        }
        SnsClient sns = SnsClient.builder().httpClient(UrlConnectionHttpClient.create()).build();
        return new Dependencias(new EventoSolicitudDecididaMapper(),
                new NotificarSolicitudDecididaUseCase(new SnsCanalDeCorreo(sns, topicArn)));
    }
}
