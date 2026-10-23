package io.github.arcangelotanzi.assistant.order;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateOrderRequest(
        @NotNull Long customerId,
        @NotNull LocalDate promisedDeliveryDate,
        @NotNull @PositiveOrZero @Digits(integer=10, fraction=2)
        BigDecimal totalAmount
) {
}
