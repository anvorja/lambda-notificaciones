package com.crediya.notificaciones.application;

import com.crediya.notificaciones.domain.model.SolicitudDecidida;
import com.crediya.notificaciones.domain.port.CanalDeCorreo;
import com.crediya.notificaciones.domain.service.ComponerCorreo;

/**
 * HU6: comunica al solicitante la decision final de su solicitud.
 */
public class NotificarSolicitudDecididaUseCase {

    private final CanalDeCorreo canalDeCorreo;

    public NotificarSolicitudDecididaUseCase(CanalDeCorreo canalDeCorreo) {
        this.canalDeCorreo = canalDeCorreo;
    }

    public void ejecutar(SolicitudDecidida evento) {
        canalDeCorreo.enviar(ComponerCorreo.componer(evento));
    }
}
