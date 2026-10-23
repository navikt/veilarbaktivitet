package no.nav.veilarbaktivitet.admin

import lombok.RequiredArgsConstructor
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTransaksjonsType.KASSERT
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.KasseringsBestilling
import no.nav.veilarbaktivitet.aktivitetskort.feil.AktivitetIkkeFunnetFeil
import no.nav.veilarbaktivitet.aktivitetskort.feil.ErrorMessage
import no.nav.veilarbaktivitet.oversikten.OversiktenService
import no.nav.veilarbaktivitet.person.Innsender
import no.nav.veilarbaktivitet.person.Person
import no.nav.veilarbaktivitet.veilarbportefolje.AktivitetKafkaProducerService
import no.nav.veilarbaktivitet.veilarbportefolje.EndringsType
import no.nav.veilarbaktivitet.veilarbportefolje.dto.AktivitetTypeDTO
import no.nav.veilarbaktivitet.veilarbportefolje.dto.KafkaAktivitetMeldingV4
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.*

@Service
@RequiredArgsConstructor
class KasseringsService(
    private val aktivitetDAO: AktivitetDAO,
    private val kasseringDAO: KasseringDAO,
    private val oversiktenService: OversiktenService,
    private val aktivitetKafkaProducerService: AktivitetKafkaProducerService,
) {

    fun kasserAktivitet(aktivitet: AktivitetData, ident: Person.NavIdent, begrunnelse: String? = null) {
        val oppdatertAktivitet = aktivitet.toBuilder()
            .endretAv(ident.get())
            .endretDato(Date())
            .endretAvType(Innsender.NAV)
            .transaksjonsType(KASSERT)
            .status(AktivitetStatus.AVBRUTT).build()
        aktivitetDAO.oppdaterAktivitet(oppdatertAktivitet)

        if (aktivitet.aktivitetType == AktivitetTypeData.MOTE || aktivitet.aktivitetType == AktivitetTypeData.SAMTALEREFERAT) {
            oversiktenService.lagreStoppMeldingOmUdeltSamtalereferatIUtboks(aktivitet.aktorId, aktivitet.id)
        }

        return kasseringDAO.kasserAktivitetMedBegrunnelse(
            aktivitet.id,
            ident,
            begrunnelse
        )
    }

    @Throws(AktivitetIkkeFunnetFeil::class)
    fun kasserAktivitet(kasseringsBestilling: KasseringsBestilling) {
        val aktivitet = aktivitetDAO.hentAktivitetByFunksjonellId(kasseringsBestilling.aktivitetsId)
        if (aktivitet.isEmpty) throw AktivitetIkkeFunnetFeil(
            ErrorMessage("Kan ikke kassere aktivitet som ikke finnes"),
            null
        ) else {
            val navIdent = Person.navIdent(kasseringsBestilling.navIdent.get())
            kasserAktivitet(aktivitet.get(), navIdent, kasseringsBestilling.begrunnelse)
        }
    }

    @Transactional
    fun slettAktivitet(aktivitet: AktivitetData) {
        if (aktivitet.fhoId.isNullOrBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Aktiviteter med forhåndsorientering kan ikke slettes")
        }

        // Vi sletter kun EKSTERNAKTIVITET. Aktiviteter med forhåndsorientering, møter som kan ha SMS-varsling,
        // aktiviteter aktive filtre i porteføljen eller aktiviteter med uløste Min Side-varsler bør ikke slettes via admin.
        if (aktivitet.aktivitetType != AktivitetTypeData.EKSTERNAKTIVITET) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Kun aktiviteter av typen EKSTERNAKTIVITET kan slettes")
        }

        val kafkaMelding = lagKafkaMeldingForSletting(aktivitet)
        aktivitetKafkaProducerService.sendAktivitetMelding(kafkaMelding)
        kasseringDAO.slettAktivitet(aktivitet.id)
    }

    private fun lagKafkaMeldingForSletting(aktivitet: AktivitetData): KafkaAktivitetMeldingV4 {
        val eksternAktivitet = aktivitet.eksternAktivitetData
            ?: throw IllegalStateException("Kan ikke slette EKSTERNAKTIVITET uten ekstern aktivitet-data")
        val tiltakskode = eksternAktivitet.tiltaksKode
            ?: AktivitetTypeDTO.aktivitetsKortTypeToArenaTiltakskode(eksternAktivitet.type)
        val oppfolgingsperiodeId = aktivitet.oppfolgingsperiodeId?.toString()
            ?: throw IllegalStateException("Kan ikke slette aktivitet ${aktivitet.id} uten oppfølgingsperiode")

        return KafkaAktivitetMeldingV4.builder()
            .aktivitetId(aktivitet.id.toString())
            .version(aktivitet.versjon)
            .aktorId(aktivitet.aktorId.get())
            .fraDato(aktivitet.fraDato)
            .tilDato(aktivitet.tilDato)
            .endretDato(aktivitet.endretDato)
            .aktivitetType(AktivitetTypeDTO.TILTAK)
            .aktivitetStatus(AktivitetStatus.AVBRUTT)
            .endringsType(EndringsType.HISTORISK)
            .lagtInnAv(aktivitet.endretAvType)
            .avtalt(aktivitet.isAvtalt)
            .historisk(aktivitet.historiskDato != null)
            .tiltakskode(tiltakskode)
            .oppfolgingsperiodeId(oppfolgingsperiodeId)
            .build()
    }
}
