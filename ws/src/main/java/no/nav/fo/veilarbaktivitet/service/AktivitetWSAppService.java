package no.nav.fo.veilarbaktivitet.service;

import lombok.val;
import no.nav.apiapp.security.PepClient;
import no.nav.fo.veilarbaktivitet.domain.AktivitetData;
import no.nav.fo.veilarbaktivitet.ws.consumer.ArenaAktivitetConsumer;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class AktivitetWSAppService extends AktivitetAppService {

    @Inject
    public AktivitetWSAppService(
            ArenaAktivitetConsumer arenaAktivitetConsumer,
            AktivitetService aktivitetService,
            BrukerService brukerService,
            PepClient pepClient
    ) {
        super(arenaAktivitetConsumer, aktivitetService, brukerService, pepClient);
    }

    public AktivitetData opprettNyAktivtet(String ident, AktivitetData aktivitetData) {
        sjekkTilgangTilFnr(ident);
        return brukerService.getAktorIdForFNR(ident)
                .map(aktorId -> aktivitetService.opprettAktivitet(aktorId, aktivitetData, aktorId))
                .map(this::hentAktivitet)
                .orElseThrow(RuntimeException::new);
    }

    @Override
    public AktivitetData oppdaterAktivitet(AktivitetData aktivitet) {
        val originalAktivitet = hentAktivitet(aktivitet.getId()); // innebærer tilgangskontroll
        if (originalAktivitet.isAvtalt()) {
            return originalAktivitet;
        }

        return brukerService.getLoggedInnUser()
                .map(userIdent -> {
                    aktivitetService.oppdaterAktivitet(originalAktivitet, aktivitet, userIdent);
                    return hentAktivitet(aktivitet.getId());
                }).orElseThrow(RuntimeException::new);
    }

}
