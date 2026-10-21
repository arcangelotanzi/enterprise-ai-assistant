package io.github.arcangelotanzi.assistant.order;

public enum OrderStatus {
    PENDING,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    public boolean isOpen() {
        return this != DELIVERED && this != CANCELLED;
    }
}
