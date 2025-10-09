package no.nav.veilarbaktivitet.aktivitetskort.dto

import no.nav.veilarbaktivitet.aktivitetskort.feil.*

enum class ErrorType {
    AKTIVITET_IKKE_FUNNET,
    DESERIALISERINGSFEIL,
    DUPLIKATMELDINGFEIL,
    KAFKA_KEY_ULIK_AKTIVITETSID,
    MANGLER_OPPFOLGINGSPERIODE,
    MESSAGEID_LIK_AKTIVITETSID,
    UGYLDIG_IDENT,
    ULOVLIG_ENDRING,
    VALIDERINGSFEIL;

    companion object {
        fun of(exception: AktivitetsKortFunksjonellException): ErrorType {
            return when (exception) {
                is MessageIdIkkeUnikFeil -> MESSAGEID_LIK_AKTIVITETSID
                is AktivitetIkkeFunnetFeil -> AKTIVITET_IKKE_FUNNET
                is DeserialiseringsFeil -> DESERIALISERINGSFEIL
                is DuplikatMeldingFeil -> DUPLIKATMELDINGFEIL
                is KeyErIkkeFunksjonellIdFeil -> KAFKA_KEY_ULIK_AKTIVITETSID
                is ManglerOppfolgingsperiodeFeil -> MANGLER_OPPFOLGINGSPERIODE
                is UgyldigIdentFeil -> UGYLDIG_IDENT
                is UlovligEndringFeil -> ULOVLIG_ENDRING
                is ValideringFeil -> VALIDERINGSFEIL
            }
        }
    }
}
