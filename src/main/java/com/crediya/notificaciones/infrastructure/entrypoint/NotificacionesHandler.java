package com.crediya.notificaciones.infrastructure.entrypoint;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Punto de entrada de la Lambda de NOTIFICACIONES (HU6): consume mensajes de la cola de SQS y publica el correo en SNS.
 * <p>
 * Responde con fallos parciales del lote: solo los mensajes que fallaron vuelven a la cola (y, tras agotar los reintentos,
 * a la cola de mensajes fallidos); los demas se consideran entregados. Requiere
 * {@code FunctionResponseTypes=ReportBatchItemFailures} en la asociacion con la cola.
 */
public class NotificacionesHandler implements RequestHandler<SQSEvent, SQSBatchResponse> {

    private static final System.Logger LOG = System.getLogger(NotificacionesHandler.class.getName());

    private final Dependencias dependencias;

    /** Constructor que usa Lambda: arma las dependencias una sola vez por contenedor. */
    public NotificacionesHandler() {
        this(Dependencias.desdeEntorno());
    }

    NotificacionesHandler(Dependencias dependencias) {
        this.dependencias = dependencias;
    }

    @Override
    public SQSBatchResponse handleRequest(SQSEvent evento, Context contexto) {
        List<SQSBatchResponse.BatchItemFailure> fallos = new ArrayList<>();
        for (SQSEvent.SQSMessage mensaje : evento.getRecords()) {
            try {
                dependencias.useCase().ejecutar(dependencias.mapper().aDominio(mensaje.getBody()));
                LOG.log(System.Logger.Level.INFO, "Notificacion entregada messageId={0}", mensaje.getMessageId());
            } catch (RuntimeException e) {
                // Solo el tipo de error y el id del mensaje: el cuerpo trae datos personales
                LOG.log(System.Logger.Level.ERROR, "Fallo la notificacion messageId={0} error={1}",
                        mensaje.getMessageId(), e.getClass().getSimpleName());
                fallos.add(new SQSBatchResponse.BatchItemFailure(mensaje.getMessageId()));
            }
        }
        return new SQSBatchResponse(fallos);
    }
}
