package no.nav.veilarbaktivitet.oversikten

import java.time.LocalDateTime

data class OversiktenMelding(
    val personID: String,
    val avsender: String = "veilarbaktivitet",
    val kategori: Kategori,
    val operasjon: Operasjon,
    val hendelse: Hendelse
) {
    companion object {
        private fun baseUrlVeilarbpersonflate(erProd: Boolean) =
            if (erProd) "https://veilarbpersonflate.intern.nav.no" else "https://veilarbpersonflate.intern.dev.nav.no"

        fun forUdeltSamtalereferat(fnr: String, operasjon: Operasjon, erProd: Boolean) = OversiktenMelding(
            personID = fnr,
            kategori = Kategori.UDELT_SAMTALEREFERAT,
            operasjon = operasjon,
            hendelse = Hendelse(
                beskrivelse = "Bruker har et udelt samtalereferat",
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
        UDELT_SAMTALEREFERAT
    }

    enum class Operasjon {
        START,
        OPPDATER,
        STOPP
    }
}
