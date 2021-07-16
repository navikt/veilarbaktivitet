package no.nav.veilarbaktivitet.stilling_fra_nav;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukernotifikasjonService;
import no.nav.veilarbaktivitet.brukernotifikasjon.Varseltype;
import no.nav.veilarbaktivitet.domain.*;
import no.nav.veilarbaktivitet.kvp.KvpService;
import no.nav.veilarbaktivitet.manuell_status.v2.ManuellStatusV2Client;
import no.nav.veilarbaktivitet.manuell_status.v2.ManuellStatusV2DTO;
import no.nav.veilarbaktivitet.nivaa4.Nivaa4Client;
import no.nav.veilarbaktivitet.nivaa4.Nivaa4DTO;
import no.nav.veilarbaktivitet.oppfolging.v2.OppfolgingV2Client;
import no.nav.veilarbaktivitet.oppfolging.v2.OppfolgingV2UnderOppfolgingDTO;
import no.nav.veilarbaktivitet.service.AktivitetService;
import no.nav.veilarbaktivitet.stilling_fra_nav.deling_av_cv.Arbeidssted;
import no.nav.veilarbaktivitet.stilling_fra_nav.deling_av_cv.ForesporselOmDelingAvCv;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static no.nav.veilarbaktivitet.manuell_status.v2.ManuellStatusV2DTO.*;

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

    @Transactional
    @KafkaListener(topics = "${topic.inn.stillingFraNav}")
    @Transactional
    public void createAktivitet(ForesporselOmDelingAvCv melding) {
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

        Optional<ManuellStatusV2DTO> manuellStatusResponse = manuellStatusClient.get(aktorId);
        Optional<Nivaa4DTO> nivaa4DTO = nivaa4Client.get(aktorId);
        Optional<OppfolgingV2UnderOppfolgingDTO> oppfolgingResponse = oppfolgingClient.getUnderoppfolging(aktorId);

        boolean underKvp = kvpService.erUnderKvp(aktorId);
        boolean underOppfolging = oppfolgingResponse.map(OppfolgingV2UnderOppfolgingDTO::isErUnderOppfolging).orElse(false);
        boolean erManuell = manuellStatusResponse.map(ManuellStatusV2DTO::isErUnderManuellOppfolging).orElse(true);
        boolean erReservertIKrr = manuellStatusResponse.map(ManuellStatusV2DTO::getKrrStatus).map(KrrStatus::isErReservert).orElse(true);
        boolean harBruktNivaa4 = nivaa4DTO.map(Nivaa4DTO::isHarbruktnivaa4).orElse(false);

        AktivitetData aktivitetData = map(melding);

        if (!underOppfolging || underKvp) {
            producerClient.sendIkkeOpprettet(aktivitetData);
            return;
        }

        Person.NavIdent navIdent = Person.navIdent(melding.getOpprettetAv());

        AktivitetData aktivitet = aktivitetService.opprettAktivitet(aktorId, aktivitetData, navIdent);

        if (erManuell || erReservertIKrr || !harBruktNivaa4) {
            producerClient.sendOpprettetIkkeVarslet(aktivitet);
        } else {
            brukernotifikasjonService.opprettOppgavePaaAktivitet(aktivitet.getId(), aktivitet.getVersjon(), aktorId, "TODO tekst", Varseltype.stilling_fra_nav); //TODO finn riktig tekst
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
