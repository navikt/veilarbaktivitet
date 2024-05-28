package no.nav.veilarbaktivitet.internapi

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData
import no.nav.veilarbaktivitet.aktivitet.domain.IJobbAktivitetData
import no.nav.veilarbaktivitet.aktivitet.domain.JobbStatusTypeData
import no.nav.veilarbaktivitet.internapi.model.*
import no.nav.veilarbaktivitet.stilling_fra_nav.CvKanDelesData
import no.nav.veilarbaktivitet.util.DateUtils
import org.apache.commons.lang3.NotImplementedException

object InternAktivitetMapper {
    fun mapTilAktivitet(aktivitetData: AktivitetData): Aktivitet {
        val aktivitetType: AktivitetTypeData = aktivitetData.aktivitetType
        return when (aktivitetType) {
            AktivitetTypeData.EGENAKTIVITET -> mapTilEgenaktivitet(aktivitetData)
            AktivitetTypeData.JOBBSOEKING -> mapTilJobbsoeking(aktivitetData)
            AktivitetTypeData.SOKEAVTALE -> mapTilSokeavtale(aktivitetData)
            AktivitetTypeData.IJOBB -> mapTilIjobb(aktivitetData)
            AktivitetTypeData.BEHANDLING -> mapTilBehandling(aktivitetData)
            AktivitetTypeData.MOTE -> mapTilMote(aktivitetData)
            AktivitetTypeData.SAMTALEREFERAT -> mapTilSamtalereferat(aktivitetData)
            AktivitetTypeData.STILLING_FRA_NAV -> mapTilStillingFraNav(aktivitetData)
            AktivitetTypeData.EKSTERNAKTIVITET -> mapTilTiltak(aktivitetData)
        }
    }

    private fun mapTilTiltak(aktivitetData: AktivitetData): Aktivitet {
        throw NotImplementedException("TODO")
    }

    private fun mapTilEgenaktivitet(aktivitetData: AktivitetData): Egenaktivitet {
        val egenAktivitetData = aktivitetData.egenAktivitetData
        return Egenaktivitet(
            aktivitetType = AktivitetType.egenaktivitet,
            avtaltMedNav = aktivitetData.isAvtalt(),
            aktivitetId = aktivitetData.id.toString(),
            kontorsperreEnhetId = aktivitetData.kontorsperreEnhetId,
            oppfolgingsperiodeId = aktivitetData.oppfolgingsperiodeId,
            status = Status.valueOf(aktivitetData.status.name),
            beskrivelse = aktivitetData.beskrivelse,
            tittel = aktivitetData.tittel,
            fraDato = DateUtils.toOffsetDateTime(aktivitetData.fraDato),
            tilDato = DateUtils.toOffsetDateTime(aktivitetData.tilDato),
            opprettetDato = DateUtils.toOffsetDateTime(aktivitetData.opprettetDato),
            endretDato = DateUtils.toOffsetDateTime(aktivitetData.opprettetDato),

            hensikt = egenAktivitetData.hensikt,
            oppfolging = egenAktivitetData.oppfolging
        )
    }

    private fun mapTilJobbsoeking(aktivitetData: AktivitetData): Jobbsoeking {
        val stillingsSoekAktivitetData = aktivitetData.stillingsSoekAktivitetData
        val stillingsoekEtikett = stillingsSoekAktivitetData.stillingsoekEtikett
        return Jobbsoeking(
            aktivitetType = AktivitetType.jobbsoeking,
            avtaltMedNav = aktivitetData.isAvtalt,
            aktivitetId = aktivitetData.id.toString(),
            kontorsperreEnhetId = aktivitetData.kontorsperreEnhetId,
            oppfolgingsperiodeId = aktivitetData.oppfolgingsperiodeId,
            status = Status.valueOf(aktivitetData.status.name),
            beskrivelse = aktivitetData.beskrivelse,
            tittel = aktivitetData.tittel,
            fraDato = DateUtils.toOffsetDateTime(aktivitetData.fraDato),
            tilDato = DateUtils.toOffsetDateTime(aktivitetData.tilDato),
            opprettetDato = DateUtils.toOffsetDateTime(aktivitetData.opprettetDato),
            endretDato = DateUtils.toOffsetDateTime(aktivitetData.opprettetDato),

            arbeidsgiver = stillingsSoekAktivitetData.arbeidsgiver,
            stillingsTittel = stillingsSoekAktivitetData.stillingsTittel,
            arbeidssted = stillingsSoekAktivitetData.arbeidssted,
            stillingsoekEtikett =
                stillingsoekEtikett
                    ?.let { it.name }
                    ?.let { Jobbsoeking.StillingsoekEtikett.valueOf(it) },
            kontaktPerson = stillingsSoekAktivitetData.kontaktPerson
        )
    }

