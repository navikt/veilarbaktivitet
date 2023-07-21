package no.nav.veilarbaktivitet.aktivitetskort

import com.fasterxml.jackson.core.JsonProcessingException
import lombok.extern.slf4j.Slf4j
import no.nav.common.json.JsonUtils
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData

@Slf4j
object AktivitetskortCompareUtil {
    fun erFaktiskOppdatert(innkommende: AktivitetData, eksisterende: AktivitetData): Boolean {
        val mapper = JsonUtils.getMapper()
        val eksisterendeMedRelevanteFelter = eksisterende
            .withId(null)
            .withAvtalt(false) //ignorere avtalt - håndteres i eget kall
            .withForhaandsorientering(null)
            .withOppfolgingsperiodeId(null)
            .withVersjon(null)
            .withTransaksjonsType(null)
            .withStatus(null)
            .withEndretAv(null)
            .withEndretDato(null)
            .withEndretAvType(null)
        // Innkommende vil aldri ha interne tekniske ider, transaksjonstype eller oppfølgingsperiode
        val innkommendeMedRelevanteFelter = innkommende
            .withId(null)
            .withAvtalt(false) //ignorere avtalt - håndteres i eget kall
            .withForhaandsorientering(null)
            .withOppfolgingsperiodeId(null)
            .withVersjon(null)
            .withTransaksjonsType(null)
            .withStatus(null)
            .withEndretAv(null)
            .withEndretDato(null)
            .withEndretAvType(null)
        return try {
            mapper.writeValueAsString(eksisterendeMedRelevanteFelter) != mapper.writeValueAsString(
                innkommendeMedRelevanteFelter
            )
        } catch (e: JsonProcessingException) {
            throw IllegalStateException("Kunne ikke parse aktiviteter for sammenligning", e)
        }
    }
}
