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
@RequestMapping("/api/innsynsrett")
class InnsynrettControler(private val authService: IAuthService) {
    @GetMapping()
    @AuthorizeFnr(auditlogMessage = "sjekker foresatte innsyns rett")
    fun foresatteHarInnsynsrett(): InnsynsrettDTO {
        val fnr = if (authService.erEksternBruker()) {
            authService.getLoggedInnUser()
        } else  {
            throw ResponseStatusException(HttpStatus.FORBIDDEN)
        }
        fnr.get()

        return InnsynsrettDTO(foresatteHarInnsynsrett = isUnder18(fnr.get())) // Adjust return value as needed
    }

    data class InnsynsrettDTO(
        val foresatteHarInnsynsrett: Boolean
    )

    fun isUnder18(fnr: String): Boolean {
        val day = fnr.substring(0, 2).toInt()
        val month = fnr.substring(2, 4).toInt()
        val year = fnr.substring(4, 6).toInt()

        // Determine the century
        val indexTilSifferSomBestemerÅrHundre = 6
        val century = if (Integer.parseInt(fnr[indexTilSifferSomBestemerÅrHundre].toString()) < 4) 1900 else 2000
        val birthYear = century + year

        val birthDate = LocalDate.of(birthYear, month, day)
        val currentDate = LocalDate.now()
        val age = Period.between(birthDate, currentDate).years

        return age < 18

    }


}



//export function is18OrOlder(personNumber: string): boolean {
//    const fødtFør2000 = parseInt(personNumber.charAt(6)) < 5;
//    //500–999 omfatter personer født i perioden 2000–2039 eller 1854–1899
//    if (fødtFør2000) return true;
//
//
//    const day = parseInt(personNumber.slice(0, 2), 10);
//    const month = parseInt(personNumber.slice(2, 4), 10) - 1;
//    const year = parseInt(personNumber.slice(4, 6), 10) + 2000;
//
//    const birthDate = new Date(year, month, day);
//    const age = differenceInYears(new Date(), birthDate);
//
//    return age >= 18;
//
//
//}