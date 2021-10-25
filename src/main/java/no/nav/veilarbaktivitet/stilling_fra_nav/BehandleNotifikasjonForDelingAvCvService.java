package no.nav.veilarbaktivitet.stilling_fra_nav;

import lombok.RequiredArgsConstructor;
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO;
import no.nav.veilarbaktivitet.aktivitet.AktivitetService;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.brukernotifikasjon.Brukernotifikasjon;
import no.nav.veilarbaktivitet.person.Person;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BehandleNotifikasjonForDelingAvCvService {

    private final StillingFraNavProducerClient stillingFraNavProducerClient;
    private final AktivitetDAO aktivitetDAO;
    private final AktivitetService aktivitetService;

    @Transactional
    public void behandleFerdigstiltKvittering(Brukernotifikasjon brukernotifikasjon) {
        AktivitetData aktivitetData = aktivitetDAO.hentAktivitet(brukernotifikasjon.getAktivitetId());

        AktivitetData nyAktivitet = aktivitetData.toBuilder().stillingFraNavData(aktivitetData.getStillingFraNavData().withLivslopsStatus(LivslopsStatus.HAR_VARSLET)).build();
        aktivitetService.oppdaterAktivitet(aktivitetData, nyAktivitet, Person.navIdent("SYSTEM"));
        stillingFraNavProducerClient.sendVarslet(aktivitetData);

    }
}
