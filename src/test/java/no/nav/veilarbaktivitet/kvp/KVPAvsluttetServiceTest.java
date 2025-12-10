package no.nav.veilarbaktivitet.kvp;

import lombok.val;
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData;
import no.nav.veilarbaktivitet.oversikten.OversiktenService;
import no.nav.veilarbaktivitet.person.Person;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

import static org.assertj.core.util.DateUtil.now;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KVPAvsluttetServiceTest {
    @Mock
    private AktivitetDAO aktivitetDAO;

    @Mock
    private OversiktenService oversiktenService;

    private KVPAvsluttetService kvpService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        kvpService = new KVPAvsluttetService(aktivitetDAO, oversiktenService);
    }


    @ParameterizedTest
    @EnumSource(value = AktivitetTypeData.class, names = {"MOTE", "SAMTALEREFERAT"})
    void skal_lagre_stopp_melding_for_avbrutt_mote_og_samtalereferat(AktivitetTypeData aktivitetType) {
        val aktorId = new Person.AktorId("1231231231230");
        val aktivitetId = 123456789L;
        AktivitetData aktivitet = AktivitetData.builder()
                .id(aktivitetId)
                .aktorId(aktorId)
                .opprettetDato(Date.from(ZonedDateTime.now().minusDays(7).toInstant()))
                .aktivitetType(aktivitetType)
                .status(AktivitetStatus.PLANLAGT)
                .kontorsperreEnhetId("9001")
                .build();

        when(aktivitetDAO.hentAktiviteterForAktorId(aktorId)).thenReturn(List.of(aktivitet));

        kvpService.settAktiviteterInomKVPPeriodeTilAvbrutt(aktorId, "ferdig kvp", now());

        verify(oversiktenService).lagreStoppMeldingOmUdeltSamtalereferatIUtboks(aktorId, aktivitetId);
        verify(aktivitetDAO).oppdaterAktivitet(any());
    }
}
