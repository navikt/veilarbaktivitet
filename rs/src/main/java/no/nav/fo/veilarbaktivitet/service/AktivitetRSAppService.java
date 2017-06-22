package no.nav.fo.veilarbaktivitet.service;

import lombok.val;
import no.nav.fo.veilarbaktivitet.domain.AktivitetData;
import no.nav.fo.veilarbaktivitet.ws.consumer.ArenaAktivitetConsumer;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class AktivitetRSAppService extends AktivitetAppService {

    @Inject
    AktivitetRSAppService(ArenaAktivitetConsumer arenaAktivitetConsumer,
                          AktivitetService aktivitetService,
                          BrukerService endretAv) {
        super(arenaAktivitetConsumer, aktivitetService, endretAv);
    }

    public AktivitetData opprettNyAktivtet(String ident, AktivitetData aktivitetData) {
        return brukerService.getLoggedInnUser()
                .flatMap(userIdent -> brukerService
                        .getAktorIdForFNR(ident)
                        .map(aktorId -> aktivitetService.opprettAktivitet(aktorId, aktivitetData, userIdent))
                ).map(this::hentAktivitet)
                .orElseThrow(RuntimeException::new);
    }

    @Override
    public AktivitetData oppdaterAktivitet(AktivitetData aktivitet) {
        val orginal = hentAktivitet(aktivitet.getId());

        return brukerService.getLoggedInnUser()
                .map(userIdent -> {
                    if (orginal.isAvtalt()) {
                        aktivitetService.oppdaterAktivitetFrist(orginal, aktivitet, userIdent);
                    } else {
                        aktivitetService.oppdaterAktivitet(orginal, aktivitet, userIdent);
                    }

                    return hentAktivitet(aktivitet.getId());
                })
                .orElseThrow(RuntimeException::new);
    }
}
