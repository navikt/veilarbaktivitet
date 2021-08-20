package no.nav.veilarbaktivitet.service;

import no.nav.common.abac.Pep;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.UserRole;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.NavIdent;
import no.nav.veilarbaktivitet.domain.Person;
import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthContextHolder authContextHolder;
    @Mock
    private AktorOppslagClient aktorOppslagClient;
    @Mock
    private Pep veilarbPep;

    @InjectMocks
    private AuthService authService;

    private static String NAVIDENT = "Z999999";
    private static String FNR = "10101055555";
    private static String AKTORID = "111111666666";

    @Test
    void getLoggedInnNavUser() {
        NavIdent navIdent = NavIdent.of(NAVIDENT);
        Person navPerson = Person.navIdent(NAVIDENT);

        when(authContextHolder.getRole()).thenReturn(Optional.of(UserRole.INTERN));
        when(authContextHolder.getNavIdent()).thenReturn(Optional.of(navIdent));
        Optional<Person> loggedInnUser = authService.getLoggedInnUser();
        assertEquals(navPerson, loggedInnUser.get());
   }

    @Test
    void getLoggedInnEksternUser() {
        Person eksternBruker = Person.aktorId(AKTORID);

        when(authContextHolder.getRole()).thenReturn(Optional.of(UserRole.EKSTERN));
        when(authContextHolder.erEksternBruker()).thenReturn(true);
        when(authContextHolder.getSubject()).thenReturn(Optional.of(FNR));
        when(aktorOppslagClient.hentAktorId(any())).thenReturn(AktorId.of(AKTORID));
        Optional<Person> loggedInnUser = authService.getLoggedInnUser();
        assertEquals(eksternBruker, loggedInnUser.get());
    }

    @Test
    void erSystemBruker() {
        when(authContextHolder.erSystemBruker()).thenReturn(Boolean.TRUE);
        boolean erSystemBruker = authService.erSystemBruker();
        assertEquals( Boolean.TRUE, erSystemBruker);
    }

}