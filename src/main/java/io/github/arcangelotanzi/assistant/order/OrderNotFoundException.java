package io.github.arcangelotanzi.assistant.order;

import io.github.arcangelotanzi.assistant.common.ResourceNotFoundException;

public class OrderNotFoundException extends ResourceNotFoundException {
    public OrderNotFoundException(Long id) {
        super("No order found with id " + id);
    }
}
