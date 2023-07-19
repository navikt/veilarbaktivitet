package no.nav.veilarbaktivitet.aktivitetskort.service

import lombok.RequiredArgsConstructor
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO
import no.nav.veilarbaktivitet.aktivitet.AktivitetService
import no.nav.veilarbaktivitet.aktivitet.KasseringDAO
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.KasseringsBestilling
import no.nav.veilarbaktivitet.aktivitetskort.feil.AktivitetIkkeFunnetFeil
import no.nav.veilarbaktivitet.aktivitetskort.feil.ErrorMessage
import no.nav.veilarbaktivitet.person.Person
import org.springframework.stereotype.Service

@Service
@RequiredArgsConstructor
class KasseringsService(
    private val aktivitetDAO: AktivitetDAO,
    private val aktivitetService: AktivitetService,
    private val kasseringDAO: KasseringDAO,
) {

    @Throws(AktivitetIkkeFunnetFeil::class)
    fun kassertAktivitet(kasseringsBestilling: KasseringsBestilling) {
        val aktivitet = aktivitetDAO.hentAktivitetByFunksjonellId(kasseringsBestilling.aktivitetsId)
        if (aktivitet.isEmpty) throw AktivitetIkkeFunnetFeil(
            ErrorMessage("Kan ikke kassere aktivitet som ikke finnes"),
            null
        ) else {
            val navIdent = Person.navIdent(kasseringsBestilling.navIdent.get())
            kasserAktivitetMedTekniskId(aktivitet.get(), navIdent, kasseringsBestilling.begrunnelse)
        }
    }

    private fun kasserAktivitetMedTekniskId(aktivitet: AktivitetData, navIdent: Person.NavIdent, begrunnelse: String?) {
        if (aktivitet.getStatus() != AktivitetStatus.AVBRUTT && aktivitet.getStatus() != AktivitetStatus.FULLFORT) {
            aktivitetService.oppdaterStatus(
                aktivitet,
                aktivitet.withStatus(AktivitetStatus.AVBRUTT),
                navIdent.tilIdent()
            )
        }
        kasseringDAO.kasserAktivitetMedBegrunnelse(aktivitet.getId(), begrunnelse)
    }
}
