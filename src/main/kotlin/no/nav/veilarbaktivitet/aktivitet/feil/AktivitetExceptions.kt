package no.nav.veilarbaktivitet.aktivitet.feil

sealed class EndringAvAktivitetException(override val message: String) : RuntimeException(message)
class EndringAvFerdigAktivitetException(message: String) : EndringAvAktivitetException(message)
class EndringAvHistoriskAktivitetException(message: String) : EndringAvAktivitetException(message)
class EndringAvUtdatertVersjonException(message: String) : EndringAvAktivitetException(message)
class AktivitetVersjonOutOfOrderException(message: String) : EndringAvAktivitetException(message)