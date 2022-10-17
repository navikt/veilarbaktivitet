package no.nav.veilarbaktivitet.avtalt_med_nav;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.NavIdent;
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO;
import no.nav.veilarbaktivitet.aktivitet.MetricService;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTransaksjonsType;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.aktivitet.mappers.AktivitetDTOMapper;
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukernotifikasjonService;
import no.nav.veilarbaktivitet.brukernotifikasjon.VarselType;
import no.nav.veilarbaktivitet.person.InnsenderData;
import no.nav.veilarbaktivitet.person.Person;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static no.nav.veilarbaktivitet.util.DateUtils.dateToLocalDateTime;

@Service
@Transactional
@Slf4j
public class AvtaltMedNavService {
    private final MetricService metricService;
    private final AktivitetDAO aktivitetDAO;
    private final ForhaandsorienteringDAO fhoDAO;
    private final MeterRegistry meterRegistry;
    private final BrukernotifikasjonService brukernotifikasjonService;

    public static final String AVTALT_MED_NAV_COUNTER = "aktivitet.avtalt.med.nav";
    public static final String AKTIVITET_TYPE_LABEL = "AktivitetType";
    public static final String FORHAANDSORIENTERING_TYPE_LABEL = "ForhaandsorienteringType";

    public static final String FORHAANDSORIENTERING_DITT_NAV_TEKST = "Du har fått en viktig oppgave du må gjøre.";


    public AvtaltMedNavService(MetricService metricService,
                               AktivitetDAO aktivitetDAO,
                               ForhaandsorienteringDAO fhoDAO,
                               MeterRegistry meterRegistry,
                               BrukernotifikasjonService brukernotifikasjonService) {

        this.metricService = metricService;
        this.aktivitetDAO = aktivitetDAO;
        this.fhoDAO = fhoDAO;
        this.meterRegistry = meterRegistry;
        this.brukernotifikasjonService = brukernotifikasjonService;

        Counter.builder(AVTALT_MED_NAV_COUNTER)
                .description("Antall aktiviteter som er avtalt med NAV")
                .tags(AKTIVITET_TYPE_LABEL, "", FORHAANDSORIENTERING_TYPE_LABEL, "")
                .register(meterRegistry);
    }

    AktivitetData hentAktivitet(long aktivitetId) {
        return aktivitetDAO.hentAktivitet(aktivitetId);
    }

    @Transactional
    public AktivitetDTO opprettFHO(AvtaltMedNavDTO avtaltDTO, long aktivitetId, Person.AktorId aktorId, NavIdent ident) {
        var fhoDTO = avtaltDTO.getForhaandsorientering();
        Date now = new Date();

        if (fhoDTO.getTekst() != null && fhoDTO.getTekst().isEmpty()) {
            fhoDTO.setTekst(null);
        }

        var fho = fhoDAO.insert(avtaltDTO, aktivitetId, aktorId, ident.get(), now);
        if(!fhoDTO.getType().equals(Type.IKKE_SEND_FORHAANDSORIENTERING)) {
            if(!brukernotifikasjonService.kanVarsles(aktorId)){
                throw new IllegalStateException("bruker kan ikke varsles");
            }
            brukernotifikasjonService.opprettVarselPaaAktivitet(aktivitetId, avtaltDTO.getAktivitetVersjon(), aktorId, FORHAANDSORIENTERING_DITT_NAV_TEKST, VarselType.FORHAANDSORENTERING);
        }

        var nyAktivitet = aktivitetDAO.hentAktivitet(aktivitetId)
            //    .withVersjon(avtaltDTO.getAktivitetVersjon())
                .withForhaandsorientering(fho)
                .withFhoId(fho.getId())
                .withEndretDato(now)
                .withTransaksjonsType(AktivitetTransaksjonsType.AVTALT)
                .withEndretAv(ident.get())
                .withLagtInnAv(InnsenderData.NAV) // alltid NAV
                .withAvtalt(true);

        aktivitetDAO.oppdaterAktivitet(nyAktivitet, dateToLocalDateTime(now));

        metricService.oppdaterAktivitetMetrikk(nyAktivitet, true, nyAktivitet.isAutomatiskOpprettet());
        meterRegistry.counter(AVTALT_MED_NAV_COUNTER, FORHAANDSORIENTERING_TYPE_LABEL, fho.getType().name(), AKTIVITET_TYPE_LABEL, nyAktivitet.getAktivitetType().name()).increment();

        return AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetDAO.hentAktivitet(aktivitetId).withForhaandsorientering(fho), false);
    }

    public AktivitetDTO markerSomLest(Forhaandsorientering fho, Person innloggetBruker) {
        var aktivitet = aktivitetDAO.hentAktivitet(Long.parseLong(fho.getAktivitetId()));
        var now = new Date();

        fhoDAO.markerSomLest(fho.getId(), now, aktivitet.getVersjon());
        fho = fhoDAO.getById(fho.getId());

        AktivitetData nyAktivitet = aktivitet
                .toBuilder()
                .fhoId(fho.getId())
                .forhaandsorientering(fho)
                .endretDato(now)
                .transaksjonsType(AktivitetTransaksjonsType.FORHAANDSORIENTERING_LEST)
                .endretAv(innloggetBruker.get())
                .lagtInnAv(InnsenderData.BRUKER) // alltid Bruker
                .build();

        aktivitetDAO.oppdaterAktivitet(nyAktivitet);
        brukernotifikasjonService.setDone(Integer.parseInt(fho.getAktivitetId()), VarselType.FORHAANDSORENTERING);

        metricService.oppdaterAktivitetMetrikk(aktivitet, true, aktivitet.isAutomatiskOpprettet());

        return AktivitetDTOMapper.mapTilAktivitetDTO(nyAktivitet, true);
    }

    public Forhaandsorientering hentFhoForAktivitet(long aktivitetId) {
        return fhoDAO.getFhoForAktivitet(aktivitetId);
    }

    public Forhaandsorientering hentFHO(String fhoId) {
        if(fhoId == null) return null;
        return fhoDAO.getById(fhoId);
    }

    public List<Forhaandsorientering> hentFHO(List<String> fhoIder){
        var fhoIderUtenTomme = fhoIder.stream().filter(x-> x != null && !x.isEmpty()).collect(Collectors.toList());
        return fhoDAO.getById(fhoIderUtenTomme);
    }

    public boolean settVarselFerdig(String forhaandsorienteringId) {
        if (forhaandsorienteringId == null) return false;
        return fhoDAO.settVarselFerdig(forhaandsorienteringId);
    }
}
