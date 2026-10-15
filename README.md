# CrediYa - Lambda de Notificaciones

AWS Lambda (Java 21) que comunica a los solicitantes la decision final de su solicitud de credito (HU6). Consume mensajes de
una cola de SQS que publica el microservicio de SOLICITUDES y entrega el correo publicandolo en un topico de SNS.

```
SOLICITUDES --(outbox)--> SQS crediya-notificaciones --> [esta Lambda] --> SNS crediya-correos --> SES / suscriptores
                              |  tras 3 intentos fallidos
                              +--> SQS crediya-notificaciones-dlq
```

## Arquitectura hexagonal

```
domain/          modelo (SolicitudDecidida, Correo...), ComponerCorreo (funcion pura) y el puerto CanalDeCorreo
application/     NotificarSolicitudDecididaUseCase
infrastructure/  aws/SnsCanalDeCorreo (adaptador de salida), json/ (contrato del evento), entrypoint/NotificacionesHandler
```

`ArquitecturaTest` (ArchUnit) hace cumplir la regla de dependencia: el dominio no conoce AWS, JSON ni logs, y la aplicacion solo
conoce el dominio. La cobertura minima del 90% en dominio y aplicacion se verifica con `./gradlew check`.

## Contrato del evento (`SOLICITUD_DECIDIDA`, version 1)

```json
{
  "version": 1, "id_evento": "uuid", "tipo": "SOLICITUD_DECIDIDA", "ocurrido_en": "2026-10-15T10:00:00Z",
  "destinatario": { "email": "maria@example.com", "nombre": "Maria Gomez" },
  "solicitud": { "id_solicitud": "uuid", "estado": "APROBADA", "monto": 5000000.00, "plazo": 24, "tipo_prestamo": "PERSONAL" },
  "plan_de_pago": [ { "numero": 1, "cuota": 258404.18, "capital": 168404.18, "interes": 90000.00, "saldo": 4831595.82 } ]
}
```

`estado` es `APROBADA` o `RECHAZADA`; `plan_de_pago` es opcional (solo acompana a las aprobaciones automaticas, HU7). La Lambda es un
*lector tolerante*: ignora campos que no conoce. Un mensaje que no cumple el contrato se rechaza con un error y, tras agotar los
reintentos, termina en la cola de mensajes fallidos.

## Comportamiento ante fallos

- La Lambda responde con **fallos parciales del lote** (`ReportBatchItemFailures`): solo los mensajes que fallaron vuelven a la
  cola; los demas se dan por entregados. La asociacion con la cola debe declarar `FunctionResponseTypes=ReportBatchItemFailures`.
- Los logs registran el id del mensaje y el tipo de error, **nunca el contenido** (trae datos personales).
- El asunto del correo se normaliza a ASCII de una linea (lo exige SNS).

> **Entrega real del correo:** SNS no envia correos a destinatarios arbitrarios: el protocolo `email` exige que cada destino
> confirme su suscripcion. Para entregar al solicitante, el topico debe alimentar a **SES** o a un suscriptor que lo haga. Cada
> mensaje lleva los atributos `destinatario` y `asunto` para que el suscriptor pueda enrutarlo.

## Configuracion

| Variable | Descripcion |
|----------|-------------|
| `CORREOS_TOPIC_ARN` | ARN del topico de SNS donde se publica el correo (obligatoria; si falta la funcion falla al arrancar con un mensaje claro) |
| `AWS_REGION`, credenciales | Las aporta Lambda (rol de ejecucion) |
| `AWS_ENDPOINT_URL` | Solo para pruebas locales (LocalStack): el SDK la respeta |

Permisos del rol de ejecucion: `sns:Publish` sobre el topico, y lectura de la cola (`AWSLambdaSQSQueueExecutionRole`).

## Compilar, probar y desplegar

```bash
./gradlew check        # pruebas + arquitectura + cobertura minima 90% en dominio y aplicacion
./gradlew shadowJar    # build/libs/lambda-notificaciones.jar (8.5 MB, sin Netty ni Apache)
```

- Handler: `com.crediya.notificaciones.infrastructure.entrypoint.NotificacionesHandler::handleRequest`
- Runtime: `java21`; memoria sugerida 512 MB; timeout 30 s (menor que el *visibility timeout* de la cola).
- La prueba `SnsCanalDeCorreoLocalStackTest` usa SNS y SQS reales de LocalStack 4.9.2 mediante Testcontainers.
  Nota: desde LocalStack 2026.x la imagen `latest` exige un token de cuenta; por eso se fija la 4.9.2.

## Flujo de trabajo (gitflow)

`main` <- `develop` <- `feature/HU-XX-nombre`, integrando con `--no-ff`.
