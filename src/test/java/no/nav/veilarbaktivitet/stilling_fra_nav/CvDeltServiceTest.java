package no.nav.veilarbaktivitet.stilling_fra_nav;

import no.nav.veilarbaktivitet.SpringBootTestBase;
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData;
import no.nav.veilarbaktivitet.person.InnsenderData;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.SpyBean;

import static org.mockito.ArgumentMatchers.any;

public class CvDeltServiceTest extends SpringBootTestBase {

    @SpyBean
    DelingAvCvDAO delingAvCvDAO;

    @SpyBean
    CvDeltService cvDeltService;

    @SpyBean
    AktivitetDAO aktivitetDAO;
    private final ArgumentCaptor<AktivitetData> aktivitetjeger = ArgumentCaptor.forClass(AktivitetData.class);

    @Before
    public void setUp() {
        Mockito.doReturn(null).when(aktivitetDAO).oppdaterAktivitet(any());
    }

    @Test
    public void happy_case_svart_ja() {
        cvDeltService.behandleRekrutteringsbistandoppdatering(BESTILLINGSID, RekrutteringsbistandStatusoppdateringEventType.CV_DELT, NAVIDENT, STILLING_FRA_NAV_HAR_SVART_JA);

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
    public void happy_men_dessverre_case_ikke_fatt_jobben() {
        cvDeltService.behandleRekrutteringsbistandoppdatering(BESTILLINGSID, RekrutteringsbistandStatusoppdateringEventType.IKKE_FATT_JOBBEN, NAVIDENT, STILLING_FRA_NAV_HAR_SVART_JA);

        Mockito.verify(aktivitetDAO).oppdaterAktivitet(aktivitetjeger.capture());
        AktivitetData aktivitetData_etter = aktivitetjeger.getValue();

        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(aktivitetData_etter.getEndretAv()).isEqualTo(NAVIDENT);
            assertions.assertThat(aktivitetData_etter.getLagtInnAv()).isSameAs(InnsenderData.NAV);
            assertions.assertThat(aktivitetData_etter.getStatus()).isSameAs(AktivitetStatus.FULLFORT);
            assertions.assertThat(aktivitetData_etter.getStillingFraNavData()).isNotNull();
            assertions.assertThat(aktivitetData_etter.getStillingFraNavData().getSoknadsstatus()).isSameAs(Soknadsstatus.AVSLAG);
            assertions.assertThat(aktivitetData_etter.getStillingFraNavData().getLivslopsStatus()).isSameAs(AktivitetStatus.FULLFORT);
            assertions.assertAll();
        });
    }

    @Test
    public void hvis_meldingen_mangler_navident_blir_navIdent_satt_til_SYSTEM() {
        cvDeltService.behandleRekrutteringsbistandoppdatering(BESTILLINGSID, RekrutteringsbistandStatusoppdateringEventType.CV_DELT, null, STILLING_FRA_NAV_HAR_SVART_JA);

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

    private final AktivitetData STILLING_FRA_NAV_HAR_SVART_NEI = AktivitetData.builder()
            .id(Long.parseLong(BESTILLINGSID))
            .versjon(1L)
            .aktivitetType(AktivitetTypeData.STILLING_FRA_NAV)
            .lagtInnAv(InnsenderData.NAV)
            .status(AktivitetStatus.GJENNOMFORES)
            .stillingFraNavData(
                    StillingFraNavData.builder()
                            .soknadsstatus(Soknadsstatus.VENTER)
                            .livslopsStatus(LivslopsStatus.HAR_SVART)
                            .cvKanDelesData(CvKanDelesData.builder()
                                    .kanDeles(Boolean.FALSE)
                                    .build())
                            .build()
            )
            .build();

}