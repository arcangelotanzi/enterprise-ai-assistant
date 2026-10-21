package io.github.arcangelotanzi.assistant.customer;

public class CustomerNotFoundException extends RuntimeException {
    public CustomerNotFoundException(Long id) {
        super("No customer found with id " + id);
    }
}
