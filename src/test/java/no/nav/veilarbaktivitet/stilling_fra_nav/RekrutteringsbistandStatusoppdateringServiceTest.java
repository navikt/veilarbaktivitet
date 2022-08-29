package no.nav.veilarbaktivitet.stilling_fra_nav;

import no.nav.veilarbaktivitet.SpringBootTestBase;
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData;
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukernotifikasjonService;
import no.nav.veilarbaktivitet.person.InnsenderData;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import static org.mockito.ArgumentMatchers.any;

public class RekrutteringsbistandStatusoppdateringServiceTest extends SpringBootTestBase {
    @Autowired
    RekrutteringsbistandStatusoppdateringService rekrutteringsbistandStatusoppdateringService;

    @SpyBean
    AktivitetDAO aktivitetDAO;

    @MockBean
    StillingFraNavMetrikker stillingFraNavMetrikker;

    @MockBean
    BrukernotifikasjonService brukernotifikasjonService;
    private final ArgumentCaptor<AktivitetData> aktivitetjeger = ArgumentCaptor.forClass(AktivitetData.class);

    @Before
    public void setUp() {
        Mockito.doReturn(null).when(aktivitetDAO).oppdaterAktivitet(any());
    }

    @Test
    public void happy_case_svart_ja() {
        rekrutteringsbistandStatusoppdateringService.behandleCvDelt(BESTILLINGSID, NAVIDENT, STILLING_FRA_NAV_HAR_SVART_JA);

        Mockito.verify(aktivitetDAO).oppdaterAktivitet(aktivitetjeger.capture());
        AktivitetData aktivitetData_etter = aktivitetjeger.getValue();

        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(aktivitetData_etter.getId().toString()).isEqualTo(BESTILLINGSID);
            assertions.assertThat(aktivitetData_etter.getEndretAv()).isEqualTo(NAVIDENT);
            assertions.assertThat(aktivitetData_etter.getLagtInnAv()).isSameAs(InnsenderData.NAV);
            assertions.assertThat(aktivitetData_etter.getStatus()).isSameAs(STILLING_FRA_NAV_HAR_SVART_JA.getStatus());
            assertions.assertThat(aktivitetData_etter.getStillingFraNavData()).isNotNull();
            assertions.assertThat(aktivitetData_etter.getStillingFraNavData().getSoknadsstatus()).isSameAs(Soknadsstatus.CV_DELT);
            assertions.assertThat(aktivitetData_etter.getStillingFraNavData().getLivslopsStatus()).isSameAs(STILLING_FRA_NAV_HAR_SVART_JA.getStillingFraNavData().getLivslopsStatus());
            assertions.assertAll();
        });
    }

    @Test
    public void unhappy_case_ikke_fatt_jobben() {
        rekrutteringsbistandStatusoppdateringService.behandleIkkeFattJobben(BESTILLINGSID, NAVIDENT, STILLING_FRA_NAV_HAR_SVART_JA, "Lorem ipsum dolor sit amet");

        Mockito.verify(aktivitetDAO).oppdaterAktivitet(aktivitetjeger.capture());
        AktivitetData aktivitetData_etter = aktivitetjeger.getValue();

        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(aktivitetData_etter.getEndretAv()).isEqualTo(NAVIDENT);
            assertions.assertThat(aktivitetData_etter.getLagtInnAv()).isSameAs(InnsenderData.NAV);
            assertions.assertThat(aktivitetData_etter.getStatus()).as("Skal sette aktivitetstatus").isSameAs(AktivitetStatus.FULLFORT);
            assertions.assertThat(aktivitetData_etter.getStillingFraNavData()).isNotNull();
            assertions.assertThat(aktivitetData_etter.getStillingFraNavData().getSoknadsstatus()).as("Skal sette søknadstatus").isSameAs(Soknadsstatus.IKKE_FATT_JOBBEN);
            assertions.assertAll();
        });
    }

    @Test
    public void hvis_meldingen_mangler_navident_blir_navIdent_satt_til_SYSTEM() {
        rekrutteringsbistandStatusoppdateringService.behandleCvDelt(BESTILLINGSID, null, STILLING_FRA_NAV_HAR_SVART_JA);

        Mockito.verify(aktivitetDAO).oppdaterAktivitet(aktivitetjeger.capture());
        AktivitetData aktivitetData_etter = aktivitetjeger.getValue();

        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(aktivitetData_etter.getEndretAv()).isEqualTo("SYSTEM");
            assertions.assertThat(aktivitetData_etter.getLagtInnAv()).isSameAs(InnsenderData.NAV);
            assertions.assertThat(aktivitetData_etter.getStillingFraNavData()).isNotNull();
            assertions.assertThat(aktivitetData_etter.getStillingFraNavData().getSoknadsstatus()).isSameAs(Soknadsstatus.CV_DELT);
            assertions.assertAll();
        });
    }

    private final String NAVIDENT = "P314159";
    private final String BESTILLINGSID = "1337";
    private final AktivitetData STILLING_FRA_NAV_HAR_SVART_JA = AktivitetData.builder()
            .id(Long.parseLong(BESTILLINGSID))
            .versjon(1L)
            .aktivitetType(AktivitetTypeData.STILLING_FRA_NAV)
            .endretAv(NAVIDENT)
            .aktorId("12345678901")
            .lagtInnAv(InnsenderData.NAV)
            .status(AktivitetStatus.GJENNOMFORES)
            .stillingFraNavData(
                    StillingFraNavData.builder()
                            .soknadsstatus(Soknadsstatus.VENTER)
                            .livslopsStatus(LivslopsStatus.HAR_SVART)
                            .cvKanDelesData(CvKanDelesData.builder()
                                    .kanDeles(Boolean.TRUE)
                                    .build())
                            .build()
            )
            .build();

}