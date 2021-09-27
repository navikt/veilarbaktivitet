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
import no.nav.veilarbaktivitet.manuell_status.v2.ManuellStatusV2Client;
import no.nav.veilarbaktivitet.manuell_status.v2.ManuellStatusV2DTO;
import no.nav.veilarbaktivitet.nivaa4.Nivaa4Client;
import no.nav.veilarbaktivitet.nivaa4.Nivaa4DTO;
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

import static no.nav.veilarbaktivitet.manuell_status.v2.ManuellStatusV2DTO.KrrStatus;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpprettForesporselOmDelingAvCv {
    private final AktivitetService aktivitetService;
    private final DelingAvCvService delingAvCvService;
    private final KvpService kvpService;
    private final OppfolgingV2Client oppfolgingClient;
    private final ManuellStatusV2Client manuellStatusClient;
    private final BrukernotifikasjonService brukernotifikasjonService;
    private final StillingFraNavProducerClient producerClient;
    private final Nivaa4Client nivaa4Client;

    private static final String BRUKERNOTIFIKASJON_TEKST = "Her en stilling som NAV tror kan passe for deg. Gi oss en tilbakemelding.";

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

        Optional<ManuellStatusV2DTO> manuellStatusResponse;
        Optional<Nivaa4DTO> nivaa4DTO;
        Optional<OppfolgingV2UnderOppfolgingDTO> oppfolgingResponse;
        try {
            manuellStatusResponse = manuellStatusClient.get(aktorId);
            nivaa4DTO = nivaa4Client.get(aktorId);
            oppfolgingResponse = oppfolgingClient.getUnderoppfolging(aktorId);
        } catch (IngenGjeldendeIdentException exception) {
            producerClient.sendUgyldigInput(melding.getBestillingsId(), aktorId.get(), "Finner ingen gydlig ident for aktorId");
            log.warn("*** Kan ikke behandle melding={}. Årsak: {} ***", melding, exception.getMessage());
            return;
        }

        boolean underKvp = kvpService.erUnderKvp(aktorId);
        boolean underOppfolging = oppfolgingResponse.map(OppfolgingV2UnderOppfolgingDTO::isErUnderOppfolging).orElse(false);
        boolean erManuell = manuellStatusResponse.map(ManuellStatusV2DTO::isErUnderManuellOppfolging).orElse(true);
        boolean erReservertIKrr = manuellStatusResponse.map(ManuellStatusV2DTO::getKrrStatus).map(KrrStatus::isErReservert).orElse(true);
        boolean harBruktNivaa4 = nivaa4DTO.map(Nivaa4DTO::isHarbruktnivaa4).orElse(false);

        AktivitetData aktivitetData = map(melding);

        if (!underOppfolging || underKvp) {
            producerClient.sendUgyldigOppfolgingStatus(melding.getBestillingsId(), aktorId.get());
            return;
        }

        Person.NavIdent navIdent = Person.navIdent(melding.getOpprettetAv());

        AktivitetData aktivitet = aktivitetService.opprettAktivitet(aktorId, aktivitetData, navIdent);

        if (erManuell || erReservertIKrr || !harBruktNivaa4) {
            producerClient.sendOpprettetIkkeVarslet(aktivitet);
        } else {
            brukernotifikasjonService.opprettOppgavePaaAktivitet(aktivitet.getId(), aktivitet.getVersjon(), aktorId, BRUKERNOTIFIKASJON_TEKST, VarselType.STILLING_FRA_NAV);
            producerClient.sendOpprettet(aktivitet);
        }
    }

    private AktivitetData map(ForesporselOmDelingAvCv melding) {
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

        KontaktInfo kontaktInfo = melding.getKontaktInfo();
        KontaktpersonData kontaktpersonData = KontaktpersonData.builder()
                .navn(kontaktInfo.getNavn())
                .tittel(kontaktInfo.getTittel())
                .mobil(kontaktInfo.getMobil())
                .epost(kontaktInfo.getEpost())
                .build();

        StillingFraNavData stillingFraNavData = StillingFraNavData
                .builder()
                .soknadsfrist(soknadsfrist)
                .svarfrist(svarfrist)
                .arbeidsgiver(arbeidsgiver)
                .bestillingsId(bestillingsId)
                .stillingsId(stillingsId)
                .arbeidssted(arbeidsted)
                .kontaktpersonData(kontaktpersonData)
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
