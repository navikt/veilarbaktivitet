package no.nav.veilarbaktivitet.util

fun Float?.toStringWithoutNullDecimals(): String? {
    if (this == null) return null
    val stripDecimals = this % 1 == 0F
    return if (stripDecimals) Math.round(this).toString() else this.toString()
}