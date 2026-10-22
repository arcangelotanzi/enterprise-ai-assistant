package io.github.arcangelotanzi.assistant.order;

public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(Long id) {
        super("No order found with id " + id);
    }
}
