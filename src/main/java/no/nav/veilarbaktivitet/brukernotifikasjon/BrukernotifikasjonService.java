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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URL;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@EnableScheduling
public class BrukernotifikasjonService {
    private final Logger secureLogs = LoggerFactory.getLogger("SecureLog");

    private final PersonService personService;
    private final SistePeriodeService sistePeriodeService;
    private final BrukerNotifikasjonDAO dao;

    private final Nivaa4Client nivaa4Client;
    private final ManuellStatusV2Client manuellStatusClient;
    private final String aktivitetsplanBasepath;

    public BrukernotifikasjonService(PersonService personService, SistePeriodeService sistePeriodeService, BrukerNotifikasjonDAO dao, Nivaa4Client nivaa4Client, ManuellStatusV2Client manuellStatusClient, @Value("${app.env.aktivitetsplan.basepath}") String aktivitetsplanBasepath) {
        this.personService = personService;
        this.sistePeriodeService = sistePeriodeService;
        this.dao = dao;
        this.nivaa4Client = nivaa4Client;
        this.manuellStatusClient = manuellStatusClient;
        this.aktivitetsplanBasepath = aktivitetsplanBasepath;
    }


    public void setDone(
            long aktivitetId,
            VarselType varseltype
    ) {
        dao.setDone(aktivitetId, varseltype);
    }

    public void setDone(
            String aktivitetId,
            VarselType varseltype
    ) {
        dao.setDone(aktivitetId, varseltype);
    }

    public boolean kanVarsles(
            Person.AktorId aktorId
    ) {
        Optional<ManuellStatusV2DTO> manuellStatusResponse = manuellStatusClient.get(aktorId);
        Optional<Nivaa4DTO> nivaa4DTO = nivaa4Client.get(aktorId);

        boolean erManuell = manuellStatusResponse.map(ManuellStatusV2DTO::isErUnderManuellOppfolging).orElse(true);
        boolean erReservertIKrr = manuellStatusResponse.map(ManuellStatusV2DTO::getKrrStatus).map(ManuellStatusV2DTO.KrrStatus::isErReservert).orElse(true);
        boolean harBruktNivaa4 = nivaa4DTO.map(Nivaa4DTO::isHarbruktnivaa4).orElse(false);


        boolean kanVarsles = !erManuell && !erReservertIKrr && harBruktNivaa4;
        if(!kanVarsles) {
            secureLogs.info("bruker kan ikke varsles aktorId: {}, erManuell: {}, erReservertIKrr: {}, harBruktNivaa4: {}", aktorId.get(), erManuell, erReservertIKrr, harBruktNivaa4);
        }

        return kanVarsles;
    }

    @Transactional
    public UUID opprettVarselPaaAktivitet(
            long aktivitetId,
            long aktitetVersion,
            Person.AktorId aktorId,
            String ditNavTekst,
            VarselType varseltype
    ) {
        return opprettVarselPaaAktivitet(
                aktivitetId,
                aktitetVersion,
                aktorId,
                ditNavTekst,
                varseltype,
                null, null, null //Disse settes til standartekst av brukernotifiaksjoenr hvis ikke satt
        );
    }

    @Transactional
    public UUID opprettVarselPaaAktivitet(
            long aktivitetId,
            long aktitetVersion,
            Person.AktorId aktorId,
            String ditNavTekst,
            VarselType varseltype,
            String epostTitel,
            String epostBody,
            String smsTekst
    ) {
        UUID uuid = UUID.randomUUID();

        Person.Fnr fnr = personService
                .getFnrForAktorId(aktorId);

        UUID gjeldendeOppfolgingsperiode = sistePeriodeService.hentGjeldendeOppfolgingsperiodeMedFallback(aktorId);
        URL aktivtetUrl = createAktivitetLink(aktivitetId + "");

        long brukernotifikasjonId = dao.opprettBrukernotifikasjon(uuid, fnr, ditNavTekst, gjeldendeOppfolgingsperiode, varseltype, VarselStatus.PENDING, aktivtetUrl, epostTitel, epostBody, smsTekst);
        dao.aktivitetTilBrukernotifikasjon(brukernotifikasjonId, aktivitetId, aktitetVersion);

        return uuid;

    }

    @Transactional
    public UUID opprettVarselPaaArenaAktivitet(
            String arenaAktivitetId,
            Person.Fnr fnr,
            String ditNavTekst,
            VarselType varseltype
    ) {
        return opprettVarselPaaArenaAktivitet(
                arenaAktivitetId,
                fnr,
                ditNavTekst,
                varseltype,
                null, null, null //Disse settes til standartekst av brukernotifiaksjoenr hvis ikke satt
        );
    }

    @Transactional
    public UUID opprettVarselPaaArenaAktivitet(
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
                .getAktorIdForPersonBruker(fnr)
                .orElseThrow();

        UUID gjeldendeOppfolgingsperiode = sistePeriodeService.hentGjeldendeOppfolgingsperiodeMedFallback(aktorId);
        URL aktivtetUrl = createAktivitetLink(arenaAktivitetId);

        long brukernotifikasjonId = dao.opprettBrukernotifikasjon(uuid, fnr, ditNavTekst, gjeldendeOppfolgingsperiode, varseltype, VarselStatus.PENDING, aktivtetUrl, epostTitel, epostBody, smsTekst);
        dao.arenaAktivitetTilBrukernotifikasjon(brukernotifikasjonId, arenaAktivitetId);

        return uuid;

    }

    @SneakyThrows
    private URL createAktivitetLink(String aktivitetId) {
        return new URL(aktivitetsplanBasepath + "/aktivitet/vis/" + aktivitetId);
    }

    public void setDoneGrupperingsID(UUID uuid) {
        dao.setDoneGrupperingsID(uuid);
    }
}
