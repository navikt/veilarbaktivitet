package no.nav.veilarbaktivitet.brukernotifikasjon.oppgave;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.brukernotifikasjon.VarselType;
import no.nav.veilarbaktivitet.person.PersonService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URL;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
class BrukerNotifkasjonOppgaveService {
    private final OppgaveDao dao;
    private final PersonService personService;
    private final OppgaveProducer oppgaveProducer;
    private final BeskjedProducer beskjedProducer;

    @Value("${app.env.aktivitetsplan.basepath}")
    private String aktivitetsplanBasepath;

    @Transactional
    @Timed(value="brukernotifikasjon_opprett_oppgave_sendt")
    public void send(SkalSendes skalSendes) {
        boolean oppdatertOk = dao.setSendt(skalSendes.getBrukernotifikasjonLopeNummer());

        if (oppdatertOk) {
            sendVarsel(skalSendes);
        }
    }

    private void sendVarsel(SkalSendes skalSendes) {
        VarselType varselType = skalSendes.getVarselType();
        long offset = switch (varselType.getBrukernotifikasjonType()) {
            case OPPGAVE -> oppgaveProducer.sendOppgave(skalSendes);
            case BESKJED -> beskjedProducer.sendBeskjed(skalSendes);
        };

        log.debug("Brukernotifikasjon {} med type {} publisert med offset {}", skalSendes.getBrukernotifikasjonId(), varselType.getBrukernotifikasjonType().name(), offset);
    }

    @SneakyThrows
    private URL createAktivitetLink(long aktivitetId) {
        return new URL(aktivitetsplanBasepath + "/aktivitet/vis/" + aktivitetId);
    }

    List<SkalSendes> hentVarselSomSkalSendes(int maxAntall) {
        return dao.hentVarselSomSkalSendes(maxAntall);
    }

    int avbrytIkkeSendteOppgaverForAvslutteteAktiviteter() {
        return dao.avbrytIkkeSendteOppgaverForAvslutteteAktiviteter();
    }
}
