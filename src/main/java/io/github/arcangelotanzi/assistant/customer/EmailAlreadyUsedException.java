package io.github.arcangelotanzi.assistant.customer;

public class EmailAlreadyUsedException extends RuntimeException {
    public EmailAlreadyUsedException(String email){
        super("Email " + email + " is already used by another customer");
    }
}
