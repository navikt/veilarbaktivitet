package no.nav.veilarbaktivitet.mock_nav_modell;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import no.nav.common.auth.context.UserRole;
import no.nav.poao_tilgang.poao_tilgang_test_core.PrivatBruker;
import no.nav.veilarbaktivitet.oppfolging.periode.Oppfolgingsperiode;
import no.nav.veilarbaktivitet.person.Navn;
import no.nav.veilarbaktivitet.person.Person;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Getter
public class MockBruker extends RestassuredUser {
    protected final PrivatBruker privatbruker;
    public UUID oppfolgingsperiodeId;
    @Setter(AccessLevel.PACKAGE)
    private BrukerOptions brukerOptions;

    private List<Oppfolgingsperiode> oppfolgingsperioder = new ArrayList<>();

    MockBruker(BrukerOptions brukerOptions, PrivatBruker privatBruker) {
        super(privatBruker.getNorskIdent(), UserRole.EKSTERN);
        this.brukerOptions = brukerOptions;
        if (brukerOptions.isUnderOppfolging()) {
            oppfolgingsperiodeId = UUID.randomUUID();
            var oppfolgingsperiodeObject = new Oppfolgingsperiode(
                    getAktorId().get(),
                    oppfolgingsperiodeId,
                    // Aktiviteter fra arena-endepunkt er hardkodet til å ha start-dato 2021-11-18
                    // For at disse skal komme med må oppfolgingsperiode start før
                    ZonedDateTime.now().withYear(2021).withMonth(10),
                    null
            );
            oppfolgingsperioder.add(oppfolgingsperiodeObject);
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

    public Navn getNavn() {
        return brukerOptions.getNavn();
    }

    public Person.AktorId getAktorIdAsAktorId() {
        return getAktorId();
    }

    public void setOppfolgingsperiodeId(UUID oppfolgingsperiodeId) {
        this.oppfolgingsperiodeId = oppfolgingsperiodeId;

        var sisteEksisterendeOppfølgingsperiode = getNyesteOppfølgingsperiode();
        var startDatoNestePeriode = sisteEksisterendeOppfølgingsperiode != null ? sisteEksisterendeOppfølgingsperiode.startTid().plusDays(1) : ZonedDateTime.now().minusDays(1);

        var nyOppfølgingsperiode = new Oppfolgingsperiode(
                getAktorId().get(),
                oppfolgingsperiodeId,
                startDatoNestePeriode,
                null
        );

        oppfolgingsperioder.add(nyOppfølgingsperiode);
    }

    public String getOppfolgingsenhet() {
        return privatbruker.getOppfolgingsenhet();
    }

    public Oppfolgingsperiode getNyesteOppfølgingsperiode() {
        if (oppfolgingsperioder == null || oppfolgingsperioder.isEmpty()) {
            return null;
        } else {
            return Collections.max(oppfolgingsperioder, ((o1, o2) ->
                    Math.toIntExact(o1.startTid().toEpochSecond() - o2.startTid().toEpochSecond())
            ));
        }
    }
}
