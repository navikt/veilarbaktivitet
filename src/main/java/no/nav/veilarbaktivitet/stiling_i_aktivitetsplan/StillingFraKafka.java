package no.nav.veilarbaktivitet.stiling_i_aktivitetsplan;

import lombok.RequiredArgsConstructor;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.veilarbaktivitet.avro.Arbeidssted;
import no.nav.veilarbaktivitet.avro.ForesporselOmDelingAvCv;
import no.nav.veilarbaktivitet.avro.KvitteringDelingAvCv;
import no.nav.veilarbaktivitet.avro.KvitteringsTypeEnum;
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
public class StillingFraKafka {
    private final KvpService kvpService;
    private final AktivitetService aktivitetService;
    private final OppfolgingStatusClient oppfolgingStatusClient;
    private final KafkaProducerClient<String, KvitteringDelingAvCv> producerClient;

    public void createAktivitet(ForesporselOmDelingAvCv melding) {
        String callId = melding.getCallId(); //TODO sett på loggen

        Person.AktorId aktorId = Person.aktorId(melding.getAktorId());
        Optional<OppfolgingStatusDTO> oppfolgingStatusDTO = oppfolgingStatusClient.get(aktorId);

        boolean underoppfolging = oppfolgingStatusDTO.map(OppfolgingStatusDTO::isUnderOppfolging).orElse(false);
        boolean erManuell = oppfolgingStatusDTO.map(OppfolgingStatusDTO::isErManuell).orElse(true);

        if (!underoppfolging) {
            sendIkkeOpprettet("ikke under oppfølging", melding);
            return;
        }

        if (kvpService.erUnderKvp(aktorId)) {
            sendIkkeOpprettet("", melding);
            return;
        }

        AktivitetData aktivitetData = map(melding);
        Person.NavIdent navIdent = Person.navIdent(melding.getOpprettetAv());

        aktivitetService.opprettAktivitet(aktorId, aktivitetData, navIdent); //TODO fiks idenpotent

        if (erManuell) {
            sendKanIkkeSvare(melding, "er manuell bruker");
        } else if (false) { //TODO ikke nivå 4
            sendKanIkkeSvare(melding, "ikke nivaa 4");
        } else {
            sendSuksess(melding);
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
        String bestillingsId = melding.getId();
        String stillingsId = melding.getStillingsId();


        List<Arbeidssted> arbeidssteder = melding.getArbeidssteder();//TODO finn ut av denne
        String arbeidsted = arbeidssteder
                .stream()
                .map(it -> "Norge".equals(it.getLand()) ? it.getKommune() : it.getLand())
                .collect(Collectors.joining(", "));

        StillingFraNavData stillingFraNavData = StillingFraNavData
                .builder()
                .soknadsfrist(soknadsfrist)
                .svarFrist(svarfrist)
                .arbeidsgiver(arbeidsgiver)
                .bestillingsId(bestillingsId)
                .stillingsId(stillingsId)
                .arbeidsSted(arbeidsted)
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

    private void sendIkkeOpprettet(String feilmelding, ForesporselOmDelingAvCv melding) {
        sendKvittering(KvitteringsTypeEnum.ikke_opprettet, melding, feilmelding);
    }

    private void sendKanIkkeSvare(ForesporselOmDelingAvCv melding, String feilmelding) {
        sendKvittering(KvitteringsTypeEnum.opprettet_kan_ikke_svare, melding, feilmelding);
    }

    private void sendSuksess(ForesporselOmDelingAvCv melding) {
        sendKvittering(KvitteringsTypeEnum.opprettet, melding, null);
    }

    private void sendKvittering(KvitteringsTypeEnum kvitteringsTypeEnum, ForesporselOmDelingAvCv melding, String feilmeldig) {
        KvitteringDelingAvCv KvitteringDelingAvCv = new KvitteringDelingAvCv();
        KvitteringDelingAvCv.setKvitteringsType(kvitteringsTypeEnum);
        KvitteringDelingAvCv.setAktorId(melding.getAktorId());
        KvitteringDelingAvCv.setFeilmelding(feilmeldig);

        ProducerRecord<String, KvitteringDelingAvCv> stringKvitteringDelingAvCvProducerRecord = new ProducerRecord<>("", KvitteringDelingAvCv.getId(), KvitteringDelingAvCv);
        producerClient.send(stringKvitteringDelingAvCvProducerRecord);
    }
}
