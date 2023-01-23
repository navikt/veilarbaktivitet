package no.nav.veilarbaktivitet.service;

import com.nimbusds.jwt.JWTClaimsSet;
import no.nav.common.abac.Pep;
import no.nav.common.auth.Constants;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.UserRole;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.NavIdent;
import no.nav.veilarbaktivitet.person.AuthService;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.person.PersonService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static no.nav.common.auth.Constants.ID_PORTEN_PID_CLAIM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private final AuthContextHolder authContextHolder = mock(AuthContextHolder.class);
    private final AktorOppslagClient aktorOppslagClient = mock(AktorOppslagClient.class);
    private final Pep veilarbPep = mock(Pep.class);

    private final PersonService personService = new PersonService(aktorOppslagClient);

    @InjectMocks
    private AuthService authService = new AuthService(authContextHolder, veilarbPep, personService);


    private static final String NAVIDENT = "Z999999";
    private static final String FNR = "10101055555";
    private static final String AKTORID = "111111666666";

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
        when(authContextHolder.getUid()).thenReturn(Optional.of(FNR));
        when(aktorOppslagClient.hentAktorId(any())).thenReturn(AktorId.of(AKTORID));
        Optional<Person> loggedInnUser = authService.getLoggedInnUser();
        assertEquals(eksternBruker, loggedInnUser.get());
    }

    @Test
    void erSystemBruker() {
        when(authContextHolder.erSystemBruker()).thenReturn(Boolean.TRUE);
        boolean erSystemBruker = authService.erSystemBruker();
        assertEquals(Boolean.TRUE, erSystemBruker);
    }


    @Test
    void skal_tillate_ekstern_bruker_med_riktig_fnr() {
        Person eksternBruker = Person.fnr(FNR);
        when(authContextHolder.getUid()).thenReturn(Optional.of(FNR));
        when(authContextHolder.getIdTokenClaims()).thenReturn(
                Optional.ofNullable(new JWTClaimsSet.Builder()
                        .claim(ID_PORTEN_PID_CLAIM, FNR)
                        .claim("acr", "Level4")
                        .build())
        );
        authService.sjekkEksternBrukerHarTilgang(eksternBruker);
    }

    @Test
    void skal_blokkere_ekstern_bruker_med_feil_fnr() {
        when(authContextHolder.getUid()).thenReturn(Optional.of(FNR));
        when(authContextHolder.getIdTokenClaims()).thenReturn(
                Optional.ofNullable(new JWTClaimsSet.Builder()
                        .claim(ID_PORTEN_PID_CLAIM, FNR)
                        .claim("acr", "Level4")
                        .build())
        );
        Assertions.assertThrows(ResponseStatusException.class, () -> {
            authService.sjekkEksternBrukerHarTilgang(Person.fnr("12121212121"));
        });
    }

}