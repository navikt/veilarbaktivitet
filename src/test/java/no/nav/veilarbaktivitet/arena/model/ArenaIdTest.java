package no.nav.veilarbaktivitet.arena.model;

import no.nav.common.json.JsonUtils;
import org.assertj.core.api.Assertions;
import org.intellij.lang.annotations.Language;
import org.junit.Test;

public class ArenaIdTest {

    @Test
    public void should_deserialize_arenaId_correctly() {

        @Language("JSON")
        var json = """
                {
            "tiltaksnavn": "tiltaksnavn",
            "aktivitetId": "123123",
            "tiltakLokaltNavn": "lokaltnavn",
            "arrangor": "arrangor",
            "bedriftsnummer": "asd",
            "deltakelsePeriode": {
                "fom": "2021-11-18",
                "tom": "2021-11-25"
            },
            "deltakelseProsent": 60,
            "deltakerStatus": "GJENN",
            "statusSistEndret": "2021-11-18",
            "begrunnelseInnsoking": "asd",
            "antallDagerPerUke": 3.0
          }
        """;
        var tiltak = JsonUtils.fromJson(json, AktiviteterDTO.Tiltaksaktivitet.class);
        Assertions.assertThat(tiltak.getAktivitetId().id()).isEqualTo("ARENA123123");
    }

    @Test
    public void should_deserialize_null_arenaId_correctly() {

        @Language("JSON")
        var json = """
                {
            "tiltaksnavn": "tiltaksnavn",
            "aktivitetId": null,
            "tiltakLokaltNavn": "lokaltnavn",
            "arrangor": "arrangor",
            "bedriftsnummer": "asd",
            "deltakelsePeriode": {
                "fom": "2021-11-18",
                "tom": "2021-11-25"
            },
            "deltakelseProsent": 60,
            "deltakerStatus": "GJENN",
            "statusSistEndret": "2021-11-18",
            "begrunnelseInnsoking": "asd",
            "antallDagerPerUke": 3.0
          }
        """;
        var tiltak = JsonUtils.fromJson(json, AktiviteterDTO.Tiltaksaktivitet.class);
        Assertions.assertThat(tiltak.getAktivitetId()).isNull();
    }

    @Test
    public void should_serialize_areanId_correctly() {
        var akt=  new AktiviteterDTO.Tiltaksaktivitet()
                .setDeltakerStatus("GJENN")
                .setTiltaksnavn("asdas")
                .setAktivitetId(new ArenaId("asd"));
        var json = JsonUtils.toJson(akt);
        Assertions.assertThat(json).isEqualTo("""
                {"tiltaksnavn":"asdas","aktivitetId":"ARENAasd","tiltakLokaltNavn":null,"arrangor":null,"bedriftsnummer":null,"deltakelsePeriode":null,"deltakelseProsent":null,"deltakerStatus":"GJENN","statusSistEndret":null,"begrunnelseInnsoking":null,"antallDagerPerUke":null}
        """.trim());
    }

    @Test
    public void should_serialize_null_areanId_correctly() {
        var akt=  new AktiviteterDTO.Tiltaksaktivitet()
                .setDeltakerStatus("GJENN")
                .setTiltaksnavn("asdas")
                .setAktivitetId(null);
        var json = JsonUtils.toJson(akt);
        Assertions.assertThat(json).isEqualTo("""
                {"tiltaksnavn":"asdas","aktivitetId":null,"tiltakLokaltNavn":null,"arrangor":null,"bedriftsnummer":null,"deltakelsePeriode":null,"deltakelseProsent":null,"deltakerStatus":"GJENN","statusSistEndret":null,"begrunnelseInnsoking":null,"antallDagerPerUke":null}
        """.trim());
    }

}