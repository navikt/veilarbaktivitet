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
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class KVPAvsluttetService {
    private final AktivitetDAO aktivitetDAO;
    private final OversiktenService oversiktenService;

    public boolean finnesAktivitetMedKontorsperreEnhet(Person.AktorId aktoerId) {
        return aktivitetDAO.hentAktiviteterForAktorId(aktoerId).stream()
                .anyMatch(aktivitet -> aktivitet.getKontorsperreEnhetId() != null && erAktiv(aktivitet));
    }

    private void settAktiviteterInomKVPPeriodeTilAvbrutt(Person.AktorId aktoerId, String avsluttetBegrunnelse, Date kvpAvsluttetTidspunkt) {
        aktivitetDAO.hentAktiviteterForAktorId(aktoerId).stream()
                .filter(this::filtrerKontorSperretOgStatusErIkkeAvBruttEllerFullfort)
                .filter(this::filtrerEksterneAktiviteter)
                .filter(aktivitet -> aktivitet.getOpprettetDato().before(kvpAvsluttetTidspunkt))
                .map(aktivitet -> settKVPAktivitetTilAvbrutt(aktivitet, avsluttetBegrunnelse, kvpAvsluttetTidspunkt))
                .forEach(oppdatertAktivitet -> {
                    aktivitetDAO.oppdaterAktivitet(oppdatertAktivitet);

                    if (erAvbruttMoteEllerSamtalereferat(oppdatertAktivitet)) {
                        oversiktenService.lagreStoppMeldingOmUdeltSamtalereferatIUtboks(oppdatertAktivitet.getAktorId(), oppdatertAktivitet.getId()
                        );
                    }
                });
    }

    public void håndterKvpAvsluttet(Person.AktorId aktoerId, Optional<String> begrunnelse, Optional<Date> sluttDato) {
        Date kvpAvsluttetTidspunkt = sluttDato.orElseThrow(() -> new IllegalArgumentException("Mangler sluttDato/avsluttet for KVP AVSLUTTET"));
        boolean finnesAktivitetMedKontorSperrePåBruker = finnesAktivitetMedKontorsperreEnhet(aktoerId);
        if (!finnesAktivitetMedKontorSperrePåBruker) {
            return;
        }
        settAktiviteterInomKVPPeriodeTilAvbrutt(aktoerId, begrunnelse.orElse("KVP avsluttet"), kvpAvsluttetTidspunkt);
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
