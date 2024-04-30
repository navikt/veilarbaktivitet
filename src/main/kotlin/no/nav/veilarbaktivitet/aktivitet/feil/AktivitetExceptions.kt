package no.nav.veilarbaktivitet.aktivitet.feil

class EndringAvFerdigAktivitetException(override val message: String): RuntimeException(message)

class EndringAvHistoriskAktivitetException(override val message: String): RuntimeException(message)