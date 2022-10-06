package no.nav.veilarbaktivitet.person;

public class IkkeFunnetPersonException extends RuntimeException {

    public IkkeFunnetPersonException() {
        super();
    }

    public IkkeFunnetPersonException(String message) {
        super(message);
    }
}
