package io.github.arcangelotanzi.assistant.customer;

import io.github.arcangelotanzi.assistant.common.ResourceNotFoundException;

public class CustomerNotFoundException extends ResourceNotFoundException {
    public CustomerNotFoundException(Long id) {
        super("No customer found with id " + id);
    }
}
