package no.nav.veilarbaktivitet.config

import no.nav.poao.dab.spring_a2_annotations.auth.*
import no.nav.veilarbaktivitet.config.ownerProviders.AktivitetOwnerProvider
import no.nav.veilarbaktivitet.config.ownerProviders.ForhaandsorienteringOwnerProvider
import no.nav.veilarbaktivitet.config.ownerProviders.OppfolgingsperiodeOwnerProvider
import org.springframework.stereotype.Component
import java.util.*
import kotlin.reflect.KClass

object ArenaAktivitetResource: ResourceType
object AktivitetResource: ResourceType
object ForhaandsorienteringResource: ResourceType
object OppfolgingsperiodeResource: ResourceType

@Component
class VeilarbaktivitetOwnerProvider(
    val aktivitetOwnerProvider: AktivitetOwnerProvider,
    val forhaandsorienteringOwnerProvider: ForhaandsorienteringOwnerProvider,
    val oppfolgingsperiodeOwnerProvider: OppfolgingsperiodeOwnerProvider
): OwnerProvider {
    override fun getOwner(resourceId: String, resourceType: KClass<out ResourceType>): OwnerResult {
        return when (resourceType) {
            AktivitetResource::class -> aktivitetOwnerProvider.getOwner(resourceId)
            ForhaandsorienteringResource::class -> forhaandsorienteringOwnerProvider.getOwner(resourceId)
            OppfolgingsperiodeResource::class -> oppfolgingsperiodeOwnerProvider.getOwner(UUID.fromString(resourceId))
            else -> null
        }
            ?.let { OwnerResultSuccess(fnr = it, enhetId = null) }
            ?: ResourceNotFound
    }
}

