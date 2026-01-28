package no.nav.veilarbaktivitet.stilling_fra_nav;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.client.aktorregister.IngenGjeldendeIdentException;
import no.nav.veilarbaktivitet.aktivitet.AktivitetService;
import no.nav.veilarbaktivitet.aktivitet.MetricService;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTransaksjonsType;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData;
import no.nav.veilarbaktivitet.brukernotifikasjon.MinsideVarselService;
import no.nav.veilarbaktivitet.brukernotifikasjon.VarselType;
import no.nav.veilarbaktivitet.brukernotifikasjon.opprettVarsel.AktivitetVarsel;
import no.nav.veilarbaktivitet.kvp.KvpService;
import no.nav.veilarbaktivitet.oppfolging.periode.IngenGjeldendePeriodeException;
import no.nav.veilarbaktivitet.oppfolging.periode.SistePeriodeService;
import no.nav.veilarbaktivitet.person.Innsender;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.stilling_fra_nav.deling_av_cv.ForesporselOmDelingAvCv;
import no.nav.veilarbaktivitet.stilling_fra_nav.deling_av_cv.KontaktInfo;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;

import static no.nav.veilarbaktivitet.config.TeamLog.teamLog;
import static no.nav.veilarbaktivitet.stilling_fra_nav.DelingAvCvService.utledArbeidstedtekst;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpprettForesporselOmDelingAvCv {
    private final AktivitetService aktivitetService;
    private final DelingAvCvService delingAvCvService;
    private final KvpService kvpService;
    private final SistePeriodeService sistePeriodeService;
    private final MinsideVarselService brukernotifikasjonService;
    private final StillingFraNavProducerClient producerClient;
    private final StillingFraNavMetrikker metrikker;

    private static final String BRUKERNOTIFIKASJON_TEKST = "Vi søker etter kandidater til denne stillingen. Kan denne stillingen passe for deg?";

    @Transactional
    @KafkaListener(topics = "${topic.inn.stillingFraNav}", containerFactory = "stringAvroKafkaListenerContainerFactory", autoStartup = "${app.kafka.enabled:false}")
    public void createAktivitet(ConsumerRecord<String, ForesporselOmDelingAvCv> consumerRecord) {
        ForesporselOmDelingAvCv melding = consumerRecord.value();

        if (delingAvCvService.aktivitetAlleredeOpprettetForBestillingsId(melding.getBestillingsId())) {
            log.info("ForesporselOmDelingAvCv med bestillingsId={} har allerede en aktivitet", melding.getBestillingsId());
            return;
        }
        teamLog.info("OpprettForesporselOmDelingAvCv.createAktivitet {}", melding);
        Person.AktorId aktorId = Person.aktorId(melding.getAktorId());
        if (aktorId.get() == null) {
            log.error("OpprettForesporselOmDelingAvCv.createAktivitet AktorId=null");
        }

        boolean underOppfolging;
        try {
            sistePeriodeService.hentGjeldendeOppfolgingsperiodeMedFallback(aktorId);
            underOppfolging = true;
        } catch (IngenGjeldendeIdentException exception) {
            producerClient.sendUgyldigInput(melding.getBestillingsId(), aktorId.get(), "Finner ingen gyldig ident for aktorId");
            log.warn("*** Kan ikke behandle melding. Årsak: {} ***. Se securelogs for payload.", exception.getMessage());
            teamLog.warn("*** Kan ikke behandle melding={}. Årsak: {} ***", melding, exception.getMessage());
            return;
        } catch (IngenGjeldendePeriodeException exception) {
            underOppfolging = false;
        }

        boolean underKvp = kvpService.erUnderKvp(aktorId);

        if (!underOppfolging || underKvp) {
            producerClient.sendUgyldigOppfolgingStatus(melding.getBestillingsId(), aktorId.get());
            return;
        }
        boolean kanVarsle = brukernotifikasjonService.kanVarsles(aktorId);
        AktivitetData aktivitetData = map(melding, kanVarsle);
        MDC.put(MetricService.SOURCE, "rekrutteringsbistand");
        AktivitetData aktivitet = aktivitetService.opprettAktivitet(aktivitetData);
        MDC.clear();
        if (kanVarsle) {
            brukernotifikasjonService.opprettVarselPaaAktivitet(new AktivitetVarsel(
                aktivitet.getId(), aktivitet.getVersjon(), aktorId, BRUKERNOTIFIKASJON_TEKST, VarselType.STILLING_FRA_NAV, null, null, null
            ));
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

        String arbeidsted = utledArbeidstedtekst(melding.getArbeidssteder());

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
                .aktorId(aktorId)
                .tittel(stillingstittel)
                .aktivitetType(AktivitetTypeData.STILLING_FRA_NAV)
                .status(AktivitetStatus.BRUKER_ER_INTERESSERT)
                .transaksjonsType(AktivitetTransaksjonsType.OPPRETTET)
                .fraDato(new Date(opprettet.toEpochMilli()))
                .endretAvType(Innsender.NAV)
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
        String orginalNavn = kontaktInfo.getNavn();
        String navnUnder250 = orginalNavn == null ? null : orginalNavn.substring(0, Math.min(orginalNavn.length(), 240));
        return KontaktpersonData.builder()
                .navn(navnUnder250)
                .tittel(kontaktInfo.getTittel())
                .mobil(kontaktInfo.getMobil())
                .build();
    }
}
