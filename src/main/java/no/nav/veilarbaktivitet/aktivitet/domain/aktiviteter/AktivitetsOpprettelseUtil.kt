package no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTransaksjonsType
import no.nav.veilarbaktivitet.util.DateUtils

object AktivitetsOpprettelseUtil {

    @JvmStatic
    fun tilAktivitetsData(opprettelse: AktivitetsOpprettelse): AktivitetData {
        val opprettFelter = opprettelse.opprettFelter
        val muterbareFelter = opprettelse.muterbareFelter
        val sporing = opprettelse.sporing

        val builder = AktivitetData.builder()
            // Opprett-felter
            .aktorId(opprettFelter.aktorId)
            .aktivitetType(opprettFelter.aktivitetType)
            .kontorsperreEnhetId(opprettFelter.kontorsperreEnhetId)
            .malid(opprettFelter.malid)
            .opprettetDato(DateUtils.zonedDateTimeToDate(opprettFelter.opprettetDato))
            .automatiskOpprettet(opprettFelter.automatiskOpprettet)
            .oppfolgingsperiodeId(opprettFelter.oppfolgingsperiodeId)
            // Muterbare felter
            .tittel(muterbareFelter.tittel)
            .beskrivelse(muterbareFelter.beskrivelse)
            .fraDato(muterbareFelter.fraDato)
            .tilDato(muterbareFelter.tilDato)
            .lenke(muterbareFelter.lenke)
            // Sporingsdata
            .endretAv(sporing.endretAv)
            .endretAvType(sporing.endretAvType)
            .endretDato(DateUtils.zonedDateTimeToDate(sporing.endretDato))
            // Felles for opprettelse
            .transaksjonsType(AktivitetTransaksjonsType.OPPRETTET)
            .status(opprettelse.opprettFelter.status)

        when (opprettelse) {
            is Egenaktivitet.Opprett -> builder.egenAktivitetData(opprettelse.egenAktivitetData)
            is Jobbsoeking.Opprett -> builder.stillingsSoekAktivitetData(opprettelse.stillingsSoekAktivitetData)
            is Sokeavtale.Opprett -> builder.sokeAvtaleAktivitetData(opprettelse.sokeAvtaleAktivitetData)
            is Ijobb.Opprett -> builder.iJobbAktivitetData(opprettelse.iJobbAktivitetData)
            is Behandling.Opprett -> builder.behandlingAktivitetData(opprettelse.behandlingAktivitetData)
            is Mote.Opprett -> builder.moteData(opprettelse.moteData)
            is Samtalereferat.Opprett -> builder.moteData(opprettelse.moteData)
            is StillingFraNav.Opprett -> builder
                .stillingFraNavData(opprettelse.stillingFraNavData)
            is Eksternaktivitet.Opprett -> builder
                .eksternAktivitetData(opprettelse.eksternAktivitetData)
                .funksjonellId(opprettelse.funksjonellId)
                .avtalt(opprettelse.avtaltMedNav)
        }

        return builder.build()
    }

}