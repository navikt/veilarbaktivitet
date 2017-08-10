package no.nav.fo.veilarbaktivitet.service;

import lombok.val;
import no.nav.apiapp.feil.IngenTilgang;
import no.nav.fo.veilarbaktivitet.domain.AktivitetData;
import no.nav.fo.veilarbaktivitet.domain.AktivitetTransaksjonsType;
import no.nav.fo.veilarbaktivitet.domain.AktivitetTypeData;
import no.nav.fo.veilarbaktivitet.ws.consumer.ArenaAktivitetConsumer;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import static no.nav.fo.veilarbaktivitet.domain.AktivitetTypeData.MOTE;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetTypeData.SAMTALEREFERAT;

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
        val original = hentAktivitet(aktivitet.getId());

        return brukerService.getLoggedInnUser()
                .map(userIdent -> {
                    if (original.isAvtalt()) {
                        if (original.getAktivitetType() == MOTE) {
                            aktivitetService.oppdaterMoteTidOgSted(original, aktivitet, userIdent);
                        } else {
                            aktivitetService.oppdaterAktivitetFrist(original, aktivitet, userIdent);
                        }
                    } else {
                        aktivitetService.oppdaterAktivitet(original, aktivitet, userIdent);
                    }

                    return hentAktivitet(aktivitet.getId());
                })
                .orElseThrow(RuntimeException::new);
    }


    public AktivitetData oppdaterReferat(AktivitetData aktivitet, AktivitetTransaksjonsType aktivitetTransaksjonsType) {
        aktivitetService.oppdaterReferat(
                aktivitet,
                aktivitetTransaksjonsType, brukerService.getLoggedInnUser().orElseThrow(IngenTilgang::new)
        );
        return hentAktivitet(aktivitet.getId());
    }

}
