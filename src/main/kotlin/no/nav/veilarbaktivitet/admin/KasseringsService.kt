package no.nav.veilarbaktivitet.admin

import lombok.RequiredArgsConstructor
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.KasseringsBestilling
import no.nav.veilarbaktivitet.aktivitetskort.feil.AktivitetIkkeFunnetFeil
import no.nav.veilarbaktivitet.aktivitetskort.feil.ErrorMessage
import no.nav.veilarbaktivitet.person.Innsender
import no.nav.veilarbaktivitet.person.Person
import org.springframework.stereotype.Service
import java.util.*

@Service
@RequiredArgsConstructor
class KasseringsService(
    private val aktivitetDAO: AktivitetDAO,
    private val kasseringDAO: KasseringDAO,
) {

    fun kasserAktivitet(aktivitet: AktivitetData, ident: Person.NavIdent, begrunnelse: String? = null) {
        aktivitet.withEndretAv(ident.get())
        aktivitet.withEndretDato(Date())
        aktivitet.withEndretAvType(Innsender.NAV)
        aktivitet.withStatus(AktivitetStatus.AVBRUTT)
        aktivitetDAO.oppdaterAktivitet(aktivitet)

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
}
