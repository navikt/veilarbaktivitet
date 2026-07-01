package no.nav.veilarbaktivitet.aktivitet

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData
import no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter.AktivitetsEndring
import no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter.Mote
import org.springframework.stereotype.Service

@Service
class AktivitetOppdateringService(
    private val aktivitetService: AktivitetService
) {

    fun oppdaterSomNav(aktivitetsEndring: AktivitetsEndring, originalAktivitet: AktivitetData) {
        originalAktivitet
            .oppdaterMøtestedTidOgKanal(aktivitetsEndring)
            .oppdaterAktivitet(aktivitetsEndring)
    }

    private fun AktivitetData.oppdaterMøtestedTidOgKanal(aktivitetsEndring: AktivitetsEndring): AktivitetData {
        if (this.aktivitetType != AktivitetTypeData.MOTE) return this

        if (erMøtestedTidEllerKanalEndret(this, aktivitetsEndring as Mote.Endre)) {
            aktivitetService.oppdaterMoteTidStedOgKanal(this, aktivitetsEndring)
            return aktivitetService.hentAktivitetMedForhaandsorientering(this.id)
        }
        return this
    }

    private fun AktivitetData.oppdaterAktivitet(aktivitetsEndring: AktivitetsEndring): AktivitetData {
        if (this.aktivitetType == AktivitetTypeData.MOTE && erMøtedetaljerEndret(this, aktivitetsEndring as Mote.Endre)) {
            aktivitetService.oppdaterMoteDetaljer(this, aktivitetsEndring as Mote.Endre)
        } else if (this.aktivitetType == AktivitetTypeData.MOTE && this.isAvtalt) {
            aktivitetService.oppdaterAktivitetFrist(this, aktivitetsEndring)
        } else {
            aktivitetService.oppdaterAktivitet(this, aktivitetsEndring)
        }
        return aktivitetService.hentAktivitetMedForhaandsorientering(this.id)
    }


    private fun erMøtestedTidEllerKanalEndret(originalAktivitet: AktivitetData, endretMøteAktivitet: Mote.Endre): Boolean {
        val fraDatoEndret = originalAktivitet.getFraDato() != endretMøteAktivitet.muterbareFelter.fraDato
        val tilDatoEndret = originalAktivitet.getTilDato() != endretMøteAktivitet.muterbareFelter.tilDato

        val adresseEndret = originalAktivitet.moteData.adresse != endretMøteAktivitet.moteData.adresse
        val kanalEndret = originalAktivitet.moteData.kanal != endretMøteAktivitet.moteData.kanal

        return fraDatoEndret || tilDatoEndret || adresseEndret || kanalEndret
    }

    private fun erMøtedetaljerEndret(originalAktivitet: AktivitetData, endretMøteAktivitet: Mote.Endre): Boolean {
        val tittelEndret = originalAktivitet.tittel != endretMøteAktivitet.muterbareFelter.tittel
        val beskrivelseEndret = originalAktivitet.beskrivelse != endretMøteAktivitet.muterbareFelter.beskrivelse

        val originalMote = originalAktivitet.moteData
        val nyMote = endretMøteAktivitet.moteData

        val forberedelserEndret =
            originalMote != null && (originalMote.forberedelser != nyMote.forberedelser)

        return tittelEndret || beskrivelseEndret || forberedelserEndret
    }
}