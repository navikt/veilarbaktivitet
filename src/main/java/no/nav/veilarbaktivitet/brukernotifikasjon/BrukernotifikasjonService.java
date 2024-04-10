package no.nav.veilarbaktivitet.brukernotifikasjon;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO;
import no.nav.veilarbaktivitet.arena.model.ArenaId;
import no.nav.veilarbaktivitet.manuell_status.v2.ManuellStatusV2Client;
import no.nav.veilarbaktivitet.manuell_status.v2.ManuellStatusV2DTO;
import no.nav.veilarbaktivitet.oppfolging.periode.SistePeriodeService;
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

    private final ManuellStatusV2Client manuellStatusClient;
    private final String aktivitetsplanBasepath;
    private final AktivitetDAO aktivitetDAO;

    public BrukernotifikasjonService(PersonService personService, SistePeriodeService sistePeriodeService, BrukerNotifikasjonDAO dao, ManuellStatusV2Client manuellStatusClient, @Value("${app.env.aktivitetsplan.basepath}") String aktivitetsplanBasepath,AktivitetDAO aktivitetDAO) {
        this.personService = personService;
        this.sistePeriodeService = sistePeriodeService;
        this.dao = dao;
        this.manuellStatusClient = manuellStatusClient;
        this.aktivitetsplanBasepath = aktivitetsplanBasepath;
        this.aktivitetDAO = aktivitetDAO;
    }


    public void setDone(
            long aktivitetId,
            VarselType varseltype
    ) {
        dao.setDone(aktivitetId, varseltype);
    }

    public void setDone(
            ArenaId aktivitetId,
            VarselType varseltype
    ) {
        dao.setDone(aktivitetId, varseltype);
    }

    public boolean kanVarsles(
            Person.AktorId aktorId
    ) {
        Optional<ManuellStatusV2DTO> manuellStatusResponse = manuellStatusClient.get(aktorId);

        boolean erManuell = manuellStatusResponse.map(ManuellStatusV2DTO::isErUnderManuellOppfolging).orElse(true);
        boolean erReservertIKrr = manuellStatusResponse.map(ManuellStatusV2DTO::getKrrStatus).map(ManuellStatusV2DTO.KrrStatus::isErReservert).orElse(true);


        boolean kanVarsles = !erManuell && !erReservertIKrr;
        if(!kanVarsles) {
            secureLogs.info("bruker kan ikke varsles aktorId: {}, erManuell: {}, erReservertIKrr: {}", aktorId.get(), erManuell, erReservertIKrr);
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
        dao.kobleAktivitetIdTilBrukernotifikasjon(brukernotifikasjonId, aktivitetId, aktitetVersion);

        return uuid;

    }

    public boolean finnesBrukernotifikasjonMedVarselTypeForAktivitet(long aktivitetsId, VarselType varselType) {
        return dao.finnesBrukernotifikasjonMedVarselTypeForAktivitet(aktivitetsId, varselType);
    }

    @Transactional
    public UUID opprettVarselPaaArenaAktivitet(
            ArenaId arenaAktivitetId,
            Optional<Long> aktivitetId,
            Person.Fnr fnr,
            String ditNavTekst,
            VarselType varseltype
    ) {
        UUID uuid = UUID.randomUUID();

        Person.AktorId aktorId = personService
                .getAktorIdForPersonBruker(fnr)
                .orElseThrow();

        UUID gjeldendeOppfolgingsperiode = sistePeriodeService.hentGjeldendeOppfolgingsperiodeMedFallback(aktorId);
        URL aktivtetUrl = createAktivitetLink(aktivitetId.map(Object::toString).orElseGet(arenaAktivitetId::id));

        // epostTittel, epostBody og smsTekst settes til standartekst av brukernotifiaksjoenr hvis ikke satt
        long brukernotifikasjonId = dao.opprettBrukernotifikasjon(uuid, fnr, ditNavTekst, gjeldendeOppfolgingsperiode, varseltype, VarselStatus.PENDING, aktivtetUrl, null, null, null);
        dao.kobleArenaAktivitetIdTilBrukernotifikasjon(brukernotifikasjonId, arenaAktivitetId);
        // Populer brukernotifikasjon koblingstabell til vanlig aktivitet også
        aktivitetId
            .flatMap(aktivitetDAO::hentMaybeAktivitet)
            .ifPresent(aktivitet -> dao.kobleAktivitetIdTilBrukernotifikasjon(brukernotifikasjonId, aktivitet.getId(), aktivitet.getVersjon()));

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
