package no.nav.veilarbaktivitet.kvp;

import lombok.RequiredArgsConstructor;
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTransaksjonsType;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData;
import no.nav.veilarbaktivitet.person.Person;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class KVPAvsluttetService {
    private final AktivitetDAO aktivitetDAO;

    public void settAktiviteterInomKVPPeriodeTilAvbrutt(Person.AktorId aktoerId, String avsluttetBegrunnelse, Date avsluttetDato) {
        aktivitetDAO.hentAktiviteterForAktorId(aktoerId)
                .stream()
                .filter(this::filtrerKontorSperretOgStatusErIkkeAvBruttEllerFullfort)
                .filter(this::filtrerEksterneAktiviteter)
                .filter(aktitet -> aktitet.getOpprettetDato().before(avsluttetDato))
                .map(aktivitetData -> settKVPAktivitetTilAvbrutt(aktivitetData, avsluttetBegrunnelse, avsluttetDato))
                .forEach(aktivitetDAO::oppdaterAktivitet);
    }

    private boolean filtrerKontorSperretOgStatusErIkkeAvBruttEllerFullfort(AktivitetData aktivitetData) {
        var aktivitetStatus = aktivitetData.getStatus();
        return aktivitetData.getKontorsperreEnhetId() != null && !(aktivitetStatus.equals(AktivitetStatus.AVBRUTT) || aktivitetStatus.equals(AktivitetStatus.FULLFORT));
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
}
