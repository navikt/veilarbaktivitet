package no.nav.veilarbaktivitet.stilling_fra_nav;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.client.aktorregister.IngenGjeldendeIdentException;
import no.nav.veilarbaktivitet.aktivitet.AktivitetService;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTransaksjonsType;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData;
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukernotifikasjonService;
import no.nav.veilarbaktivitet.brukernotifikasjon.VarselType;
import no.nav.veilarbaktivitet.kvp.KvpService;
import no.nav.veilarbaktivitet.oppfolging.v2.OppfolgingV2Client;
import no.nav.veilarbaktivitet.oppfolging.v2.OppfolgingV2UnderOppfolgingDTO;
import no.nav.veilarbaktivitet.person.InnsenderData;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.stilling_fra_nav.deling_av_cv.Arbeidssted;
import no.nav.veilarbaktivitet.stilling_fra_nav.deling_av_cv.ForesporselOmDelingAvCv;
import no.nav.veilarbaktivitet.stilling_fra_nav.deling_av_cv.KontaktInfo;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpprettForesporselOmDelingAvCv {
    private final AktivitetService aktivitetService;
    private final DelingAvCvService delingAvCvService;
    private final KvpService kvpService;
    private final OppfolgingV2Client oppfolgingClient;
    private final BrukernotifikasjonService brukernotifikasjonService;
    private final StillingFraNavProducerClient producerClient;
    private final StillingFraNavMetrikker metrikker;

    private static final String BRUKERNOTIFIKASJON_TEKST = "Kan denne stillingen passe for deg? Vi leter etter jobbsøkere for en arbeidsgiver.";

    @Transactional
    @KafkaListener(topics = "${topic.inn.stillingFraNav}", containerFactory = "stringAvroKafkaListenerContainerFactory")
    public void createAktivitet(ConsumerRecord<String, ForesporselOmDelingAvCv> consumerRecord) {
        ForesporselOmDelingAvCv melding = consumerRecord.value();

        if (delingAvCvService.aktivitetAlleredeOpprettetForBestillingsId(melding.getBestillingsId())) {
            log.info("ForesporselOmDelingAvCv med bestillingsId={} har allerede en aktivitet", melding.getBestillingsId());
            return;
        }
        log.info("OpprettForesporselOmDelingAvCv.createAktivitet {}", melding);
        Person.AktorId aktorId = Person.aktorId(melding.getAktorId());
        log.info("OpprettForesporselOmDelingAvCv.createAktivitet AktorId={}", aktorId.get());

        if (aktorId.get() == null) {
            log.error("OpprettForesporselOmDelingAvCv.createAktivitet AktorId=null");
        }

        Optional<OppfolgingV2UnderOppfolgingDTO> oppfolgingResponse;
        try {
            oppfolgingResponse = oppfolgingClient.fetchUnderoppfolging(aktorId);
        } catch (IngenGjeldendeIdentException exception) {
            producerClient.sendUgyldigInput(melding.getBestillingsId(), aktorId.get(), "Finner ingen gydlig ident for aktorId");
            log.warn("*** Kan ikke behandle melding={}. Årsak: {} ***", melding, exception.getMessage());
            return;
        }

        boolean underKvp = kvpService.erUnderKvp(aktorId);
        boolean underOppfolging = oppfolgingResponse.map(OppfolgingV2UnderOppfolgingDTO::isErUnderOppfolging).orElse(false);

        if (!underOppfolging || underKvp) {
            producerClient.sendUgyldigOppfolgingStatus(melding.getBestillingsId(), aktorId.get());
            return;
        }

        Person.NavIdent navIdent = Person.navIdent(melding.getOpprettetAv());

        boolean kanVarsle = brukernotifikasjonService.kanVarsles(aktorId);

        AktivitetData aktivitetData = map(melding, kanVarsle);

        AktivitetData aktivitet = aktivitetService.opprettAktivitet(aktorId, aktivitetData, navIdent);

        if (kanVarsle) {
            brukernotifikasjonService.opprettOppgavePaaAktivitet(aktivitet.getId(), aktivitet.getVersjon(), aktorId, BRUKERNOTIFIKASJON_TEKST, VarselType.STILLING_FRA_NAV);
            producerClient.sendOpprettet(aktivitet);
        } else {
            producerClient.sendOpprettetIkkeVarslet(aktivitet);
        }

        metrikker.countStillingFraNavOpprettet(kanVarsle);
    }

    private static AktivitetData map(ForesporselOmDelingAvCv melding, boolean kanVarsle) {
        //aktivitetdata
        String stillingstittel = melding.getStillingstittel();
        Person.AktorId aktorId = Person.aktorId(melding.getAktorId());
        Person.NavIdent navIdent = Person.navIdent(melding.getOpprettetAv());
        Instant opprettet = melding.getOpprettet();

        //nye kolonner
        Date svarfrist = new Date(melding.getSvarfrist().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        String arbeidsgiver = melding.getArbeidsgiver();
        String soknadsfrist = melding.getSoknadsfrist();
        String bestillingsId = melding.getBestillingsId();
        String stillingsId = melding.getStillingsId();

        List<Arbeidssted> arbeidssteder = melding.getArbeidssteder();
        String arbeidsted = arbeidssteder
                .stream()
                .map(it -> "Norge".equalsIgnoreCase(it.getLand()) ? it.getKommune() : it.getLand())
                .collect(Collectors.joining(", "));

        KontaktpersonData kontaktpersonData = getKontaktInfo(melding.getKontaktInfo());

        StillingFraNavData stillingFraNavData = StillingFraNavData
                .builder()
                .soknadsfrist(soknadsfrist)
                .svarfrist(svarfrist)
                .arbeidsgiver(arbeidsgiver)
                .bestillingsId(bestillingsId)
                .stillingsId(stillingsId)
                .arbeidssted(arbeidsted)
                .kontaktpersonData(kontaktpersonData)
                .livslopsStatus(kanVarsle ? LivslopsStatus.PROVER_VARSLING : LivslopsStatus.KAN_IKKE_VARSLE)
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
                .automatiskOpprettet(false)
                .opprettetDato(new Date())
                .endretDato(new Date())
                .stillingFraNavData(stillingFraNavData)
                .build();
    }

    private static KontaktpersonData getKontaktInfo(KontaktInfo kontaktInfo) {
        if (kontaktInfo == null) {
            return null;
        }
        return KontaktpersonData.builder()
                .navn(kontaktInfo.getNavn())
                .tittel(kontaktInfo.getTittel())
                .mobil(kontaktInfo.getMobil())
                .build();
    }
}
