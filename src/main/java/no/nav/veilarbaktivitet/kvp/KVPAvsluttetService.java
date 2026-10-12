package no.nav.veilarbaktivitet.kvp;

import lombok.RequiredArgsConstructor;
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTransaksjonsType;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData;
import no.nav.veilarbaktivitet.oversikten.OversiktenService;
import no.nav.veilarbaktivitet.person.Person;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class KVPAvsluttetService {
    private final AktivitetDAO aktivitetDAO;
    private final OversiktenService oversiktenService;

    public boolean finnesAktivitetMedKontorsperreEnhet(Person.AktorId aktoerId, String kontorsperreEnhetId) {
        return aktivitetDAO.hentAktiviteterForAktorId(aktoerId).stream()
                .anyMatch(aktivitet -> kontorsperreEnhetId.equals(aktivitet.getKontorsperreEnhetId()) && erAktiv(aktivitet));
    }

    public void settAktiviteterInomKVPPeriodeTilAvbrutt(Person.AktorId aktoerId, String avsluttetBegrunnelse, Date avsluttetDato) {
        aktivitetDAO.hentAktiviteterForAktorId(aktoerId).stream()
                .filter(this::filtrerKontorSperretOgStatusErIkkeAvBruttEllerFullfort)
                .filter(this::filtrerEksterneAktiviteter)
                .filter(aktivitet -> aktivitet.getOpprettetDato().before(avsluttetDato))
                .map(aktivitet -> settKVPAktivitetTilAvbrutt(aktivitet, avsluttetBegrunnelse, avsluttetDato))
                .forEach(oppdatertAktivitet -> {
                    aktivitetDAO.oppdaterAktivitet(oppdatertAktivitet);

                    if (erAvbruttMoteEllerSamtalereferat(oppdatertAktivitet)) {
                        oversiktenService.lagreStoppMeldingOmUdeltSamtalereferatIUtboks(oppdatertAktivitet.getAktorId(), oppdatertAktivitet.getId()
                        );
                    }
                });
    }


    private boolean filtrerKontorSperretOgStatusErIkkeAvBruttEllerFullfort(AktivitetData aktivitetData) {
        return aktivitetData.getKontorsperreEnhetId() != null && erAktiv(aktivitetData);
    }

    private boolean erAktiv(AktivitetData aktivitetData) {
        var aktivitetStatus = aktivitetData.getStatus();
        return aktivitetStatus != AktivitetStatus.AVBRUTT && aktivitetStatus != AktivitetStatus.FULLFORT;
    }

    private boolean filtrerEksterneAktiviteter(AktivitetData aktivitetData) {
        return AktivitetTypeData.EKSTERNAKTIVITET != aktivitetData.getAktivitetType();
    }

    private AktivitetData settKVPAktivitetTilAvbrutt(AktivitetData aktivitetData, String avsluttetBegrunnelse, Date avsluttetDato) {
        return aktivitetData
                .withTransaksjonsType(AktivitetTransaksjonsType.STATUS_ENDRET)
                .withStatus(AktivitetStatus.AVBRUTT)
                .withAvsluttetKommentar(avsluttetBegrunnelse)
                .withEndretDato(avsluttetDato);
    }

    private boolean erAvbruttMoteEllerSamtalereferat(AktivitetData aktivitet){
        return (aktivitet.getAktivitetType() == AktivitetTypeData.MOTE ||
                aktivitet.getAktivitetType() == AktivitetTypeData.SAMTALEREFERAT) &&
                aktivitet.getStatus() == AktivitetStatus.AVBRUTT;
    }
}
