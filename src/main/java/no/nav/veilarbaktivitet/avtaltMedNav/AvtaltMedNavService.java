package no.nav.veilarbaktivitet.avtaltMedNav;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
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
public class AvtaltMedNavService {
    private final MetricService metricService;
    private final AktivitetDAO aktivitetDAO;
    private final ForhaandsorienteringDAO fhoDAO;
    private final MeterRegistry meterRegistry;

    public static final String AVTALT_MED_NAV_COUNTER = "aktivitet.avtalt.med.nav";
    public static final String AKTIVITET_TYPE_LABEL = "AktivitetType";
    public static final String FORHAANDSORIENTERING_TYPE_LABEL = "ForhaandsorienteringType";



    public AvtaltMedNavService(MetricService metricService,
                               AktivitetDAO aktivitetDAO,
                               ForhaandsorienteringDAO fhoDAO,
                               MeterRegistry meterRegistry) {

        this.metricService = metricService;
        this.aktivitetDAO = aktivitetDAO;
        this.fhoDAO = fhoDAO;
        this.meterRegistry = meterRegistry;

        Counter.builder(AVTALT_MED_NAV_COUNTER)
                .description("Antall aktiviteter som er avtalt med NAV")
                .tags(AKTIVITET_TYPE_LABEL, "", FORHAANDSORIENTERING_TYPE_LABEL, "")
                .register(meterRegistry);
    }

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
                .withTransaksjonsType(AktivitetTransaksjonsType.AVTALT)
                .withAvtalt(true);

        aktivitetDAO.insertAktivitet(nyAktivitet, now);

        metricService.oppdaterAktivitetMetrikk(nyAktivitet, true, nyAktivitet.isAutomatiskOpprettet());

        return AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetDAO.hentAktivitet(aktivitetId).withForhaandsorientering(fho));
    }

    public AktivitetDTO markerSomLest(Forhaandsorientering fho) {
        var aktivitet = aktivitetDAO.hentAktivitet(Long.parseLong(fho.getAktivitetId()));

        fhoDAO.markerSomLest(fho.getId(), new Date());
        fho = fhoDAO.getById(fho.getId());

        AktivitetData nyAktivitet = aktivitet
                .toBuilder()
                .forhaandsorientering(fho)
                .transaksjonsType(AktivitetTransaksjonsType.FORHAANDSORIENTERING_LEST)
                .build();

        aktivitetDAO.insertAktivitet(nyAktivitet);

        metricService.oppdaterAktivitetMetrikk(aktivitet, true, aktivitet.isAutomatiskOpprettet());
        meterRegistry.counter(AVTALT_MED_NAV_COUNTER, FORHAANDSORIENTERING_TYPE_LABEL, fho.getType().name(), AKTIVITET_TYPE_LABEL, aktivitet.getAktivitetType().name()).increment();

        return AktivitetDTOMapper.mapTilAktivitetDTO(nyAktivitet);
    }

    public Forhaandsorientering hentForhaandsorientering(long aktivitetId) {
        return fhoDAO.getFhoForAktivitet(aktivitetId);
    }

}
