package no.nav.veilarbaktivitet.aktivitetskort;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.log4j.Log4j;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.json.JsonUtils;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;

@Slf4j
public class AktivitetskortCompareUtil {

    public static boolean erFaktiskOppdatert(AktivitetData innkommende, AktivitetData eksisterende) {
        var mapper = JsonUtils.getMapper();
        var eksisterendeMedRelevanteFelter = eksisterende
                .withStatus(null)
                .withId(null)
                .withForhaandsorientering(null)
                .withVersjon(null);
        var innkommendeMedRelevanteFelter = innkommende
                .withStatus(null)
                .withId(null)
                .withForhaandsorientering(null)
                .withVersjon(null);
        try {
            var eksisterendeJsonNode = mapper.readTree(mapper.writeValueAsString(eksisterendeMedRelevanteFelter));
            var innkommendeJsonNode = mapper.readTree(mapper.writeValueAsString(innkommendeMedRelevanteFelter));
            return eksisterendeJsonNode.equals(innkommendeJsonNode);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Kunne ikke parse aktiviteter for sammenligning", e);
        }

    }

}
