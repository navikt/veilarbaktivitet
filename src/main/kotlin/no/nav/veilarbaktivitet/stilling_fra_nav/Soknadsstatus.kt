package no.nav.veilarbaktivitet.stilling_fra_nav

enum class Soknadsstatus(val text: String) {
    VENTER("Venter på å bli kontaktet"),
    CV_DELT("CV er delt med arbeidsgiver"),
    SKAL_PAA_INTERVJU("Skal på intervju"),
    JOBBTILBUD("Fått jobbtilbud \uD83C\uDF89"),
    AVSLAG("Ikke fått jobben"),
    IKKE_FATT_JOBBEN("Ikke fått jobben"),
    FATT_JOBBEN("Fått jobben \uD83C\uDF89")
}
