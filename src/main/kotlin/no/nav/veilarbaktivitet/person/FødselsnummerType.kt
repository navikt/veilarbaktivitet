package no.nav.veilarbaktivitet.person

import no.nav.veilarbaktivitet.person.FødselsnummerType.Companion.fødselsnummerType
import java.lang.Integer.parseInt

enum class FødselsnummerType {
    ORDINÆRT,
    DOLLY,
    D_NUMMER,
    TEST_NORGE;

    companion object {
        fun fødselsnummerType(fnr: String): FødselsnummerType {
            val førsteMånedssiffer = parseInt(fnr.get(2).toString())
            val førsteSiffer = parseInt(fnr.get(0).toString())

            return when {
                førsteMånedssiffer >= 8 -> TEST_NORGE
                førsteMånedssiffer >= 4 -> DOLLY
                førsteSiffer >= 4 -> D_NUMMER
                else -> ORDINÆRT
            }
        }
    }
}

fun tilOrdinærtFødselsnummerFormat(fnr: String): String {
    return when (fødselsnummerType(fnr)) {
        FødselsnummerType.ORDINÆRT -> fnr
        FødselsnummerType.DOLLY -> nedjusterFørsteMånedssiffer(fnr, 4)
        FødselsnummerType.TEST_NORGE -> nedjusterFørsteMånedssiffer(fnr, 8)
        FødselsnummerType.D_NUMMER -> nedjusterFørsteSiffer(fnr, 4)
    }
}

private fun nedjusterFørsteSiffer(fnr: String, nedjusterMed: Int): String {
    val korrigertFørsteSiffer = parseInt(fnr.get(0).toString()) - nedjusterMed
    return fnr.replaceRange(0,  1, korrigertFørsteSiffer.toString())
}

private fun nedjusterFørsteMånedssiffer(fnr: String, nedjusterMed: Int): String {
    val indexFørsteMånedssiffer = 2
    val korrigertFørsteMånedssiffer = parseInt(fnr.get(indexFørsteMånedssiffer).toString()) - nedjusterMed
    return fnr.replaceRange(indexFørsteMånedssiffer, indexFørsteMånedssiffer + 1, korrigertFørsteMånedssiffer.toString())
}