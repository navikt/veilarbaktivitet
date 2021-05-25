package no.nav.veilarbaktivitet.avtaltMedNav;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.RequiredArgsConstructor;
import no.nav.veilarbaktivitet.db.dao.AktivitetDAO;
import no.nav.veilarbaktivitet.domain.AktivitetDTO;
import no.nav.veilarbaktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.domain.AktivitetTransaksjonsType;
import no.nav.veilarbaktivitet.domain.AktivitetTypeData;
import no.nav.veilarbaktivitet.mappers.AktivitetDTOMapper;
import no.nav.veilarbaktivitet.service.MetricService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class AvtaltMedNavService {
    private final MetricService metricService;
    private final MeterRegistry meterRegistry;
    private final AktivitetDAO dao;

    public AvtaltMedNavService(MetricService metricService,
             MeterRegistry meterRegistry,
            AktivitetDAO dao) {
        this.meterRegistry = meterRegistry;
        this.metricService = metricService;
        this.dao = dao;
        List<Tag> tags = Arrays.stream(Forhaandsorientering.Type.values()).map(t -> new Tag() {
                    @Override
                    public String getKey() {
                        return t.name();
                    }

                    @Override
                    public String getValue() {
                        return "ForhaandsorienteringType";
                    }
                }).collect(Collectors.toList());
        tags.addAll(Arrays.stream(AktivitetTypeData.values()).map(t -> new Tag() {
            @Override
            public String getKey() {
                return t.name();
            }

            @Override
            public String getValue() {
                return "AktivitetType";
            }
        }).collect(Collectors.toList()));
        meterRegistry.counter("aktivitet.avtalt.med.nav", tags);
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

        meterRegistry.counter("aktivitet.avtalt.med.nav", forhaandsorientering.getType().name(), aktivitet.getAktivitetType().name()).increment();


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
