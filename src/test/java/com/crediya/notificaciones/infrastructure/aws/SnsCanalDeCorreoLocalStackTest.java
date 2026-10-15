package com.crediya.notificaciones.infrastructure.aws;

import com.crediya.notificaciones.domain.model.Correo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba el adaptador contra SNS y SQS reales (LocalStack): el topico alimenta una cola "buzon" y se verifica el mensaje.
 */
@Tag("integration")
@Testcontainers
@DisplayName("SnsCanalDeCorreo contra SNS real (LocalStack)")
class SnsCanalDeCorreoLocalStackTest {

    @Container
    static final LocalStackContainer LOCALSTACK = new LocalStackContainer(DockerImageName.parse("localstack/localstack:4.9.2"))
            .withServices(LocalStackContainer.Service.SNS, LocalStackContainer.Service.SQS);

    private static SnsClient sns;
    private static SqsClient sqs;
    private static String topicArn;
    private static String colaBuzon;

    @BeforeAll
    static void prepararRecursos() {
        var credenciales = StaticCredentialsProvider.create(AwsBasicCredentials.create(LOCALSTACK.getAccessKey(), LOCALSTACK.getSecretKey()));
        sns = SnsClient.builder().httpClient(UrlConnectionHttpClient.create()).endpointOverride(LOCALSTACK.getEndpoint())
                .region(Region.of(LOCALSTACK.getRegion())).credentialsProvider(credenciales).build();
        sqs = SqsClient.builder().httpClient(UrlConnectionHttpClient.create()).endpointOverride(LOCALSTACK.getEndpoint())
                .region(Region.of(LOCALSTACK.getRegion())).credentialsProvider(credenciales).build();

        topicArn = sns.createTopic(r -> r.name("crediya-correos")).topicArn();
        colaBuzon = sqs.createQueue(r -> r.queueName("buzon")).queueUrl();
        String colaArn = sqs.getQueueAttributes(r -> r.queueUrl(colaBuzon).attributeNamesWithStrings("QueueArn")).attributesAsStrings().get("QueueArn");
        // RawMessageDelivery: la cola recibe el mensaje y sus atributos tal cual, sin el sobre de SNS
        sns.subscribe(r -> r.topicArn(topicArn).protocol("sqs").endpoint(colaArn).attributes(java.util.Map.of("RawMessageDelivery", "true")));
    }

    @Test
    @DisplayName("el correo publicado llega al suscriptor del topico con su cuerpo y los atributos de enrutamiento")
    void correoPublicado_llegaAlSuscriptor() throws Exception {
        new SnsCanalDeCorreo(sns, topicArn).enviar(new Correo("maria@example.com", "CrediYa - Tu solicitud fue aprobada", "Hola Maria Gomez"));

        var mensajes = sqs.receiveMessage(ReceiveMessageRequest.builder().queueUrl(colaBuzon).waitTimeSeconds(10)
                .messageAttributeNames("All").messageSystemAttributeNames(MessageSystemAttributeName.ALL).build()).messages();

        assertThat(mensajes).hasSize(1);
        var mensaje = mensajes.get(0);
        assertThat(mensaje.body()).isEqualTo("Hola Maria Gomez");
        assertThat(mensaje.messageAttributes().get("destinatario").stringValue()).isEqualTo("maria@example.com");
        assertThat(mensaje.messageAttributes().get("asunto").stringValue()).isEqualTo("CrediYa - Tu solicitud fue aprobada");
    }
}
