package io.github.arcangelotanzi.assistant.order;

import java.time.LocalDate;

public class InvalidDeliveryDateException extends RuntimeException {
    public InvalidDeliveryDateException(LocalDate deliveryDate, LocalDate today) {
        super("Delivery date (" + deliveryDate + ") shouldn't be before today (" + today + ")");
    }
}
