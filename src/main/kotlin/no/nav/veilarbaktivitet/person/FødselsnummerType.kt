package no.nav.veilarbaktivitet.person

import java.lang.Integer.parseInt

enum class FødselsnummerType(val førsteMånedssifferIFnrPlussetMed: Int) {
    ORDINÆRT(0),
    DOLLY(4),
    TEST_NORGE(8);

    companion object {
        fun fødselsnummerType(fnr: String): FødselsnummerType {
            val førsteMånedssiffer = parseInt(fnr.get(2).toString())
            return when {
                førsteMånedssiffer >= TEST_NORGE.førsteMånedssifferIFnrPlussetMed -> TEST_NORGE
                førsteMånedssiffer >= DOLLY.førsteMånedssifferIFnrPlussetMed -> DOLLY
                else -> ORDINÆRT
            }
        }
    }
}

fun tilOrdinærtFødselsnummerFormat(fnr: String): String {
    val fødselsnummerType = FødselsnummerType.fødselsnummerType(fnr)
    val indexFørsteMånedssiffer = 2
    val korrigertFørsteMånedssiffer = parseInt(fnr.get(indexFørsteMånedssiffer).toString()) -
            fødselsnummerType.førsteMånedssifferIFnrPlussetMed
    return fnr.replaceRange(indexFørsteMånedssiffer, indexFørsteMånedssiffer + 1, korrigertFørsteMånedssiffer.toString())
}
