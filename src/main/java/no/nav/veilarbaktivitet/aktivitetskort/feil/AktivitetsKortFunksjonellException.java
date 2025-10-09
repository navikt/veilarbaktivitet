package no.nav.veilarbaktivitet.aktivitetskort.feil;

sealed public class AktivitetsKortFunksjonellException extends Exception permits AktivitetIkkeFunnetFeil, DeserialiseringsFeil, DuplikatMeldingFeil, KeyErIkkeFunksjonellIdFeil, ManglerOppfolgingsperiodeFeil, MessageIdIkkeUnikFeil, UgyldigIdentFeil, UlovligEndringFeil, ValideringFeil {
    public AktivitetsKortFunksjonellException(ErrorMessage errorMessage, Throwable cause) {
        super(errorMessage.value(), cause);
    }
}
