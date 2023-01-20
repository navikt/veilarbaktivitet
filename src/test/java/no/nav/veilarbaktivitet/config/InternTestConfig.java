package no.nav.veilarbaktivitet.config;

import no.nav.veilarbaktivitet.oppfolging.siste_periode.SistePeriodeService;
import no.nav.veilarbaktivitet.person.AuthService;
import no.nav.veilarbaktivitet.person.Person;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Configuration
@Profile(value = {"intern"})
public class InternTestConfig {
    @Bean
    public AuthService authService() {
        AuthService mock = mock(AuthService.class);
        when(mock.erEksternBruker()).thenReturn(false);
        when(mock.getInnloggetBrukerIdent())
                .thenReturn(Optional.of(UUID.randomUUID().toString()));
        when(mock.getAktorIdForPersonBrukerService(any()))
                .thenReturn(Optional.of(Person.aktorId("aktor")));
        when(mock.getLoggedInnUser()).thenReturn(Optional.of(Person.aktorId("Saksbehandlersen")));
        return mock;
    }

    @Bean
    public SistePeriodeService sistePeriodeService() {
        SistePeriodeService mock = mock(SistePeriodeService.class);
        when(mock.hentGjeldendeOppfolgingsperiodeMedFallback(any()))
                .thenReturn(UUID.randomUUID());
        return mock;
    }
}
