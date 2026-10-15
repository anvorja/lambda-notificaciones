package com.crediya.notificaciones.domain.model;

/**
 * Mensaje listo para enviar. El asunto es texto plano de una linea (los canales de correo lo exigen asi).
 */
public record Correo(String destinatario, String asunto, String cuerpo) {
}
