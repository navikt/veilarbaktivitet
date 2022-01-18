package no.nav.veilarbaktivitet.oppfolging.siste_periode;

import lombok.RequiredArgsConstructor;
import no.nav.veilarbaktivitet.oppfolging.client.OppfolgingV2Client;
import no.nav.veilarbaktivitet.person.Person;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OppfolgingsperiodeService {
    private final OppfolgingV2Client oppfolgingV2Client;

    public UUID fallbackKallOppfolging(Person.AktorId aktorId) {
        // TODO
        return null;
    };

}
