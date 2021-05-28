package no.nav.veilarbaktivitet.avtaltMedNav;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.NavIdent;
import no.nav.veilarbaktivitet.db.dao.AktivitetDAO;
import no.nav.veilarbaktivitet.domain.AktivitetDTO;
import no.nav.veilarbaktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.domain.AktivitetTransaksjonsType;
import no.nav.veilarbaktivitet.domain.Person;
import no.nav.veilarbaktivitet.mappers.AktivitetDTOMapper;
import no.nav.veilarbaktivitet.service.MetricService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@Transactional
@RequiredArgsConstructor
public class AvtaltMedNavService {
    private final MetricService metricService;
    private final AktivitetDAO aktivitetDAO;
    private final ForhaandsorienteringDAO fhoDAO;
    private final MeterRegistry meterRegistry;

    AktivitetData hentAktivitet(long aktivitetId) {
        return aktivitetDAO.hentAktivitet(aktivitetId);
    }

    public AktivitetDTO opprettFHO(AvtaltMedNavDTO avtaltDTO, long aktivitetId, Person.AktorId aktorId, NavIdent ident) {
        var fhoDTO = avtaltDTO.getForhaandsorientering();
        Date now = new Date();

        if (fhoDTO.getTekst() != null && fhoDTO.getTekst().isEmpty()) {
            fhoDTO.setTekst(null);
        }

        var fho = fhoDAO.insert(avtaltDTO, aktivitetId, aktorId, ident.get(), now);

        var nyAktivitet = aktivitetDAO.hentAktivitet(aktivitetId)
                .withForhaandsorientering(fho)
                .withEndretDato(now)
                .withTransaksjonsType(AktivitetTransaksjonsType.AVTALT)
                .withAvtalt(true);

        aktivitetDAO.insertAktivitet(nyAktivitet, now);

        metricService.oppdaterAktivitetMetrikk(nyAktivitet, true, nyAktivitet.isAutomatiskOpprettet());
        meterRegistry.counter("aktivitet.avtalt.med.nav", fhoDTO.getType().name(), nyAktivitet.getAktivitetType().name()).increment();

        return AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetDAO.hentAktivitet(aktivitetId).withForhaandsorientering(fho));
    }

    public AktivitetDTO markerSomLest(Forhaandsorientering fho) {
        var aktivitet = aktivitetDAO.hentAktivitet(Long.parseLong(fho.getAktivitetId()));
        var now = new Date();

        fhoDAO.markerSomLest(fho.getId(), now, aktivitet.getVersjon());
        fho = fhoDAO.getById(fho.getId());

        AktivitetData nyAktivitet = aktivitet
                .toBuilder()
                .forhaandsorientering(fho)
                .endretDato(now)
                .transaksjonsType(AktivitetTransaksjonsType.FORHAANDSORIENTERING_LEST)
                .build();

        aktivitetDAO.insertAktivitet(nyAktivitet);
        return AktivitetDTOMapper.mapTilAktivitetDTO(nyAktivitet);
    }

    public Forhaandsorientering hentForhaandsorientering(long aktivitetId) {
        return fhoDAO.getFhoForAktivitet(aktivitetId);
    }

}
