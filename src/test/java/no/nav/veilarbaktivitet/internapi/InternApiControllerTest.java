package no.nav.veilarbaktivitet.internapi;

import io.restassured.response.Response;
import no.nav.veilarbaktivitet.SpringBootTestBase;
import no.nav.veilarbaktivitet.internapi.model.Aktivitet;
import no.nav.veilarbaktivitet.internapi.model.Mote;
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService;
import no.nav.veilarbaktivitet.mock_nav_modell.MockVeileder;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;


public class InternApiControllerTest extends SpringBootTestBase {
    
    @MockBean
    InternapiService internapiService;

    @Before
    public void setup() {
        Mote mote = Mote.builder()
                .aktivitetType(Aktivitet.AktivitetTypeEnum.MOTE)
                .build();
        when(internapiService.hentAktivitet(anyInt())).thenReturn(Optional.of(mote));
    }

    @Test
    public void kanari() {
        MockVeileder veileder = MockNavService.createVeileder();

        Response response = veileder.createRequest()
                .get("http://localhost:" + port + "/veilarbaktivitet/internal/api/v1/aktivitet/{aktivitetId}",1)
                .then()
                .assertThat().statusCode(HttpStatus.OK.value())
                .extract()
                .response();
        Mote mote = response.as(Mote.class);

        Assertions.assertThat(mote).isInstanceOf(Mote.class);
    }

}