package com.crediya.notificaciones.domain.port;

import com.crediya.notificaciones.domain.model.Correo;

public interface CanalDeCorreo {

    /**
     * Entrega el correo al canal. Debe lanzar una excepcion si no pudo hacerlo, para que el mensaje se reintente.
     */
    void enviar(Correo correo);
}
