package com.crediya.notificaciones.domain.model;

import com.crediya.notificaciones.domain.exception.EventoInvalidoException;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Una cuota del plan de pago, diferenciando abono a capital e intereses.
 */
public record CuotaPlanDePago(int numero, BigDecimal cuota, BigDecimal capital, BigDecimal interes, BigDecimal saldo) {

    public CuotaPlanDePago {
        if (numero < 1) {
            throw new EventoInvalidoException("El numero de cuota debe ser mayor a cero");
        }
        Objects.requireNonNull(cuota, "cuota");
        Objects.requireNonNull(capital, "capital");
        Objects.requireNonNull(interes, "interes");
        Objects.requireNonNull(saldo, "saldo");
    }
}
