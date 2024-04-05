package no.nav.veilarbaktivitet.config.ownerProviders

import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.types.identer.Fnr
import no.nav.veilarbaktivitet.arena.model.ArenaId
import org.springframework.stereotype.Component

@Component
class ForhaandsorienteringOwnerProvider(val ownerProviderDAO: OwnerProviderDAO, val aktorOppslagClient: AktorOppslagClient) {
    fun getOwner(fhoId: String): Fnr? {
        return ownerProviderDAO.getForhaandsorienteringOwner(ArenaId(fhoId))
            ?.let { aktorOppslagClient.hentFnr(it.otherAktorId()) }
    }
}
