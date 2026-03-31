package no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter

import no.nav.veilarbaktivitet.aktivitet.domain.*
import no.nav.veilarbaktivitet.stilling_fra_nav.CvKanDelesData
import no.nav.veilarbaktivitet.stilling_fra_nav.StillingFraNavData
import no.nav.veilarbaktivitet.util.MappingUtils
import java.util.*
import java.util.function.BiFunction

sealed interface AktivitetsEndring {
    val muterbareFelter: AktivitetMuterbareFelter
    val id: Long
    val versjon: Long
    val sporing: SporingsData

//    open fun berikMedAktivitetsTypeSpesifikkData(): AktivitetData
}

object AktivitetsEndringUtil {

    @JvmStatic
    fun berikMedAktivitetsTypeSpesifikkData(input: AktivitetData, endring: AktivitetsEndring): AktivitetData {
        return when (endring) {
            is Behandling.Endre ->
                mergeBehandlingAktivitetData(input.behandlingAktivitetData, endring.behandlingAktivitetData)
                    .let { input.withBehandlingAktivitetData(it) }
            is Egenaktivitet.Endre ->
                mergeEgenAktivitetData(input.egenAktivitetData, endring.egenAktivitetData)
                    .let { input.withEgenAktivitetData(it) }
            is Eksternaktivitet.Endre ->
                mergeEksternAktivitet(input.eksternAktivitetData, endring.eksternAktivitetData)
                    .let { input.withEksternAktivitetData(it) }
            is Ijobb.Endre ->
                mergeIJobbAktivitetData(input.iJobbAktivitetData, endring.iJobbAktivitetData)
                    .let { input.withIJobbAktivitetData(it) }
            is Jobbsoeking.Endre -> mergeStillingSok(input.stillingsSoekAktivitetData, endring.stillingsSoekAktivitetData)
                .let { input.withStillingsSoekAktivitetData(it) }
            // OBS, de to neste bruker samme!
            is Mote.Endre ->
                mergeMoteData(input.moteData, endring.moteData)
                    .let { input.withMoteData(it) }
            is Samtalereferat.Endre ->
                mergeMoteData(input.moteData, endring.moteData)
                    .let { input.withMoteData(it) }
            is Sokeavtale.Endre ->
                mergeSokeAvtaleAktivitetData(input.sokeAvtaleAktivitetData, endring.sokeAvtaleAktivitetData)
                    .let { input.withSokeAvtaleAktivitetData(it) }
            is StillingFraNav.Endre ->
                mergeStillingFraNav(input.stillingFraNavData, endring.stillingFraNavData)
                    .let { input.withStillingFraNavData(it) }
        }
    }

    private fun mergeBehandlingAktivitetData(
        originalBehandlingAktivitetData: BehandlingAktivitetData,
        behandlingAktivitetData: BehandlingAktivitetData
    ): BehandlingAktivitetData {
        return originalBehandlingAktivitetData
            .withBehandlingType(behandlingAktivitetData.getBehandlingType())
            .withBehandlingSted(behandlingAktivitetData.getBehandlingSted())
            .withEffekt(behandlingAktivitetData.getEffekt())
            .withBehandlingOppfolging(behandlingAktivitetData.getBehandlingOppfolging())
    }

    private fun mergeIJobbAktivitetData(
        originalIJobbAktivitetData: IJobbAktivitetData,
        iJobbAktivitetData: IJobbAktivitetData
    ): IJobbAktivitetData {
        return originalIJobbAktivitetData
            .withJobbStatusType(iJobbAktivitetData.getJobbStatusType())
            .withAnsettelsesforhold(iJobbAktivitetData.getAnsettelsesforhold())
            .withArbeidstid(iJobbAktivitetData.getArbeidstid())
    }

