package com.crediya.notificaciones.infrastructure.json;

import com.crediya.notificaciones.domain.exception.EventoInvalidoException;
import com.crediya.notificaciones.domain.model.CuotaPlanDePago;
import com.crediya.notificaciones.domain.model.Destinatario;
import com.crediya.notificaciones.domain.model.EstadoDecision;
import com.crediya.notificaciones.domain.model.SolicitudDecidida;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Traduce el JSON que publica SOLICITUDES al evento del dominio. Lector tolerante: ignora campos nuevos para que SOLICITUDES
 * pueda agregar informacion sin romper esta funcion; si faltan datos obligatorios el dominio rechaza el evento.
 */
public class EventoSolicitudDecididaMapper {

    static final String TIPO_EVENTO = "SOLICITUD_DECIDIDA";
    static final int VERSION_SOPORTADA = 1;

    private final ObjectMapper json = JsonMapper.builder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true)
            .build();

    public SolicitudDecidida aDominio(String cuerpo) {
        Evento evento = leer(cuerpo);
        if (!TIPO_EVENTO.equals(evento.tipo())) {
            throw new EventoInvalidoException("Tipo de evento no soportado: " + evento.tipo());
        }
        if (evento.version() != VERSION_SOPORTADA) {
            throw new EventoInvalidoException("Version de evento no soportada: " + evento.version());
        }
        if (evento.solicitud() == null || evento.destinatario() == null) {
            throw new EventoInvalidoException("El evento no trae la solicitud o el destinatario");
        }
        return new SolicitudDecidida(
                uuid(evento.solicitud().idSolicitud()),
                EstadoDecision.desde(evento.solicitud().estado()),
                evento.solicitud().monto(),
                evento.solicitud().plazo() == null ? 0 : evento.solicitud().plazo(),
                evento.solicitud().tipoPrestamo(),
                new Destinatario(evento.destinatario().email(), evento.destinatario().nombre()),
                plan(evento.planDePago()));
    }

    private Evento leer(String cuerpo) {
        if (cuerpo == null || cuerpo.isBlank()) {
            throw new EventoInvalidoException("El mensaje esta vacio");
        }
        try {
            return json.readValue(cuerpo, Evento.class);
        } catch (JsonProcessingException e) {
            // No se incluye el contenido del mensaje: puede traer datos personales
            throw new EventoInvalidoException("El mensaje no es un JSON valido del evento", e);
        }
    }

    private static UUID uuid(String texto) {
        try {
            return UUID.fromString(texto);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new EventoInvalidoException("El identificador de la solicitud no es un UUID valido");
        }
    }

    private static List<CuotaPlanDePago> plan(List<Cuota> cuotas) {
        if (cuotas == null) {
            return List.of();
        }
        return cuotas.stream()
                .map(c -> new CuotaPlanDePago(c.numero() == null ? 0 : c.numero(), c.cuota(), c.capital(), c.interes(), c.saldo()))
                .toList();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Evento(int version, String tipo, DestinatarioJson destinatario, SolicitudJson solicitud, List<Cuota> planDePago) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record DestinatarioJson(String email, String nombre) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SolicitudJson(String idSolicitud, String estado, BigDecimal monto, Integer plazo, String tipoPrestamo) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Cuota(Integer numero, BigDecimal cuota, BigDecimal capital, BigDecimal interes, BigDecimal saldo) {
    }
}
