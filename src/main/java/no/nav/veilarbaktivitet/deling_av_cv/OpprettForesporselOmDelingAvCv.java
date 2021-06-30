package no.nav.veilarbaktivitet.deling_av_cv;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.veilarbaktivitet.avro.Arbeidssted;
import no.nav.veilarbaktivitet.avro.DelingAvCvRespons;
import no.nav.veilarbaktivitet.avro.ForesporselOmDelingAvCv;
import no.nav.veilarbaktivitet.avro.SvarEnum;
import no.nav.veilarbaktivitet.domain.*;
import no.nav.veilarbaktivitet.kvp.KvpService;
import no.nav.veilarbaktivitet.oppfolging_status.OppfolgingStatusClient;
import no.nav.veilarbaktivitet.oppfolging_status.OppfolgingStatusDTO;
import no.nav.veilarbaktivitet.service.AktivitetService;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
public class OpprettForesporselOmDelingAvCv {
    private final KvpService kvpService;
    private final AktivitetService aktivitetService;
    private final DelingAvCvService delingAvCvService;
    private final OppfolgingStatusClient oppfolgingStatusClient;
    private final KafkaProducerClient<String, DelingAvCvRespons> producerClient;

    // TODO sett opp kafka consumer
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
            sendIkkeOpprettet(aktivitetData, melding);
            return;
        }

        if (kvpService.erUnderKvp(aktorId)) {
            sendIkkeOpprettet(aktivitetData, melding);
            return;
        }


        Person.NavIdent navIdent = Person.navIdent(melding.getOpprettetAv());

        long aktivitetId = aktivitetService.opprettAktivitet(aktorId, aktivitetData, navIdent);

        AktivitetData aktivitetMedId = aktivitetData.withId(aktivitetId);

        if (erManuell) {
            sendOpprettetIkkeVarslet(aktivitetMedId, melding );
        } else if (false) { //TODO ikke nivå 4
            sendOpprettetIkkeVarslet(aktivitetMedId, melding);
        } else {
            sendOpprettet(aktivitetMedId, melding);
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


        List<Arbeidssted> arbeidssteder = melding.getArbeidssteder();//TODO finn ut av denne
        String arbeidsted = arbeidssteder
                .stream()
                .map(it -> "Norge".equals(it.getLand()) ? it.getKommune() : it.getLand())
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
                .lenke(null) //TODO finn ut hva som skal her? bør vi endre meldingen
                .automatiskOpprettet(false) //TODO finn ut hva som skal vere her
                .opprettetDato(new Date())
                .endretDato(new Date())
                .stillingFraNavData(stillingFraNavData)
                .build();
    }

    private void sendIkkeOpprettet(AktivitetData aktivitetData, ForesporselOmDelingAvCv melding) {
        sendRespons(false, false, melding, aktivitetData);
    }

    private void sendOpprettetIkkeVarslet(AktivitetData aktivitetData, ForesporselOmDelingAvCv melding) {
        sendRespons(true, false, melding, aktivitetData);
    }

    private void sendOpprettet(AktivitetData aktivitetData, ForesporselOmDelingAvCv melding) {
        sendRespons(true, true, melding, aktivitetData);
    }

    private void sendRespons(boolean aktivitetOpprettet, boolean brukerVarslet, ForesporselOmDelingAvCv melding, AktivitetData aktivitetData) {
        DelingAvCvRespons delingAvCvRespons = new DelingAvCvRespons();
        delingAvCvRespons.setBestillingsId(melding.getBestillingsId());
        delingAvCvRespons.setAktivitetOpprettet(aktivitetOpprettet);
        delingAvCvRespons.setBrukerVarslet(brukerVarslet);
        delingAvCvRespons.setAktorId(melding.getAktorId());
        delingAvCvRespons.setAktivitetId(aktivitetData.getId() != null ? aktivitetData.getId().toString() : null);
        delingAvCvRespons.setBrukerSvar(SvarEnum.IKKE_SVART);


        ProducerRecord<String, DelingAvCvRespons> stringDelingAvCvResponsProducerRecord = new ProducerRecord<>("", delingAvCvRespons.getBestillingsId(), delingAvCvRespons);
        producerClient.send(stringDelingAvCvResponsProducerRecord);
    }
}