    private fun mergeSokeAvtaleAktivitetData(
        originalSokeAvtaleAktivitetData: SokeAvtaleAktivitetData,
        sokeAvtaleAktivitetData: SokeAvtaleAktivitetData
    ): SokeAvtaleAktivitetData? {
        return originalSokeAvtaleAktivitetData
            .withAntallStillingerSokes(sokeAvtaleAktivitetData.getAntallStillingerSokes())
            .withAntallStillingerIUken(sokeAvtaleAktivitetData.getAntallStillingerIUken())
            .withAvtaleOppfolging(sokeAvtaleAktivitetData.getAvtaleOppfolging())
    }

    private fun mergeEgenAktivitetData(
        originalEgenAktivitetData: EgenAktivitetData,
        egenAktivitetData: EgenAktivitetData
    ): EgenAktivitetData {
        return originalEgenAktivitetData
            .withOppfolging(egenAktivitetData.getOppfolging())
            .withHensikt(egenAktivitetData.getHensikt())
    }

    private fun mergeMoteData(originalMoteData: MoteData, moteData: MoteData): MoteData {
        // Referat-felter settes gjennom egne operasjoner, se oppdaterReferat()
        return originalMoteData
            .withAdresse(moteData.getAdresse())
            .withForberedelser(moteData.getForberedelser())
            .withKanal(moteData.getKanal())
    }

    private fun mergeStillingSok(
        originalStillingsoekAktivitetData: StillingsoekAktivitetData,
        stillingsoekAktivitetData: StillingsoekAktivitetData
    ): StillingsoekAktivitetData {
        return originalStillingsoekAktivitetData
            .withArbeidsgiver(stillingsoekAktivitetData.getArbeidsgiver())
            .withArbeidssted(stillingsoekAktivitetData.getArbeidssted())
            .withKontaktPerson(stillingsoekAktivitetData.getKontaktPerson())
            .withStillingsTittel(stillingsoekAktivitetData.getStillingsTittel())
    }

    private fun mergeEksternAktivitet(
        original: EksternAktivitetData,
        newData: EksternAktivitetData
    ): EksternAktivitetData {
        return original.copy(
            source = newData.source,
            tiltaksKode = newData.tiltaksKode,
            opprettetSomHistorisk = original.opprettetSomHistorisk,
            oppfolgingsperiodeSlutt = original.oppfolgingsperiodeSlutt,
            arenaId = newData.arenaId,
            type = newData.type,
            oppgave = newData.oppgave,
            handlinger = newData.handlinger,
            detaljer = newData.detaljer,
            etiketter = newData.etiketter,
            endretTidspunktKilde = newData.endretTidspunktKilde
        )
    }

    private fun mergeCVKanDelesData(existing: CvKanDelesData, updated: CvKanDelesData): CvKanDelesData {
        return existing
            .withKanDeles(updated.getKanDeles())
            .withEndretAv(updated.getEndretAv())
            .withAvtaltDato(updated.getAvtaltDato())
            .withEndretAvType(updated.getEndretAvType())
            .withEndretTidspunkt(Date())
    }

    private fun mergeStillingFraNav(existing: StillingFraNavData, updated: StillingFraNavData): StillingFraNavData {
        return existing
            .withArbeidssted(updated.getArbeidssted())
            .withArbeidsgiver(updated.getArbeidsgiver())
            .withCvKanDelesData(
                MappingUtils.merge<CvKanDelesData?>(existing.getCvKanDelesData(), updated.getCvKanDelesData())
                    .merge(BiFunction { existing: CvKanDelesData?, updated: CvKanDelesData? ->
                        this.mergeCVKanDelesData(
                            existing!!,
                            updated!!
                        )
                    })
            )
            .withLivslopsStatus(updated.getLivslopsStatus())
            .withSoknadsstatus(updated.getSoknadsstatus())
            .withBestillingsId(updated.getBestillingsId())
            .withSvarfrist(updated.getSvarfrist())
            .withKontaktpersonData(updated.getKontaktpersonData())
            .withDetaljer(updated.getDetaljer())
            .withVarselId(updated.getVarselId())
    }
}