package no.nav.veilarbaktivitet.mock_nav_modell;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import no.nav.common.auth.context.UserRole;
import no.nav.poao_tilgang.poao_tilgang_test_core.PrivatBruker;
import no.nav.veilarbaktivitet.oppfolging.periode.Oppfolgingsperiode;
import no.nav.veilarbaktivitet.person.Person;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Getter
public class MockBruker extends RestassuredUser {
    protected final PrivatBruker privatbruker;
    @Setter
    public UUID oppfolgingsperiode;
    @Setter(AccessLevel.PACKAGE)
    private BrukerOptions brukerOptions;

    private List<Oppfolgingsperiode> oppfolgingsperioder;

    MockBruker(BrukerOptions brukerOptions, PrivatBruker privatBruker) {
        super(privatBruker.getNorskIdent(), UserRole.EKSTERN);
        this.brukerOptions = brukerOptions;
        if (brukerOptions.isUnderOppfolging()) {
            oppfolgingsperiode = UUID.randomUUID();
            var oppfolgingsperiodeObject = new Oppfolgingsperiode(
                    getAktorId().get(),
                    oppfolgingsperiode,
                    // Aktiviteter fra arena-endepunkt er hardkodet til å ha start-dato 2021-11-18
                    // For at disse skal komme med må oppfolgingsperiode start før
                    ZonedDateTime.now().withYear(2021).withMonth(10),
                    null
            );
            oppfolgingsperioder = List.of(oppfolgingsperiodeObject);

        }
        this.privatbruker = privatBruker;
    }

    public String getFnr() {
        return super.ident;
    }

    public Person.AktorId getAktorId() {
        return Person.aktorId(new StringBuilder(getFnr()).reverse().toString());
    }


    public Person.Fnr getFnrAsFnr() {
        return Person.fnr(super.ident);
    }


    public Person.AktorId getAktorIdAsAktorId() {
        return getAktorId();
    }

    public String getOppfolgingsenhet() {
        return privatbruker.getOppfolgingsenhet();
    }
}
