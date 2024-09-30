package no.nav.veilarbaktivitet.foresatteinnsynsrett

import no.nav.poao.dab.spring_a2_annotations.auth.AuthorizeFnr
import no.nav.poao.dab.spring_auth.IAuthService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate
import java.time.Period


@RestController
@RequestMapping("/api/ekstern/innsynsrett")
class InnsynrettController(private val authService: IAuthService) {
    
    @GetMapping()
    @AuthorizeFnr(auditlogMessage = "sjekker foresatte innsynsrett")
    fun foresatteHarInnsynsrett(): InnsynsrettDTO {
        val fnr = if (authService.erEksternBruker()) {
            authService.getLoggedInnUser()
        } else  {
            throw ResponseStatusException(HttpStatus.FORBIDDEN)
        }

        return InnsynsrettDTO(foresatteHarInnsynsrett = isUnder18(fnr.get())) // Adjust return value as needed
    }

    data class InnsynsrettDTO(
        val foresatteHarInnsynsrett: Boolean
    )

    private fun isUnder18(fødselsnummer: String): Boolean {
        val fnr = if (erSyntetiskFødselsnummer(fødselsnummer)) {
            tilOrdinærtFødselsnummerFormat(fødselsnummer)
        } else fødselsnummer

        val dag = fnr.substring(0, 2).toInt()
        val måned = fnr.substring(2, 4).toInt()
        val år = fnr.substring(4, 6).toInt()

        val indexTilSifferSomBestemerÅrHundre = 6
        val århundre = if (Integer.parseInt(fnr[indexTilSifferSomBestemerÅrHundre].toString()) < 4) 1900 else 2000
        val fødselsår = århundre + år

        val fødselsdato = LocalDate.of(fødselsår, måned, dag)
        val dagensdato = LocalDate.now()
        val alder = Period.between(fødselsdato, dagensdato).years

        return alder < 18
    }

    private val førsteMånedssifferISyntetiskFnrPlussetMed = 8

    fun erSyntetiskFødselsnummer(fnr: String) =
        Integer.parseInt(fnr.get(2).toString()) >= førsteMånedssifferISyntetiskFnrPlussetMed

    fun tilOrdinærtFødselsnummerFormat(fnr: String): String {
        val korrigertFørsteMånedssiffer = Integer.parseInt(fnr.get(2).toString()) - førsteMånedssifferISyntetiskFnrPlussetMed
        return fnr.replaceRange(2,3, korrigertFørsteMånedssiffer.toString())
    }
}