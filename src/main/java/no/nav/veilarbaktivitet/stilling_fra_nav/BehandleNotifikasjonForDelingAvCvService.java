package no.nav.veilarbaktivitet.stilling_fra_nav;

import lombok.RequiredArgsConstructor;
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO;
import no.nav.veilarbaktivitet.aktivitet.AktivitetService;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.brukernotifikasjon.Brukernotifikasjon;
import no.nav.veilarbaktivitet.brukernotifikasjon.kvitering.KvitteringDAO;
import no.nav.veilarbaktivitet.person.Person;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BehandleNotifikasjonForDelingAvCvService {

    private final StillingFraNavProducerClient stillingFraNavProducerClient;
    private final AktivitetDAO aktivitetDAO;
    private final AktivitetService aktivitetService;
    private final KvitteringDAO kvitteringDAO;

    @Transactional
    public void behandleFerdigstiltKvittering(Brukernotifikasjon brukernotifikasjon) {
        AktivitetData aktivitetData = aktivitetDAO.hentAktivitet(brukernotifikasjon.getAktivitetId());

        // Hvis aktiviteten er svart trenger vi ikke å varsle
        if (aktivitetData.getStillingFraNavData().cvKanDelesData != null) {
            kvitteringDAO.setFerdigBehandlet(brukernotifikasjon.getId());
            return;
        }

        AktivitetData nyAktivitet = aktivitetData.toBuilder().stillingFraNavData(aktivitetData.getStillingFraNavData().withLivslopsStatus(LivslopsStatus.HAR_VARSLET)).build();
        aktivitetService.oppdaterAktivitet(aktivitetData, nyAktivitet, Person.navIdent("SYSTEM"));
        kvitteringDAO.setFerdigBehandlet(brukernotifikasjon.getId());

        stillingFraNavProducerClient.sendVarslet(aktivitetData);
    }

    public void behandleFeiletKvittering(Brukernotifikasjon brukernotifikasjon) {
        AktivitetData aktivitetData = aktivitetDAO.hentAktivitet(brukernotifikasjon.getAktivitetId());

        // Hvis aktiviteten er svart trenger vi ikke å varsle men vi setter nofigikasjonen til ferdig behandlet
        if (aktivitetData.getStillingFraNavData().cvKanDelesData != null) {
            kvitteringDAO.setFerdigBehandlet(brukernotifikasjon.getId());
            return;
        }

        AktivitetData nyAktivitet = aktivitetData.toBuilder().stillingFraNavData(aktivitetData.getStillingFraNavData().withLivslopsStatus(LivslopsStatus.KAN_IKKE_VARSLE)).build();
        aktivitetService.oppdaterAktivitet(aktivitetData, nyAktivitet, Person.navIdent("SYSTEM"));
        kvitteringDAO.setFerdigBehandlet(brukernotifikasjon.getId());

        stillingFraNavProducerClient.sendKanIkkeVarsle(aktivitetData);

    }
}
