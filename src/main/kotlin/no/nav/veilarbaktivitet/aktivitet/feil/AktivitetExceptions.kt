package no.nav.veilarbaktivitet.aktivitet.feil

sealed interface EndringAvAktivitetException { val message: String }
class EndringAvFerdigAktivitetException(override val message: String): RuntimeException(message), EndringAvAktivitetException
class EndringAvHistoriskAktivitetException(override val message: String): RuntimeException(message), EndringAvAktivitetException