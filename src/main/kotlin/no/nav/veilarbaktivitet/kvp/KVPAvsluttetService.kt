package no.nav.veilarbaktivitet.kvp

import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTransaksjonsType
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData
import no.nav.veilarbaktivitet.oversikten.OversiktenService
import no.nav.veilarbaktivitet.person.Person
import org.springframework.stereotype.Service
import java.util.*

@Service
class KVPAvsluttetService(
    private val aktivitetDAO: AktivitetDAO,
    private val oversiktenService: OversiktenService
) {

    private fun settAktiviteterInomKVPPeriodeTilAvbrutt(
        aktoerId: Person.AktorId,
        avsluttetBegrunnelse: String,
        kvpAvsluttetTidspunkt: Date
    ) {
        aktivitetDAO.hentAktiviteterForAktorId(aktoerId)
            .filter(::filtrerKontorSperretOgStatusErIkkeAvBruttEllerFullfort)
            .filter(::filtrerEksterneAktiviteter)
            .filter { aktivitet -> aktivitet.opprettetDato.before(kvpAvsluttetTidspunkt) }
            .map { aktivitet ->
                settKVPAktivitetTilAvbrutt(
                    aktivitet,
                    avsluttetBegrunnelse,
                    kvpAvsluttetTidspunkt
                )
            }
            .forEach { oppdatertAktivitet ->
                aktivitetDAO.oppdaterAktivitet(oppdatertAktivitet)
                if (erAvbruttMoteEllerSamtalereferat(oppdatertAktivitet)) {
                    oversiktenService.lagreStoppMeldingOmUdeltSamtalereferatIUtboks(
                        oppdatertAktivitet.aktorId, oppdatertAktivitet.id
                    )
                }
            }
    }

    fun håndterKvpAvsluttet(aktoerId: Person.AktorId, begrunnelse: Optional<String>, sluttDato: Optional<Date>) {
        val kvpAvsluttetTidspunkt =
            sluttDato.orElseThrow { IllegalArgumentException("Mangler sluttDato/avsluttet for KVP AVSLUTTET") }
        settAktiviteterInomKVPPeriodeTilAvbrutt(aktoerId, begrunnelse.orElse("KVP avsluttet"), kvpAvsluttetTidspunkt)
    }


    private fun filtrerKontorSperretOgStatusErIkkeAvBruttEllerFullfort(aktivitetData: AktivitetData): Boolean {
        return aktivitetData.kontorsperreEnhetId != null && erAktiv(aktivitetData)
    }

    private fun erAktiv(aktivitetData: AktivitetData): Boolean {
        val aktivitetStatus = aktivitetData.status
        return aktivitetStatus != AktivitetStatus.AVBRUTT && aktivitetStatus != AktivitetStatus.FULLFORT
    }

    private fun filtrerEksterneAktiviteter(aktivitetData: AktivitetData): Boolean {
        return AktivitetTypeData.EKSTERNAKTIVITET != aktivitetData.aktivitetType
    }

    private fun settKVPAktivitetTilAvbrutt(
        aktivitetData: AktivitetData,
        avsluttetBegrunnelse: String,
        avsluttetDato: Date
    ): AktivitetData {
        return aktivitetData
            .withTransaksjonsType(AktivitetTransaksjonsType.STATUS_ENDRET)
            .withStatus(AktivitetStatus.AVBRUTT)
            .withAvsluttetKommentar(avsluttetBegrunnelse)
            .withEndretDato(avsluttetDato)
    }

    private fun erAvbruttMoteEllerSamtalereferat(aktivitet: AktivitetData): Boolean {
        return (aktivitet.aktivitetType == AktivitetTypeData.MOTE ||
                aktivitet.aktivitetType == AktivitetTypeData.SAMTALEREFERAT) &&
                aktivitet.status == AktivitetStatus.AVBRUTT
    }
}
