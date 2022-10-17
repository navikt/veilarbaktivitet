package no.nav.veilarbaktivitet.aktivitetskort;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.log4j.Log4j;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.json.JsonUtils;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;

@Slf4j
public class AktivitetskortCompareUtil {
    private AktivitetskortCompareUtil() {}

    public static boolean erFaktiskOppdatert(AktivitetData innkommende, AktivitetData eksisterende) {
        var mapper = JsonUtils.getMapper();
        var eksisterendeMedRelevanteFelter = eksisterende
                .withId(null)
                .withForhaandsorientering(null)
                .withOppfolgingsperiodeId(null)
                .withVersjon(null)
                .withTransaksjonsType(null)
                .withStatus(null);
        // Innkommende vil aldri ha interne tekniske ider, transaksjonstype eller oppfølgingsperiode
        var innkommendeMedRelevanteFelter = innkommende
                .withId(null)
                .withForhaandsorientering(null)
                .withOppfolgingsperiodeId(null)
                .withVersjon(null)
                .withTransaksjonsType(null)
                .withStatus(null);
        try {
            return !mapper.writeValueAsString(eksisterendeMedRelevanteFelter)
                .equals(mapper.writeValueAsString(innkommendeMedRelevanteFelter));
            } catch (JsonProcessingException e) {
            throw new IllegalStateException("Kunne ikke parse aktiviteter for sammenligning", e);
        }

    }

}
