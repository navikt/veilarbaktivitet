package no.nav.veilarbaktivitet.brukernotifikasjon;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.manuell_status.v2.ManuellStatusV2Client;
import no.nav.veilarbaktivitet.manuell_status.v2.ManuellStatusV2DTO;
import no.nav.veilarbaktivitet.nivaa4.Nivaa4Client;
import no.nav.veilarbaktivitet.nivaa4.Nivaa4DTO;
import no.nav.veilarbaktivitet.oppfolging.siste_periode.SistePeriodeService;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.person.PersonService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@EnableScheduling
public class BrukernotifikasjonArenaAktivitetService {

    private final PersonService personService;
    private final SistePeriodeService sistePeriodeService;
    private final BrukerNotifikasjonArenaDAO dao;

    private final Nivaa4Client nivaa4Client;
    private final ManuellStatusV2Client manuellStatusClient;

    private final String aktivitetsplanBasepath;

    public BrukernotifikasjonArenaAktivitetService(PersonService personService, SistePeriodeService sistePeriodeService, BrukerNotifikasjonArenaDAO dao, Nivaa4Client nivaa4Client, ManuellStatusV2Client manuellStatusClient, @Value("${app.env.aktivitetsplan.basepath}") String aktivitetsplanBasepath) {
        this.personService = personService;
        this.sistePeriodeService = sistePeriodeService;
        this.dao = dao;
        this.nivaa4Client = nivaa4Client;
        this.manuellStatusClient = manuellStatusClient;
        this.aktivitetsplanBasepath = aktivitetsplanBasepath;
    }


    public void setDone(
            String arenaAktivitetID,
            VarselType varseltype
    ) {
        dao.setDone(arenaAktivitetID, varseltype);
    }

    public boolean kanVarsles(
            Person.AktorId aktorId
    ) {
        Optional<ManuellStatusV2DTO> manuellStatusResponse = manuellStatusClient.get(aktorId);
        Optional<Nivaa4DTO> nivaa4DTO = nivaa4Client.get(aktorId);

        boolean erManuell = manuellStatusResponse.map(ManuellStatusV2DTO::isErUnderManuellOppfolging).orElse(true);
        boolean erReservertIKrr = manuellStatusResponse.map(ManuellStatusV2DTO::getKrrStatus).map(ManuellStatusV2DTO.KrrStatus::isErReservert).orElse(true);
        boolean harBruktNivaa4 = nivaa4DTO.map(Nivaa4DTO::isHarbruktnivaa4).orElse(false);

        return !erManuell && !erReservertIKrr && harBruktNivaa4;
    }

    public UUID opprettVarselPaaAktivitet(
            String arenaAktivitetId,
            Person.Fnr fnr,
            String ditNavTekst,
            VarselType varseltype
    ) {
        return opprettVarselPaaAktivitet(
                arenaAktivitetId,
                fnr,
                ditNavTekst,
                varseltype,
                null, null, null //Disse settes til standartekst av brukernotifiaksjoenr hvis ikke satt
        );
    }

    UUID opprettVarselPaaAktivitet(
            String arenaAktivitetId,
            Person.Fnr fnr,
            String ditNavTekst,
            VarselType varseltype,
            String epostTitel,
            String epostBody,
            String smsTekst
    ) {
        UUID uuid = UUID.randomUUID();

        Person.AktorId aktorId = personService
                .getAktorIdForPersonBruker(fnr).orElseThrow();

        UUID gjeldendeOppfolgingsperiode = sistePeriodeService.hentGjeldendeOppfolgingsperiodeMedFallback(aktorId);
        URL aktivtetUrl = createAktivitetLink(arenaAktivitetId);

        dao.opprettBrukernotifikasjon(uuid, arenaAktivitetId, fnr, ditNavTekst, gjeldendeOppfolgingsperiode, varseltype, VarselStatus.PENDING, aktivtetUrl, epostTitel, epostBody, smsTekst);
        return uuid;

    }

    @SneakyThrows
    private URL createAktivitetLink(String aktivitetId) {
        return new URL(aktivitetsplanBasepath + "/aktivitet/vis/" + aktivitetId);
    }
}
