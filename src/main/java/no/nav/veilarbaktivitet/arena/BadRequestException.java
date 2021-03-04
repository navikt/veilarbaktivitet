package no.nav.veilarbaktivitet.arena;

public class BadRequestException extends Exception {
    public BadRequestException(String message) {
        super(message);
    }

    private BadRequestException() {};
}