    private fun mapTilSokeavtale(aktivitetData: AktivitetData): Sokeavtale {
        val sokeAvtaleAktivitetData = aktivitetData.sokeAvtaleAktivitetData
        return Sokeavtale(
            aktivitetType = AktivitetType.sokeavtale,
            avtaltMedNav = aktivitetData.isAvtalt,
            aktivitetId = aktivitetData.id.toString(),
            kontorsperreEnhetId = aktivitetData.kontorsperreEnhetId,
            oppfolgingsperiodeId = aktivitetData.oppfolgingsperiodeId,
            status = Status.valueOf(aktivitetData.status.name),
            beskrivelse = aktivitetData.beskrivelse,
            tittel = aktivitetData.tittel,
            fraDato = DateUtils.toOffsetDateTime(aktivitetData.fraDato),
            tilDato = DateUtils.toOffsetDateTime(aktivitetData.tilDato),
            opprettetDato = DateUtils.toOffsetDateTime(aktivitetData.opprettetDato),
            endretDato = DateUtils.toOffsetDateTime(aktivitetData.opprettetDato),

            antallStillingerSokes = sokeAvtaleAktivitetData.getAntallStillingerSokes(),
            antallStillingerIUken = sokeAvtaleAktivitetData.getAntallStillingerIUken(),
            avtaleOppfolging = sokeAvtaleAktivitetData.getAvtaleOppfolging(),
            )

    }

    private fun mapTilIjobb(aktivitetData: AktivitetData): Ijobb {
        val iJobbAktivitetData: IJobbAktivitetData? = aktivitetData.iJobbAktivitetData
        val jobbStatusType: JobbStatusTypeData? = iJobbAktivitetData?.jobbStatusType
        return Ijobb(
            aktivitetType = AktivitetType.ijobb,
            avtaltMedNav = aktivitetData.isAvtalt,
            aktivitetId = aktivitetData.id.toString(),
            kontorsperreEnhetId = aktivitetData.kontorsperreEnhetId,
            oppfolgingsperiodeId = aktivitetData.oppfolgingsperiodeId,
            status = Status.valueOf(aktivitetData.status.name),
            beskrivelse = aktivitetData.beskrivelse,
            tittel = aktivitetData.tittel,
            fraDato = DateUtils.toOffsetDateTime(aktivitetData.fraDato),
            tilDato = DateUtils.toOffsetDateTime(aktivitetData.tilDato),
            opprettetDato = DateUtils.toOffsetDateTime(aktivitetData.opprettetDato),
            endretDato = DateUtils.toOffsetDateTime(aktivitetData.opprettetDato),

            jobbStatusType = jobbStatusType?.let { it.name }?.let { Ijobb.JobbStatusType.valueOf(it) },
            ansettelsesforhold= iJobbAktivitetData?.ansettelsesforhold,
            arbeidstid= iJobbAktivitetData?.arbeidstid,
        )
    }

    private fun mapTilBehandling(aktivitetData: AktivitetData): Behandling {
        val behandlingAktivitetData = aktivitetData.behandlingAktivitetData
        return Behandling(
            aktivitetType = AktivitetType.behandling,
            avtaltMedNav = aktivitetData.isAvtalt,
            aktivitetId = aktivitetData.id.toString(),
            kontorsperreEnhetId = aktivitetData.kontorsperreEnhetId,
            oppfolgingsperiodeId = aktivitetData.oppfolgingsperiodeId,
            status = Status.valueOf(aktivitetData.status.name),
            beskrivelse = aktivitetData.beskrivelse,
            tittel = aktivitetData.tittel,
            fraDato = DateUtils.toOffsetDateTime(aktivitetData.fraDato),
            tilDato = DateUtils.toOffsetDateTime(aktivitetData.tilDato),
            opprettetDato = DateUtils.toOffsetDateTime(aktivitetData.opprettetDato),
            endretDato = DateUtils.toOffsetDateTime(aktivitetData.opprettetDato),

            behandlingType = behandlingAktivitetData.behandlingType,
            behandlingSted = behandlingAktivitetData.behandlingSted,
            effekt = behandlingAktivitetData.effekt,
            behandlingOppfolging = behandlingAktivitetData.behandlingOppfolging
        )
    }

