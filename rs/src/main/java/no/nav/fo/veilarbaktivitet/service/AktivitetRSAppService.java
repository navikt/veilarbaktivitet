package no.nav.fo.veilarbaktivitet.service;

import lombok.val;
import no.nav.fo.veilarbaktivitet.domain.AktivitetData;
import no.nav.fo.veilarbaktivitet.ws.consumer.AktoerConsumer;
import no.nav.fo.veilarbaktivitet.ws.consumer.ArenaAktivitetConsumer;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class AktivitetRSAppService extends AktivitetAppService {

    @Inject
    AktivitetRSAppService(AktoerConsumer aktoerConsumer,
                          ArenaAktivitetConsumer arenaAktivitetConsumer,
                          AktivitetService aktivitetService) {
        super(aktoerConsumer, arenaAktivitetConsumer, aktivitetService);
    }

    @Override
    public AktivitetData oppdaterAktivitet(AktivitetData aktivitet) {
        val orignalAktivitet = hentAktivitet(aktivitet.getId());

        if (orignalAktivitet.isAvtalt()) {
            aktivitetService.oppdaterAktivitetFrist(orignalAktivitet, aktivitet);
        } else {
            aktivitetService.oppdaterAktivitet(orignalAktivitet, aktivitet);
        }

        return hentAktivitet(aktivitet.getId());
    }
}
