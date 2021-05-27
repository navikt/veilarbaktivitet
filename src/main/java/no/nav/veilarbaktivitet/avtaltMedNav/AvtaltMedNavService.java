package no.nav.veilarbaktivitet.avtaltMedNav;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import no.nav.veilarbaktivitet.db.dao.AktivitetDAO;
import no.nav.veilarbaktivitet.domain.AktivitetDTO;
import no.nav.veilarbaktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.domain.AktivitetTransaksjonsType;
import no.nav.veilarbaktivitet.domain.InnsenderData;
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
    private final MeterRegistry meterRegistry;
    private final AktivitetDAO dao;

    AktivitetData hentAktivitet(long aktivitetId) {
        return dao.hentAktivitet(aktivitetId);
    }

    AktivitetDTO markerSomAvtaltMedNav(long aktivitetId, AvtaltMedNav avtaltMedNav, @NonNull Person innloggetBruker) {
        AktivitetData aktivitet = dao.hentAktivitet(aktivitetId);
        var forhaandsorientering = avtaltMedNav.getForhaandsorientering();

        if (forhaandsorientering.getTekst() != null && forhaandsorientering.getTekst().isEmpty()) {
            forhaandsorientering.setTekst(null);
        }

        AktivitetData nyAktivitet = aktivitet
                .toBuilder()
                .avtalt(true)
                .forhaandsorientering(forhaandsorientering)
                .transaksjonsType(AktivitetTransaksjonsType.AVTALT)
                .endretAv(innloggetBruker.get())
                .lagtInnAv(InnsenderData.NAV) // alltid NAV
                .build();

        dao.insertAktivitet(nyAktivitet);

        metricService.oppdaterAktivitetMetrikk(aktivitet, true, aktivitet.isAutomatiskOpprettet());
        meterRegistry.counter("aktivitet.avtalt.med.nav", forhaandsorientering.getType().name(), aktivitet.getAktivitetType().name()).increment();

        return AktivitetDTOMapper.mapTilAktivitetDTO(dao.hentAktivitet(aktivitetId));
    }

    AktivitetDTO markerSomLest(AktivitetData aktivitetData, @NonNull Person innloggetBruker) {

        Forhaandsorientering fho = aktivitetData.getForhaandsorientering().toBuilder().lest(new Date()).build();

        AktivitetData aktivitet = aktivitetData
                .toBuilder()
                .forhaandsorientering(fho)
                .transaksjonsType(AktivitetTransaksjonsType.FORHAANDSORIENTERING_LEST)
                .endretAv(innloggetBruker.get())
                .lagtInnAv(InnsenderData.BRUKER) // alltid Bruker
                .build();

        dao.insertAktivitet(aktivitet);

        return AktivitetDTOMapper.mapTilAktivitetDTO(dao.hentAktivitet(aktivitetData.getId()));
    }
}
