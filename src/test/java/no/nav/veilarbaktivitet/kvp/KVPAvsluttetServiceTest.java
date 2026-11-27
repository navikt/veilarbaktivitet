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
import java.util.Optional;

import static org.assertj.core.util.DateUtil.now;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KVPAvsluttetServiceTest {
    private final Person.AktorId aktorId = new Person.AktorId("1231231231230");

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
    void skal_lagre_stoppmelding_i_utboks_nar_kvp_avsluttes(AktivitetTypeData aktivitetType) {
        val aktivitet = nyKvpAktivitet(aktivitetType);
        when(aktivitetDAO.hentAktiviteterForAktorId(aktorId)).thenReturn(List.of(aktivitet));

        kvpService.håndterKvpAvsluttet(aktorId, Optional.of("ferdig kvp"), Optional.of(now()));

        verify(oversiktenService).lagreStoppMeldingOmUdeltSamtalereferatIUtboks(aktorId, aktivitet.getId());
    }

    @ParameterizedTest
    @EnumSource(value = AktivitetTypeData.class, names = {"MOTE", "SAMTALEREFERAT"})
    void skal_oppdatere_siste_aktivitet_en_gang_etter_alle_avbrytelser_nar_kvp_avsluttes(AktivitetTypeData aktivitetType) {
        val aktivitet = nyKvpAktivitet(aktivitetType);
        val annenAktivitet = aktivitet.withId(aktivitet.getId() + 1);
        when(aktivitetDAO.hentAktiviteterForAktorId(aktorId)).thenReturn(List.of(aktivitet, annenAktivitet));

        kvpService.håndterKvpAvsluttet(aktorId, Optional.of("ferdig kvp"), Optional.of(now()));

        val rekkefolge = inOrder(aktivitetDAO, oversiktenService);
        rekkefolge.verify(aktivitetDAO, times(2)).oppdaterAktivitet(any());
        rekkefolge.verify(oversiktenService).lagreOppdaterSisteMøteMelding(aktorId);
    }

    @Test
    void skal_ikke_oppdatere_siste_aktivitet_for_annen_aktivitetstype_nar_kvp_avsluttes() {
        val aktivitet = nyKvpAktivitet(AktivitetTypeData.EGENAKTIVITET);
        when(aktivitetDAO.hentAktiviteterForAktorId(aktorId)).thenReturn(List.of(aktivitet));

        kvpService.håndterKvpAvsluttet(aktorId, Optional.of("ferdig kvp"), Optional.of(now()));

        verify(aktivitetDAO).oppdaterAktivitet(any());
        verify(oversiktenService, never()).lagreOppdaterSisteMøteMelding(aktorId);
    }

    private AktivitetData nyKvpAktivitet(AktivitetTypeData aktivitetType) {
        return AktivitetData.builder()
                .id(123456789L)
                .aktorId(aktorId)
                .opprettetDato(Date.from(ZonedDateTime.now().minusDays(7).toInstant()))
                .aktivitetType(aktivitetType)
                .status(AktivitetStatus.PLANLAGT)
                .kontorsperreEnhetId("9001")
                .build();
    }
}