    private fun mapTilMote(aktivitetData: AktivitetData): Mote {
        val moteData = aktivitetData.moteData
        return Mote(
            aktivitetType = AktivitetType.mote,
            avtaltMedNav = aktivitetData.isAvtalt,
            aktivitetId = aktivitetData.id.toString(),
            kontorsperreEnhetId = aktivitetData.kontorsperreEnhetId,
            oppfolgingsperiodeId = aktivitetData.oppfolgingsperiodeId,
            status = Status.valueOf(aktivitetData.status.name),
            beskrivelse = aktivitetData.beskrivelse,
            tittel = aktivitetData.tittel,
            fraDato = DateUtils.toOffsetDateTime(aktivitetData.fraDato),
            tilDato = DateUtils.toOffsetDateTime(aktivitetData.tilDato),
            opprettetDato = DateUtils.toOffsetDateTime(aktivitetData.opprettetDato),
            endretDato = DateUtils.toOffsetDateTime(aktivitetData.opprettetDato),

            adresse = moteData.adresse,
            forberedelser = moteData.forberedelser,
            kanal = Mote.Kanal.valueOf(moteData.kanal.name),
            referat = moteData.referat,
            referatPublisert = moteData.isReferatPublisert
        )
    }

    private fun mapTilSamtalereferat(aktivitetData: AktivitetData): Samtalereferat {
        val moteData = aktivitetData.moteData
        return Samtalereferat(
            aktivitetType = AktivitetType.samtalereferat,
            avtaltMedNav = aktivitetData.isAvtalt,
            aktivitetId = aktivitetData.id.toString(),
            kontorsperreEnhetId = aktivitetData.kontorsperreEnhetId,
            oppfolgingsperiodeId = aktivitetData.oppfolgingsperiodeId,
            status = Status.valueOf(aktivitetData.status.name),
            beskrivelse = aktivitetData.beskrivelse,
            tittel = aktivitetData.tittel,
            fraDato = DateUtils.toOffsetDateTime(aktivitetData.fraDato),
            tilDato = DateUtils.toOffsetDateTime(aktivitetData.tilDato),
            opprettetDato = DateUtils.toOffsetDateTime(aktivitetData.opprettetDato),
            endretDato = DateUtils.toOffsetDateTime(aktivitetData.opprettetDato),

            kanal = Samtalereferat.Kanal.valueOf(moteData.kanal.name),
            referat = moteData.referat,
            referatPublisert = moteData.isReferatPublisert
        )
    }

    private fun mapTilStillingFraNav(aktivitetData: AktivitetData): StillingFraNav {
        val stillingFraNavData = aktivitetData.stillingFraNavData
        val stillingFraNavCvKanDelesData: StillingFraNavAllOfCvKanDelesData? =
            mapCvKanDelesData(stillingFraNavData.cvKanDelesData)
        val soknadsstatusEnum = stillingFraNavData.soknadsstatus
            ?.let { it.name }?.let { StillingFraNav.Soknadsstatus.valueOf(it) }

        return StillingFraNav(
            aktivitetType = AktivitetType.stillingFraNav,
            avtaltMedNav = aktivitetData.isAvtalt,
            aktivitetId = aktivitetData.id.toString(),
            kontorsperreEnhetId = aktivitetData.kontorsperreEnhetId,
            oppfolgingsperiodeId = aktivitetData.oppfolgingsperiodeId,
            status = Status.valueOf(aktivitetData.status.name),
            beskrivelse = aktivitetData.beskrivelse,
            tittel = aktivitetData.tittel,
            fraDato = DateUtils.toOffsetDateTime(aktivitetData.fraDato),
            tilDato = DateUtils.toOffsetDateTime(aktivitetData.tilDato),
            opprettetDato = DateUtils.toOffsetDateTime(aktivitetData.opprettetDato),
            endretDato = DateUtils.toOffsetDateTime(aktivitetData.opprettetDato),

            cvKanDelesData = stillingFraNavCvKanDelesData,
            soknadsfrist = stillingFraNavData.soknadsfrist,
            svarfrist = DateUtils.toLocalDate(stillingFraNavData.svarfrist),
            arbeidsgiver = stillingFraNavData.arbeidsgiver,
            bestillingsId = stillingFraNavData.bestillingsId,
            stillingsId = stillingFraNavData.stillingsId,
            arbeidssted = stillingFraNavData.arbeidssted,
            soknadsstatus = soknadsstatusEnum
        )
    }

    private fun mapCvKanDelesData(cvKanDelesData: CvKanDelesData?): StillingFraNavAllOfCvKanDelesData? {
        if (cvKanDelesData == null) return null
        return StillingFraNavAllOfCvKanDelesData(
            kanDeles = cvKanDelesData.kanDeles,
            endretTidspunkt = DateUtils.toOffsetDateTime(cvKanDelesData.endretTidspunkt),
            endretAv = cvKanDelesData.endretAv,
            endretAvType = StillingFraNavAllOfCvKanDelesData.EndretAvType.valueOf(cvKanDelesData.endretAvType.name),
            avtaltDato = DateUtils.toLocalDate(cvKanDelesData.avtaltDato)
        )
    }
}
