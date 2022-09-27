package no.nav.veilarbaktivitet.aktivitetskort;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.nav.common.json.JsonUtils;
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class TiltaksAktivitetTest {
    @Test
    public void should_compare_correctly() {
        var endret = Date.from(Instant.now());
        var funkId = UUID.randomUUID();
        var gammelAktivitet = AktivitetDataTestBuilder.nyTiltaksaktivitet()
                .withEndretDato(endret)
                .withFunksjonellId(funkId);
        var nyAktivitet = gammelAktivitet
                .withTiltaksaktivitetData(gammelAktivitet
                        .getTiltaksaktivitetData()
                        .withDeltakelseStatus("MOETE")
                );

        var mapper = JsonUtils.getMapper();

        try {
            var g = gammelAktivitet
                    .withStatus(null)
                    .withId(null)
                    .withForhaandsorientering(null)
                    .withVersjon(null);
            var ny = nyAktivitet
                    .withStatus(null)
                    .withId(null)
                    .withForhaandsorientering(null)
                    .withVersjon(null);
            var jsonStringGammel = mapper.writeValueAsString(g);
            var tree = mapper.readTree(jsonStringGammel);
            var jsonStringNy = mapper.writeValueAsString(ny);
            var tree2 = mapper.readTree(jsonStringNy);
            Assertions.assertEquals(tree, tree2);

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

}
