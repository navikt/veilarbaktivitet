package no.nav.veilarbaktivitet.domain;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Calendar;
import java.util.Date;

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
                .moteTidAktivitet(new Date())
                .build();

        assertThat(build.skalSendeServicevarsel()).isTrue();
    }

    @Test
    void SkalSendeServicevarselUlikSMSTid() {
        SmsAktivitetData build = SmsAktivitetData
                .builder()
                .moteTidAktivitet(new Date())
                .smsSendtMoteTid(new Date(0))
                .build();

        assertThat(build.skalSendeServicevarsel()).isTrue();
    }

    @Test
    void skalIkkeSendeServiceVarselForLiksSMSMoteTid() {
        Date date = new Date();

        SmsAktivitetData build = SmsAktivitetData
                .builder()
                .moteTidAktivitet(date)
                .smsSendtMoteTid(date)
                .build();

        assertThat(build.skalSendeServicevarsel()).isFalse();
    }


    @Test
    void formatertMoteTid() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2020, Calendar.APRIL, 2, 10, 1);
        Instant instant = calendar.toInstant();
        Date date = new Date(instant.toEpochMilli());

        String moteTid = SmsAktivitetData
                .builder()
                .moteTidAktivitet(date)
                .build()
                .formatertMoteTid();

        assertThat(moteTid).isEqualTo("2. april 2020 klokken 10:01");
    }

    @Test
    void formatertMoteTidSkalVere24TimersKlokke() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2020, Calendar.APRIL, 2, 22, 1);
        Instant instant = calendar.toInstant();
        Date date = new Date(instant.toEpochMilli());

        String moteTid = SmsAktivitetData
                .builder()
                .moteTidAktivitet(date)
                .build()
                .formatertMoteTid();

        assertThat(moteTid).isEqualTo("2. april 2020 klokken 22:01");
    }

    @Test
    void url() {
        String url = SmsAktivitetData.builder().aktivitetId(1L).build().url();

        assertThat(url).isEqualTo("aktivitesplan_url/aktivitet/vis/1");
    }
}
