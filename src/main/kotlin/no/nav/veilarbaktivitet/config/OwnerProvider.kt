package no.nav.veilarbaktivitet.config

import no.nav.poao.dab.spring_a2_annotations.auth.*
import no.nav.veilarbaktivitet.config.ownerProviders.AktivitetOwnerProvider
import no.nav.veilarbaktivitet.config.ownerProviders.ForhaandsorienteringOwnerProvider
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

object ArenaAktivitetResource: ResourceType
object AktivitetResource: ResourceType
object ForhaandsorienteringResource: ResourceType

@Component
class VeilarbaktivitetOwnerProvider(
    val aktivitetOwnerProvider: AktivitetOwnerProvider,
    val forhaandsorienteringOwnerProvider: ForhaandsorienteringOwnerProvider
): OwnerProvider {
    override fun getOwner(resourceId: String, resourceType: KClass<out ResourceType>): OwnerResult {
        return when (resourceType) {
            AktivitetResource::class -> aktivitetOwnerProvider.getOwner(resourceId)
            ForhaandsorienteringResource::class -> forhaandsorienteringOwnerProvider.getOwner(resourceId)
            else -> null
        }
            ?.let { OwnerResultSuccess(fnr = it, enhetId = null) }
            ?: ResourceNotFound
    }
}

