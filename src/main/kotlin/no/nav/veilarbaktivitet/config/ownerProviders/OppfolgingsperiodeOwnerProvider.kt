package no.nav.veilarbaktivitet.config.ownerProviders

import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.types.identer.Fnr
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class OppfolgingsperiodeOwnerProvider(val ownerProviderDAO: OwnerProviderDAO, val aktorOppslagClient: AktorOppslagClient) {
    fun getOwner(oppfolgingsperiodeId: UUID): Fnr? {
        return ownerProviderDAO.getOppfolgingsperiodeOwner(oppfolgingsperiodeId)
            ?.let { aktorOppslagClient.hentFnr(it.otherAktorId()) }
    }
}