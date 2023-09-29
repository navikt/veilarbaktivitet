package no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort;

/**
 * Stylinghint for etiketter
 */
public enum Sentiment {
    POSITIVE, // Fått tilbud, Klar for oppstart, Påbegynt, Påmeldt? (gode nyheter for brukeren)
    NEGATIVE, // Ikke fått jobben, Avbrutt, Annulert, Fått avslag (dårlige nyheter for brukeren)
    NEUTRAL, // Deltar, Vurderes?
}
