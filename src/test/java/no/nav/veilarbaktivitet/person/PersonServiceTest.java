package no.nav.veilarbaktivitet.person;

import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.client.aktorregister.IngenGjeldendeIdentException;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class PersonServiceTest {

    private final String fnrString = "12345678901";
    private final String aktorIdString = "10987654321";

    private final Fnr COMMONS_FNR = Fnr.ofValidFnr(fnrString);
    private final Person.Fnr LOCAL_FNR = Person.fnr(fnrString);

    private final AktorId COMMONS_AKTORID = AktorId.of(aktorIdString);
    private final Person.AktorId LOCAL_AKTORID = Person.aktorId(aktorIdString);

    @Mock
    AktorOppslagClient aktorOppslagClient;
    @InjectMocks
    PersonService personService;

    @Test
    void testGetAktorIdForPersonBrukerCommon() {
        Mockito.when(aktorOppslagClient.hentAktorId(COMMONS_FNR)).thenReturn(COMMONS_AKTORID);
        AktorId aktorIdForPersonBruker = personService.getAktorIdForPersonBruker(COMMONS_FNR);
        assertThat(aktorIdForPersonBruker).isEqualTo(COMMONS_AKTORID);

        AktorId aktorIdForPersonBrukerUsingAktorId = personService.getAktorIdForPersonBruker(COMMONS_AKTORID);
        assertThat(aktorIdForPersonBrukerUsingAktorId).isEqualTo(COMMONS_AKTORID);
    }

    @Test
    void testGetAktorIdForPersonBrukerCommonThrowsOnFailure() {
        Mockito.when(aktorOppslagClient.hentAktorId(COMMONS_FNR)).thenThrow(IngenGjeldendeIdentException.class);
        assertThrows(IngenGjeldendeIdentException.class, () -> personService.getAktorIdForPersonBruker(COMMONS_FNR));
    }

    @Test
    void testGetFnrForAktorIdCommon() {
        Mockito.when(aktorOppslagClient.hentFnr(COMMONS_AKTORID)).thenReturn(COMMONS_FNR);
        Fnr fnrForAktorId = personService.getFnrForAktorId(COMMONS_AKTORID);
        assertThat(fnrForAktorId).isEqualTo(COMMONS_FNR);

        Fnr fnrForAktorIdUsingFnr = personService.getFnrForAktorId(COMMONS_FNR);
        assertThat(fnrForAktorIdUsingFnr).isEqualTo(COMMONS_FNR);
    }

    @Test
    void testGetFnrForAktorIdCommonThrowsOnFailure() {
        Mockito.when(aktorOppslagClient.hentFnr(COMMONS_AKTORID)).thenThrow(IngenGjeldendeIdentException.class);
        assertThrows(IngenGjeldendeIdentException.class, () -> personService.getFnrForAktorId(COMMONS_AKTORID));
    }

    @Test
    void testGetFnrForAktorIdLocal() {
        Mockito.when(aktorOppslagClient.hentFnr(COMMONS_AKTORID)).thenReturn(COMMONS_FNR);
        Person.Fnr fnrForAktorId = personService.getFnrForAktorId(LOCAL_AKTORID);
        assertThat(fnrForAktorId).isEqualTo(LOCAL_FNR);
    }

    @Test
    void testGetFnrForAktorIdLocalThrowsOnFailure() {
        Mockito.when(aktorOppslagClient.hentFnr(COMMONS_AKTORID)).thenThrow(IngenGjeldendeIdentException.class);
        assertThrows(IngenGjeldendeIdentException.class, () -> personService.getFnrForAktorId(LOCAL_AKTORID));
    }

    @Test
    void getFnrForPersonbrukerLocal() {
        Mockito.when(aktorOppslagClient.hentFnr(COMMONS_AKTORID)).thenReturn(COMMONS_FNR);
        Optional<Person.Fnr> fnrForPersonbruker = personService.getFnrForPersonbruker(LOCAL_AKTORID);
        assertThat(fnrForPersonbruker).hasValueSatisfying(value -> assertThat(value).isEqualTo(LOCAL_FNR));

        Optional<Person.Fnr> fnrForPersonbrukerUsingFnr = personService.getFnrForPersonbruker(LOCAL_FNR);
        assertThat(fnrForPersonbrukerUsingFnr).hasValueSatisfying(value -> assertThat(value).isEqualTo(LOCAL_FNR));
    }

    @Test
    void getFnrForPersonbrukerLocalUsingNonExternUser() {
        Optional<Person.Fnr> fnrForPersonbrukerUsingNavIdent = personService.getFnrForPersonbruker(Person.navIdent("Z999999"));
        assertThat(fnrForPersonbrukerUsingNavIdent).isEmpty();

        Optional<Person.Fnr> fnrForPersonbrukerUsingSystemUser = personService.getFnrForPersonbruker(Person.systemUser());
        assertThat(fnrForPersonbrukerUsingSystemUser).isEmpty();
    }

    @Test
    void getFnrForPersonbrukerLocalEmptyOnIngenGjeldendeIdent() {
        Mockito.when(aktorOppslagClient.hentFnr(COMMONS_AKTORID)).thenThrow(IngenGjeldendeIdentException.class);
        Optional<Person.Fnr> fnrForPersonbruker = personService.getFnrForPersonbruker(LOCAL_AKTORID);
        assertThat(fnrForPersonbruker).isEmpty();
    }

    @Test
    void testGetAktorIdForPersonBrukerLocal() {
        Mockito.when(aktorOppslagClient.hentAktorId(COMMONS_FNR)).thenReturn(COMMONS_AKTORID);
        Optional<Person.AktorId> aktorIdForPersonBruker = personService.getAktorIdForPersonBruker(LOCAL_FNR);
        assertThat(aktorIdForPersonBruker).hasValueSatisfying(aktorId -> assertThat(aktorId).isEqualTo(LOCAL_AKTORID));

        Optional<Person.AktorId> aktorIdForPersonBrukerUsingAktorId = personService.getAktorIdForPersonBruker(LOCAL_AKTORID);
        assertThat(aktorIdForPersonBrukerUsingAktorId).hasValueSatisfying(aktorId -> assertThat(aktorId).isEqualTo(LOCAL_AKTORID));
    }

    @Test
    void testGetAktorIdForPersonBrukerLocalUsingNonExternUser() {
        Optional<Person.AktorId> aktorIdForPersonBrukerUsingNavIdent = personService.getAktorIdForPersonBruker(Person.navIdent("Z999999"));
        assertThat(aktorIdForPersonBrukerUsingNavIdent).isEmpty();
        Optional<Person.AktorId> aktorIdForPersonBrukerUsingSystemUser = personService.getAktorIdForPersonBruker(Person.systemUser());
        assertThat(aktorIdForPersonBrukerUsingSystemUser).isEmpty();
    }

    @Test
    void testGetAktorIdForPersonBrukerLocalEmptyOnIngenGjeldendeIdent() {
        Mockito.when(aktorOppslagClient.hentAktorId(COMMONS_FNR)).thenThrow(IngenGjeldendeIdentException.class);
        Optional<Person.AktorId> aktorIdForPersonBruker = personService.getAktorIdForPersonBruker(LOCAL_FNR);
        assertThat(aktorIdForPersonBruker).isEmpty();
    }

}