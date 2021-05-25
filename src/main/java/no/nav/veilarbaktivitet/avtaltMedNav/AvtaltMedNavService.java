package no.nav.veilarbaktivitet.avtaltMedNav;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import no.nav.veilarbaktivitet.db.dao.AktivitetDAO;
import no.nav.veilarbaktivitet.domain.AktivitetDTO;
import no.nav.veilarbaktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.domain.AktivitetTransaksjonsType;
import no.nav.veilarbaktivitet.mappers.AktivitetDTOMapper;
import no.nav.veilarbaktivitet.service.MetricService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@Transactional
public class AvtaltMedNavService {
    private final MetricService metricService;
    private final MeterRegistry meterRegistry;
    private final AktivitetDAO dao;

    public static final String AVTALT_MED_NAV_COUNTER = "aktivitet.avtalt.med.nav";
    public static final String AKTIVITET_TYPE_LABEL = "AktivitetType";
    public static final String FORHAANDSORIENTERING_TYPE_LABEL = "ForhaandsorienteringType";



    public AvtaltMedNavService(MetricService metricService,
             MeterRegistry meterRegistry,
            AktivitetDAO dao) {
        this.meterRegistry = meterRegistry;
        this.metricService = metricService;
        this.dao = dao;
        Counter.builder(AVTALT_MED_NAV_COUNTER)
                .description("Antall aktiviteter som er avtalt med NAV")
                .tags(AKTIVITET_TYPE_LABEL, "", FORHAANDSORIENTERING_TYPE_LABEL, "")
                .register(meterRegistry);
    }

    AktivitetData hentAktivitet(long aktivitetId) {
        return dao.hentAktivitet(aktivitetId);
    }

    AktivitetDTO markerSomAvtaltMedNav(long aktivitetId, AvtaltMedNav avtaltMedNav) {
        AktivitetData aktivitet = dao.hentAktivitet(aktivitetId);
        Forhaandsorientering forhaandsorientering = avtaltMedNav.getForhaandsorientering();

        if (forhaandsorientering.getTekst() != null && forhaandsorientering.getTekst().isEmpty()) {
            forhaandsorientering.setTekst(null);
        }

        AktivitetData nyAktivitet = aktivitet
                .toBuilder()
                .avtalt(true)
                .forhaandsorientering(forhaandsorientering)
                .transaksjonsType(AktivitetTransaksjonsType.AVTALT)
                .build();

        dao.insertAktivitet(nyAktivitet);

        metricService.oppdaterAktivitetMetrikk(aktivitet, true, aktivitet.isAutomatiskOpprettet());


        meterRegistry.counter(AVTALT_MED_NAV_COUNTER, FORHAANDSORIENTERING_TYPE_LABEL, forhaandsorientering.getType().name(), AKTIVITET_TYPE_LABEL, aktivitet.getAktivitetType().name()).increment();


        return AktivitetDTOMapper.mapTilAktivitetDTO(dao.hentAktivitet(aktivitetId));
    }

    AktivitetDTO markerSomLest(AktivitetData aktivitetData) {

        Forhaandsorientering fho = aktivitetData.getForhaandsorientering().toBuilder().lest(new Date()).build();

        AktivitetData aktivitet = aktivitetData
                .toBuilder()
                .forhaandsorientering(fho)
                .transaksjonsType(AktivitetTransaksjonsType.FORHAANDSORIENTERING_LEST)
                .build();

        dao.insertAktivitet(aktivitet);

        return AktivitetDTOMapper.mapTilAktivitetDTO(dao.hentAktivitet(aktivitetData.getId()));
    }
}
