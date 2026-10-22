package io.github.arcangelotanzi.assistant.common;

// I make it abstract because no one could throw a generic error such this.
// it should always throw a specific exception, like OrderNotFoundException
// or CustomerNotFoundException
public abstract class ResourceNotFoundException extends RuntimeException {
    protected ResourceNotFoundException(String message) {
        super(message);
    }
}
