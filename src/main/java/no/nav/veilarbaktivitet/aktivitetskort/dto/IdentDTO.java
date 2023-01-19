package no.nav.veilarbaktivitet.aktivitetskort.dto;

import lombok.Builder;
import lombok.With;
import no.nav.veilarbaktivitet.person.Person;
import org.opensaml.xmlsec.signature.P;

@Builder
@With
public record IdentDTO (
    String ident,
    IdentType identType)
{
    public Person toPerson() {
        return switch (this.identType()) {
            case NAVIDENT -> Person.navIdent(this.ident());
            case PERSONBRUKERIDENT -> Person.fnr(this.ident());
            case ARENAIDENT -> Person.arenaIdent(this.ident());
            case ARBEIDSGIVER -> Person.arbeidsgiver(this.ident());
            case TILTAKSARRANGOER -> Person.tiltaksarragoer(this.ident());
        };
    }

}
