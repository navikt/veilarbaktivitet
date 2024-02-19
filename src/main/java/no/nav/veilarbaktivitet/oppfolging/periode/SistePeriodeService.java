package no.nav.veilarbaktivitet.oppfolging.periode;

import io.micrometer.core.annotation.Timed;
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

    @Timed
    public UUID hentGjeldendeOppfolgingsperiodeMedFallback(Person.AktorId aktorId) {

        Supplier<IngenGjeldendePeriodeException> exceptionSupplier = () -> new IngenGjeldendePeriodeException(String.format("AktorId: %s har ingen gjeldende oppfølgingsperiode", aktorId.get()));

        Oppfolgingsperiode oppfolgingsperiode = sistePeriodeDAO.hentSisteOppfolgingsPeriode(aktorId)
                // Mangler aktiv oppfølgingsperiode
                .filter(periode -> periode.sluttTid()  == null)
                .or(() -> oppfolgingV2Client.fetchGjeldendePeriode(aktorId)
                        .map(
                                dto -> new Oppfolgingsperiode(aktorId.get(), dto.uuid(), dto.startDato(), dto.sluttDato())
                        )
                ).orElseThrow(exceptionSupplier);

        return oppfolgingsperiode.oppfolgingsperiodeId();
    }

}
