package no.nav.veilarbaktivitet.aktivitet

open class AktivitetsplanException(message: String): Exception(message) {
}

class UlovligEndringFeil(message: String): AktivitetsplanException(message)
