package no.nav.veilarbaktivitet.domain;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SmsAktivitetDataTest {

    @BeforeAll
    static void setup() {
        System.setProperty("AKTIVITETSPLAN_URL", "aktivitesplan_url");
    }

    @Test
    void skalSendeServicevarselUtenSmsTid() {
        SmsAktivitetData build = SmsAktivitetData
                .builder()
                .moteTidAktivitet(ZonedDateTime.now())
                .build();

        assertThat(build.skalSendeServicevarsel()).isTrue();
    }

    @Test
    void SkalSendeServicevarselUlikSMSTid() {
        ZonedDateTime now = ZonedDateTime.now();

        SmsAktivitetData build = SmsAktivitetData
                .builder()
                .moteTidAktivitet(now)
                .smsSendtMoteTid(now.minusDays(1))
                .build();

        assertThat(build.skalSendeServicevarsel()).isTrue();
    }

    @Test
    void skalIkkeSendeServiceVarselForLiksSMSMoteTidOgLikKanal() {
        ZonedDateTime now = ZonedDateTime.now();

        SmsAktivitetData build = SmsAktivitetData
                .builder()
                .smsKanal("")
                .aktivitetKanal("")
                .moteTidAktivitet(now)
                .smsSendtMoteTid(now)
                .build();

        assertThat(build.skalSendeServicevarsel()).isFalse();
    }

    @Test
    void SkalSendeServicevarselUlikKanal() {
        ZonedDateTime now = ZonedDateTime.now();

        SmsAktivitetData build = SmsAktivitetData
                .builder()
                .smsKanal(KanalDTO.INTERNETT.toString())
                .aktivitetKanal(KanalDTO.TELEFON.toString())
                .moteTidAktivitet(now)
                .smsSendtMoteTid(now)
                .build();

        assertThat(build.skalSendeServicevarsel()).isTrue();
    }


    @Test
    void formatertMoteTid() {

        ZonedDateTime date = ZonedDateTime.of(2020, 4, 2, 10, 1, 0, 0, ZoneId.systemDefault());
        String moteTid = SmsAktivitetData
                .builder()
                .moteTidAktivitet(date)
                .build()
                .formatertMoteTid();

        assertThat(moteTid).isEqualTo("2. april 2020 kl. 10:01");
    }

    @Test
    void formatertMoteTidSkalVere24TimersKlokke() {
        ZonedDateTime date = ZonedDateTime.of(2020, 4, 2, 22, 1, 0, 0, ZoneId.systemDefault());

        String moteTid = SmsAktivitetData
                .builder()
                .moteTidAktivitet(date)
                .build()
                .formatertMoteTid();

        assertThat(moteTid).isEqualTo("2. april 2020 kl. 22:01");
    }

    @Test
    void formatertMoteTidSkalStotteDbFormat() {
        Timestamp timestamp = Timestamp.valueOf("2020-09-25 13:00:50.865939");
        ZonedDateTime date = timestamp.toLocalDateTime().atZone(ZoneId.systemDefault());

        String moteTid = SmsAktivitetData
                .builder()
                .moteTidAktivitet(date)
                .build()
                .formatertMoteTid();

        assertThat(moteTid).isEqualTo("25. september 2020 kl. 13:00");
    }

    @Test
    void url() {
        String url = SmsAktivitetData.builder().aktivitetId(1L).build().url();

        assertThat(url).isEqualTo("aktivitesplan_url/aktivitet/vis/1");
    }
}
