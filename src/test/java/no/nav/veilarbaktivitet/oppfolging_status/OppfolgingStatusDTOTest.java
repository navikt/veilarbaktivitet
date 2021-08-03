package no.nav.veilarbaktivitet.oppfolging_status;

import no.nav.common.json.JsonUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;

import static java.nio.charset.StandardCharsets.UTF_8;

public class OppfolgingStatusDTOTest {

    String jsonString;

    @Before
    public void setup() {
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        Resource resource = resourceLoader.getResource("classpath:oppfolging_status/get-oppfolgingsstatus-response.json");
        try (Reader reader = new InputStreamReader(resource.getInputStream(), UTF_8)) {
            jsonString = FileCopyUtils.copyToString(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    public void testJsonIgnore() {
        OppfolgingStatusDTO oppfolgingStatusDTO = JsonUtils.fromJson(jsonString, OppfolgingStatusDTO.class);
        Assertions.assertThat(oppfolgingStatusDTO)
                .hasFieldOrPropertyWithValue("reservasjonKRR", true)
                .hasFieldOrPropertyWithValue("manuell", true)
                .hasFieldOrPropertyWithValue("underOppfolging", true);
    }

}