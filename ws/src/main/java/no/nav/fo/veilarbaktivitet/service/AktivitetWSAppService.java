package no.nav.fo.veilarbaktivitet.service;

import lombok.val;
import no.nav.fo.veilarbaktivitet.domain.AktivitetData;
import no.nav.fo.veilarbaktivitet.ws.consumer.AktoerConsumer;
import no.nav.fo.veilarbaktivitet.ws.consumer.ArenaAktivitetConsumer;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class AktivitetWSAppService extends AktivitetAppService {

    @Inject
    public AktivitetWSAppService(AktoerConsumer aktoerConsumer,
                                 ArenaAktivitetConsumer arenaAktivitetConsumer,
                                 AktivitetService aktivitetService) {
        super(aktoerConsumer, arenaAktivitetConsumer, aktivitetService);
    }

    @Override
    public AktivitetData oppdaterAktivitet(AktivitetData aktivitet) {
        val originalAktivitet = hentAktivitet(aktivitet.getId());
        if (originalAktivitet.isAvtalt()) {
            return originalAktivitet;
        }
        aktivitetService.oppdaterAktivitet(originalAktivitet, aktivitet);
        return hentAktivitet(aktivitet.getId());
    }

}
