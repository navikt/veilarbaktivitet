package no.nav.fo.veilarbdialog.oversiktenVaas

import java.time.LocalDateTime

data class OversiktenMelding(
    val personID: String,
    val avsender: String = "veilarbdialog",
    val kategori: Kategori,
    val operasjon: Operasjon,
    val hendelse: Hendelse
) {
    companion object {
        private fun baseUrlVeilarbpersonflate(erProd: Boolean) =
            if (erProd) "https://veilarbpersonflate.intern.nav.no" else "https://veilarbpersonflate.intern.dev.nav.no"

        fun forUtgattVarsel(fnr: String, operasjon: Operasjon, erProd: Boolean) = OversiktenMelding(
            personID = fnr,
            kategori = Kategori.UTGATT_VARSEL,
            operasjon = operasjon,
            hendelse = Hendelse(
                beskrivelse = "Bruker har et utgått varsel",
                dato = LocalDateTime.now(),
                lenke = "${baseUrlVeilarbpersonflate(erProd)}/aktivitetsplan",
            )
        )
    }

    data class Hendelse (
        val beskrivelse: String,
        val dato: LocalDateTime,
        val lenke: String,
        val detaljer: String? = null,
    )

    enum class Kategori {
        UTGATT_VARSEL
    }

    enum class Operasjon {
        START,
        OPPDATER,
        STOPP
    }
}
