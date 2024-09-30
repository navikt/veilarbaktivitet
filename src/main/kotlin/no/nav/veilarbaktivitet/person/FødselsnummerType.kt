package no.nav.veilarbaktivitet.person

enum class FødselsnummerType(val førsteMånedssifferISyntetiskFnrPlussetMed: Int) {
    ORDINÆRT(0),
    DOLLY(4),
    TEST_NORGE(8);

    companion object {
        fun fødselsnummerType(fnr: String): FødselsnummerType {
            val førsteMånedssiffer = Integer.parseInt(fnr.get(2).toString())
            return when {
                førsteMånedssiffer >= TEST_NORGE.førsteMånedssifferISyntetiskFnrPlussetMed -> TEST_NORGE
                førsteMånedssiffer >= DOLLY.førsteMånedssifferISyntetiskFnrPlussetMed -> DOLLY
                else -> ORDINÆRT
            }
        }
    }
}

fun tilOrdinærtFødselsnummerFormat(fnr: String): String {
    val fødselsnummerType = FødselsnummerType.fødselsnummerType(fnr)
    val førsteMånedssifferIFnrMåReduseresMed = fødselsnummerType.førsteMånedssifferISyntetiskFnrPlussetMed
    val korrigertFørsteMånedssiffer = Integer.parseInt(fnr.get(2).toString()) - førsteMånedssifferIFnrMåReduseresMed
    return fnr.replaceRange(2,3, korrigertFørsteMånedssiffer.toString())
}
