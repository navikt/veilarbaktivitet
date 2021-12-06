package no.nav.veilarbaktivitet.brukernotifikasjon;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.manuell_status.v2.ManuellStatusV2Client;
import no.nav.veilarbaktivitet.manuell_status.v2.ManuellStatusV2DTO;
import no.nav.veilarbaktivitet.nivaa4.Nivaa4Client;
import no.nav.veilarbaktivitet.nivaa4.Nivaa4DTO;
import no.nav.veilarbaktivitet.oppfolging.v2.OppfolgingPeriodeMinimalDTO;
import no.nav.veilarbaktivitet.oppfolging.v2.OppfolgingV2Client;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.person.PersonService;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@EnableScheduling
@RequiredArgsConstructor
public class BrukernotifikasjonService {

    private final PersonService personService;
    private final OppfolgingV2Client oppfolgingClient;
    private final BrukerNotifikasjonDAO dao;

    private final Nivaa4Client nivaa4Client;
    private final ManuellStatusV2Client manuellStatusClient;


    public void oppgaveDone(
            long aktivitetId,
            VarselType varseltype
    ) {
        dao.setAvbrutt(aktivitetId, varseltype);
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

        return !erManuell && !erReservertIKrr && harBruktNivaa4;
    }

    public UUID opprettOppgavePaaAktivitet(
            long aktivitetId,
            long aktitetVersion,
            Person.AktorId aktorId,
            String tekst,
            VarselType varseltype
    ) {
        UUID uuid = UUID.randomUUID();

        Person.Fnr fnr = personService
                .getFnrForAktorId(aktorId);

        OppfolgingPeriodeMinimalDTO oppfolging = oppfolgingClient.getGjeldendePeriode(aktorId)
                .orElseThrow(() -> new IllegalStateException("bruker ikke under oppfolging"));

        dao.opprettBrukernotifikasjon(uuid, aktivitetId, aktitetVersion, fnr, tekst, oppfolging.getUuid(), varseltype, VarselStatus.PENDING);

        return uuid;
    }

}
