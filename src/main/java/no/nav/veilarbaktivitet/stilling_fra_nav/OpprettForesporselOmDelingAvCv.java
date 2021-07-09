package no.nav.veilarbaktivitet.stilling_fra_nav;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.avro.Arbeidssted;
import no.nav.veilarbaktivitet.avro.ForesporselOmDelingAvCv;
import no.nav.veilarbaktivitet.domain.*;
import no.nav.veilarbaktivitet.kvp.KvpService;
import no.nav.veilarbaktivitet.oppfolging_status.OppfolgingStatusClient;
import no.nav.veilarbaktivitet.oppfolging_status.OppfolgingStatusDTO;
import no.nav.veilarbaktivitet.service.AktivitetService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpprettForesporselOmDelingAvCv {
    private final KvpService kvpService;
    private final AktivitetService aktivitetService;
    private final DelingAvCvService delingAvCvService;
    private final OppfolgingStatusClient oppfolgingStatusClient;
    private final StillingFraNavProducerClient producerClient;


    @KafkaListener(topics = "${topic.inn.stillingFraNav}")
    public void createAktivitet(ForesporselOmDelingAvCv melding) {
        if (delingAvCvService.aktivitetAlleredeOpprettetForBestillingsId(melding.getBestillingsId())) {
            log.info("ForesporselOmDelingAvCv med bestillingsId {} har allerede en aktivitet", melding.getBestillingsId());
            return;
        }
        Person.AktorId aktorId = Person.aktorId(melding.getAktorId());
        Optional<OppfolgingStatusDTO> oppfolgingStatusDTO = oppfolgingStatusClient.get(aktorId);

        boolean underoppfolging = oppfolgingStatusDTO.map(OppfolgingStatusDTO::isUnderOppfolging).orElse(false);
        boolean erManuell = oppfolgingStatusDTO.map(OppfolgingStatusDTO::isErManuell).orElse(true);

        AktivitetData aktivitetData = map(melding);

        if (!underoppfolging) {
            producerClient.sendIkkeOpprettet(aktivitetData, melding);
            return;
        }

        if (kvpService.erUnderKvp(aktorId)) {
            producerClient.sendIkkeOpprettet(aktivitetData, melding);
            return;
        }


        Person.NavIdent navIdent = Person.navIdent(melding.getOpprettetAv());

        long aktivitetId = aktivitetService.opprettAktivitet(aktorId, aktivitetData, navIdent);

        AktivitetData aktivitetMedId = aktivitetData.withId(aktivitetId);

        if (erManuell) {
            producerClient.sendOpprettetIkkeVarslet(aktivitetMedId, melding );
        } else if (false) { //TODO ikke nivå 4 og krr
            producerClient.sendOpprettetIkkeVarslet(aktivitetMedId, melding);
        } else {
            producerClient.sendOpprettet(aktivitetMedId, melding);
        }
    }

    private AktivitetData map(ForesporselOmDelingAvCv melding) {
        //aktivitdata
        String stillingstittel = melding.getStillingstittel();
        Person.AktorId aktorId = Person.aktorId(melding.getAktorId());
        Person.NavIdent navIdent = Person.navIdent(melding.getOpprettetAv());
        Instant opprettet = melding.getOpprettet();

        //nye kolloner
        Date svarfrist = new Date(melding.getSvarfrist().toEpochMilli());
        String arbeidsgiver = melding.getArbeidsgiver();
        String soknadsfrist = melding.getSoknadsfrist();
        String bestillingsId = melding.getBestillingsId();
        String stillingsId = melding.getStillingsId();


        List<Arbeidssted> arbeidssteder = melding.getArbeidssteder();
        String arbeidsted = arbeidssteder
                .stream()
                .map(it -> "Norge".equalsIgnoreCase(it.getLand()) ? it.getKommune() : it.getLand())
                .collect(Collectors.joining(", "));

        StillingFraNavData stillingFraNavData = StillingFraNavData
                .builder()
                .soknadsfrist(soknadsfrist)
                .svarfrist(svarfrist)
                .arbeidsgiver(arbeidsgiver)
                .bestillingsId(bestillingsId)
                .stillingsId(stillingsId)
                .arbeidssted(arbeidsted)
                .build();

        return AktivitetData
                .builder()
                .aktorId(aktorId.get())
                .tittel(stillingstittel)
                .aktivitetType(AktivitetTypeData.STILLING_FRA_NAV)
                .status(AktivitetStatus.BRUKER_ER_INTERESSERT)
                .transaksjonsType(AktivitetTransaksjonsType.OPPRETTET)
                .fraDato(new Date(opprettet.toEpochMilli()))
                .lagtInnAv(InnsenderData.NAV)
                .endretAv(navIdent.get())
                .lenke("/rekrutteringsbistand/" + stillingsId)
                .automatiskOpprettet(false)
                .opprettetDato(new Date())
                .endretDato(new Date())
                .stillingFraNavData(stillingFraNavData)
                .build();
    }
}
