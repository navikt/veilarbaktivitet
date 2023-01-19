package no.nav.veilarbarktivitet

import no.nav.veilarbaktivitet.aktivitetskort.dto.IdentType

data class KotlinIdent(
    val ident: String,
    val identType: IdentType
)