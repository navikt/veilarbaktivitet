package no.nav.veilarbaktivitet.config.ownerProviders

import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.types.identer.Fnr
import org.springframework.stereotype.Component

@Component
class AktivitetOwnerProvider(val ownerProviderDAO: OwnerProviderDAO, val aktorOppslagClient: AktorOppslagClient) {
    fun getOwner(aktivitetId: String): Fnr? {
        return ownerProviderDAO.getAktivitetOwner(aktivitetId.toLong())
            ?.let { aktorOppslagClient.hentFnr(it.otherAktorId()) }
    }
}