package no.nav.veilarbaktivitet.brukernotifikasjon;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.oppfolging.v2.OppfolgingPeriodeMinimalDTO;
import no.nav.veilarbaktivitet.oppfolging.v2.OppfolgingV2Client;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.person.PersonService;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@EnableScheduling
@RequiredArgsConstructor
public class BrukernotifikasjonService {

    private final PersonService personService;
    private final OppfolgingV2Client oppfolgingClient;
    private final BrukerNotifikasjonDAO dao;

    public void oppgaveDone(
            long aktivitetId,
            VarselType varseltype
    ) {
        dao.setDone(aktivitetId, varseltype);
    }

    public UUID opprettOppgavePaaAktivitet(
            long aktivitetId,
            long aktitetVersion,
            Person.AktorId aktorId,
            String tekst,
            VarselType varseltype,
            VarselFunksjon funksjon
    ) {
        UUID uuid = UUID.randomUUID();

        Person.Fnr fnr = personService
                .getFnrForAktorId(aktorId);

        OppfolgingPeriodeMinimalDTO oppfolging = oppfolgingClient.getGjeldendePeriode(aktorId)
                .orElseThrow(() -> new IllegalStateException("bruker ikke under oppfolging"));

        dao.opprettBrukernotifikasjon(uuid, aktivitetId, aktitetVersion, fnr, tekst, oppfolging.getUuid(), varseltype, VarselStatus.PENDING, funksjon);

        return uuid;
    }

}
