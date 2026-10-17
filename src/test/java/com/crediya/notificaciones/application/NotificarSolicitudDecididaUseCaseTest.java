package com.crediya.notificaciones.application;

import com.crediya.notificaciones.domain.Fixtures;
import com.crediya.notificaciones.domain.model.Correo;
import com.crediya.notificaciones.domain.port.CanalDeCorreo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificarSolicitudDecididaUseCase (HU6)")
class NotificarSolicitudDecididaUseCaseTest {

    @Mock
    private CanalDeCorreo canal;

    @Test
    @DisplayName("debe componer el correo de la decision y entregarlo al canal")
    void debeComponerYEntregar() {
        new NotificarSolicitudDecididaUseCase(canal).ejecutar(Fixtures.aprobadaConPlan());

        var capturado = ArgumentCaptor.forClass(Correo.class);
        verify(canal).enviar(capturado.capture());
        assertThat(capturado.getValue().destinatario()).isEqualTo("maria@example.com");
        assertThat(capturado.getValue().cuerpo()).contains("APROBADA").contains("Cuota 1");
    }

    @Test
    @DisplayName("cuando el canal falla debe propagar el error para que el mensaje se reintente")
    void cuandoElCanalFalla_debePropagar() {
        doThrow(new IllegalStateException("sns caido")).when(canal).enviar(any());

        assertThatThrownBy(() -> new NotificarSolicitudDecididaUseCase(canal).ejecutar(Fixtures.rechazada()))
                .hasMessage("sns caido");
    }
}
