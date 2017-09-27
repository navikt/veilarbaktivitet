package no.nav.fo.veilarbaktivitet.service;

import lombok.val;
import no.nav.apiapp.security.PepClient;
import no.nav.fo.veilarbaktivitet.domain.AktivitetData;
import no.nav.fo.veilarbaktivitet.domain.AktivitetTypeData;
import no.nav.fo.veilarbaktivitet.ws.consumer.ArenaAktivitetConsumer;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static no.nav.fo.veilarbaktivitet.domain.AktivitetTypeData.*;

@Component
public class AktivitetWSAppService extends AktivitetAppService {

    private static final Set<AktivitetTypeData> TYPER_SOM_KAN_ENDRES = new HashSet<>(Arrays.asList(
            EGENAKTIVITET,
            JOBBSOEKING,
            SOKEAVTALE,
            IJOBB,
            BEHANDLING
    ));

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
    public AktivitetData oppdaterStatus(AktivitetData aktivitet) {
        val originalAktivitet = hentAktivitet(aktivitet.getId()); // innebærer tilgangskontroll
        if(TYPER_SOM_KAN_ENDRES.contains(originalAktivitet.getAktivitetType())){
            return internalOppdaterStatus(aktivitet);
        }
        return aktivitet;
    }

    @Override
    public AktivitetData oppdaterAktivitet(AktivitetData aktivitet) {
        val originalAktivitet = hentAktivitet(aktivitet.getId()); // innebærer tilgangskontroll
        if (originalAktivitet.isAvtalt() || !TYPER_SOM_KAN_ENDRES.contains(originalAktivitet.getAktivitetType())) {
            return originalAktivitet;
        }

        return brukerService.getLoggedInnUser()
                .map(userIdent -> {
                    aktivitetService.oppdaterAktivitet(originalAktivitet, aktivitet, userIdent);
                    return hentAktivitet(aktivitet.getId());
                }).orElseThrow(RuntimeException::new);
    }

}
