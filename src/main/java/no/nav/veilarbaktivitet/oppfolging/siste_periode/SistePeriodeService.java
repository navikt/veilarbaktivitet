package no.nav.veilarbaktivitet.oppfolging.siste_periode;

import lombok.RequiredArgsConstructor;
import no.nav.veilarbaktivitet.oppfolging.client.OppfolgingV2Client;
import no.nav.veilarbaktivitet.person.Person;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class SistePeriodeService {
    private final OppfolgingV2Client oppfolgingV2Client;
    private final SistePeriodeDAO sistePeriodeDAO;

    public UUID hentGjeldendeOppfolgingsperiodeMedFallback(Person.AktorId aktorId) {

        Supplier<IngenGjeldendePeriodeException> exceptionSupplier = () -> new IngenGjeldendePeriodeException(String.format("AktorId: %s har ingen gjeldende oppfølgingsperiode", aktorId.get()));

        Oppfolgingsperiode oppfolgingsperiode = sistePeriodeDAO.hentSisteOppfolgingsPeriode(aktorId.get())
                // Mangler aktiv oppfølgingsperiode
                .filter(periode -> periode.sluttTid()  == null)
                .or(() -> oppfolgingV2Client.fetchGjeldendePeriode(aktorId)
                        .map(
                                dto -> new Oppfolgingsperiode(aktorId.get(), dto.getUuid(), dto.getStartDato(), dto.getSluttDato())
                        )
                ).orElseThrow(exceptionSupplier);

        return oppfolgingsperiode.oppfolgingsperiode();
    }

}
