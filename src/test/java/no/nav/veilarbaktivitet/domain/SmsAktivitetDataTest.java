package no.nav.veilarbaktivitet.domain;

import no.nav.veilarbaktivitet.aktivitet.dto.KanalDTO;
import no.nav.veilarbaktivitet.motesms.gammel.SmsAktivitetData;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.Instant;
import java.util.Calendar;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class SmsAktivitetDataTest {

    @BeforeClass
    public static void setup() {
        System.setProperty("AKTIVITETSPLAN_URL", "aktivitesplan_url");
    }

    @Test
    public void skalSendeServicevarselUtenSmsTid() {
        SmsAktivitetData build = SmsAktivitetData
                .builder()
                .moteTidAktivitet(new Date())
                .build();

        assertThat(build.skalSendeServicevarsel()).isTrue();
    }

    @Test
    public void SkalSendeServicevarselUlikSMSTid() {
        SmsAktivitetData build = SmsAktivitetData
                .builder()
                .moteTidAktivitet(new Date())
                .smsSendtMoteTid(new Date(0))
                .build();

        assertThat(build.skalSendeServicevarsel()).isTrue();
    }

    @Test
    public void skalIkkeSendeServiceVarselForLiksSMSMoteTidOgLikKanal() {
        Date date = new Date();

        SmsAktivitetData build = SmsAktivitetData
                .builder()
                .smsKanal("")
                .aktivitetKanal("")
                .moteTidAktivitet(date)
                .smsSendtMoteTid(date)
                .build();

        assertThat(build.skalSendeServicevarsel()).isFalse();
    }

    @Test
    public void SkalSendeServicevarselUlikKanal() {
        Date date = new Date();

        SmsAktivitetData build = SmsAktivitetData
                .builder()
                .smsKanal(KanalDTO.INTERNETT.toString())
                .aktivitetKanal(KanalDTO.TELEFON.toString())
                .moteTidAktivitet(date)
                .smsSendtMoteTid(date)
                .build();

        assertThat(build.skalSendeServicevarsel()).isTrue();
    }


    @Test
    public void formatertMoteTid() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2020, Calendar.APRIL, 2, 10, 1);
        Instant instant = calendar.toInstant();
        Date date = new Date(instant.toEpochMilli());

        String moteTid = SmsAktivitetData
                .builder()
                .moteTidAktivitet(date)
                .build()
                .formatertMoteTid();

        assertThat(moteTid).isEqualTo("2. april 2020 kl. 10:01");
    }

    @Test
    public void formatertMoteTidSkalVere24TimersKlokke() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2020, Calendar.APRIL, 2, 22, 1);
        Instant instant = calendar.toInstant();
        Date date = new Date(instant.toEpochMilli());

        String moteTid = SmsAktivitetData
                .builder()
                .moteTidAktivitet(date)
                .build()
                .formatertMoteTid();

        assertThat(moteTid).isEqualTo("2. april 2020 kl. 22:01");
    }

    @Test
    public void url() {
        String url = SmsAktivitetData.builder().aktivitetId(1L).build().url();

        assertThat(url).isEqualTo("aktivitesplan_url/aktivitet/vis/1");
    }
}
