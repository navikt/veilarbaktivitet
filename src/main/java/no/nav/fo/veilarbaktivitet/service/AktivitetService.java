package no.nav.fo.veilarbaktivitet.service;

import lombok.val;
import no.nav.apiapp.feil.Feil;
import no.nav.apiapp.feil.FeilType;
import no.nav.apiapp.feil.VersjonsKonflikt;
import no.nav.fo.veilarbaktivitet.client.KvpClient;
import no.nav.fo.veilarbaktivitet.db.dao.AktivitetDAO;
import no.nav.fo.veilarbaktivitet.domain.*;
import no.nav.fo.veilarbaktivitet.kafka.KafkaService;
import no.nav.fo.veilarbaktivitet.util.FunksjonelleMetrikker;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.InternalServerErrorException;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static java.util.Optional.ofNullable;
import static no.nav.apiapp.util.StringUtils.nullOrEmpty;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetTransaksjonsType.*;
import static no.nav.fo.veilarbaktivitet.kafka.KafkaAktivitetMelding.of;
import static no.nav.fo.veilarbaktivitet.util.MappingUtils.merge;

@Component
public class AktivitetService {
    private final AktivitetDAO aktivitetDAO;
    private final KvpClient kvpClient;
    private final KafkaService kafkaService;
    private final UnleashService unleash;

    @Inject
    public AktivitetService(AktivitetDAO aktivitetDAO, KvpClient kvpClient, KafkaService kafkaService, UnleashService unleash) {
        this.aktivitetDAO = aktivitetDAO;
        this.kvpClient = kvpClient;
        this.kafkaService = kafkaService;
        this.unleash = unleash;
    }

    public List<AktivitetData> hentAktiviteterForAktorId(Person.AktorId aktorId) {
        return aktivitetDAO.hentAktiviteterForAktorId(aktorId);
    }

    public AktivitetData hentAktivitet(long id) {
        return aktivitetDAO.hentAktivitet(id);
    }

    public List<AktivitetData> hentAktivitetVersjoner(long id) {
        return aktivitetDAO.hentAktivitetVersjoner(id);
    }

    /**
     * Returnerer en kopi av AktivitetData-objektet hvor kontorsperreEnhetId
     * er satt dersom brukeren er under KVP.
     */
    private AktivitetData tagUsingKVP(AktivitetData a) {
        KvpDTO kvp;

        try {
            kvp = kvpClient.get(Person.aktorId(a.getAktorId()));
        } catch (ForbiddenException e) {
            throw new Feil(FeilType.UKJENT, "veilarbaktivitet har ikke tilgang til å spørre om KVP-status.");
        } catch (InternalServerErrorException e) {
            throw new Feil(FeilType.UKJENT, "veilarboppfolging har en intern bug, vennligst fiks applikasjonen.");
        }

        return Optional.ofNullable(kvp)
                .map(k -> a.toBuilder().kontorsperreEnhetId(k.getEnhet()).build())
                .orElse(a);
    }

    public long opprettAktivitet(Person.AktorId aktorId, AktivitetData aktivitet, Person endretAvPerson) {
        String endretAv = Optional.ofNullable(endretAvPerson).map(Person::get).orElse(null);
        long aktivitetId = aktivitetDAO.getNextUniqueAktivitetId();
        AktivitetData nyAktivivitet = aktivitet
                .toBuilder()
                .id(aktivitetId)
                .aktorId(aktorId.get())
                .lagtInnAv(aktivitet.getLagtInnAv())
                .transaksjonsType(OPPRETTET)
                .opprettetDato(new Date())
                .endretAv(endretAv)
                .automatiskOpprettet(aktivitet.isAutomatiskOpprettet())
                .build();

        AktivitetData kvpAktivivitet = tagUsingKVP(nyAktivivitet);
        lagreAktivitet(kvpAktivivitet);

        FunksjonelleMetrikker.opprettNyAktivitetMetrikk(aktivitet);
        return aktivitetId;
    }


    public void oppdaterStatus(AktivitetData originalAktivitet, AktivitetData aktivitet, Person endretAv) {
        val nyAktivitet = originalAktivitet
                .toBuilder()
                .status(aktivitet.getStatus())
                .lagtInnAv(aktivitet.getLagtInnAv())
                .avsluttetKommentar(aktivitet.getAvsluttetKommentar())
                .transaksjonsType(AktivitetTransaksjonsType.STATUS_ENDRET)
                .endretAv(endretAv != null ? endretAv.get() : null)
                .build();

        lagreAktivitet(nyAktivitet);
    }

    public void oppdaterEtikett(AktivitetData originalAktivitet, AktivitetData aktivitet, Person endretAv) {
        val nyEtikett = aktivitet.getStillingsSoekAktivitetData().getStillingsoekEtikett();

        val originalStillingsAktivitet = originalAktivitet.getStillingsSoekAktivitetData();
        val nyStillingsAktivitet = originalStillingsAktivitet.withStillingsoekEtikett(nyEtikett);

        val nyAktivitet = originalAktivitet
                .toBuilder()
                .lagtInnAv(aktivitet.getLagtInnAv())
                .stillingsSoekAktivitetData(nyStillingsAktivitet)
                .transaksjonsType(AktivitetTransaksjonsType.ETIKETT_ENDRET)
                .endretAv(endretAv != null ? endretAv.get() : null)
                .build();

        lagreAktivitet(nyAktivitet);
    }

    public void slettAktivitet(long aktivitetId) {
        aktivitetDAO.slettAktivitet(aktivitetId);
    }

    public void oppdaterAktivitetFrist(AktivitetData originalAktivitet, AktivitetData aktivitetData, Person endretAv) {
        val oppdatertAktivitetMedNyFrist = originalAktivitet
                .toBuilder()
                .lagtInnAv(aktivitetData.getLagtInnAv())
                .transaksjonsType(AktivitetTransaksjonsType.AVTALT_DATO_ENDRET)
                .tilDato(aktivitetData.getTilDato())
                .endretAv(endretAv != null ? endretAv.get() : null)
                .build();
        lagreAktivitet(oppdatertAktivitetMedNyFrist);
    }

    public void oppdaterMoteTidOgSted(AktivitetData originalAktivitet, AktivitetData aktivitetData, Person endretAv) {
        val oppdatertAktivitetMedNyFrist = originalAktivitet
                .toBuilder()
                .lagtInnAv(aktivitetData.getLagtInnAv())
                .transaksjonsType(AktivitetTransaksjonsType.MOTE_TID_OG_STED_ENDRET )
                .fraDato(aktivitetData.getFraDato())
                .tilDato(aktivitetData.getTilDato())
                .moteData(ofNullable(originalAktivitet.getMoteData()).map(d -> d.withAdresse(aktivitetData.getMoteData().getAdresse())).orElse(null))
                .endretAv(endretAv != null ? endretAv.get() : null)
                .build();
        lagreAktivitet(oppdatertAktivitetMedNyFrist);
    }

    public void oppdaterReferat(
            AktivitetData originalAktivitet,
            AktivitetData aktivitetData,
            Person endretAv
    ) {
        val transaksjon = getReferatTransakjsonType(originalAktivitet, aktivitetData);

        val merger = merge(originalAktivitet, aktivitetData);
        lagreAktivitet(originalAktivitet
                .withEndretAv(endretAv.get())
                .withTransaksjonsType(transaksjon)
                .withMoteData(merger.map(AktivitetData::getMoteData).merge(this::mergeReferat))
        );
    }

    private AktivitetTransaksjonsType getReferatTransakjsonType(AktivitetData originalAktivitet,
                                                                AktivitetData aktivitetData) {
        val transaksjon = nullOrEmpty(originalAktivitet.getMoteData().getReferat())
                ? REFERAT_OPPRETTET : REFERAT_ENDRET;

        if (!originalAktivitet.getMoteData().isReferatPublisert() && aktivitetData.getMoteData().isReferatPublisert()) {
            return REFERAT_PUBLISERT;
        }
        return transaksjon;
    }

    private MoteData mergeReferat(MoteData originalMoteData, MoteData moteData) {
        return originalMoteData
                .withReferat(moteData.getReferat())
                .withReferatPublisert(moteData.isReferatPublisert());
    }

    public void oppdaterAktivitet(AktivitetData originalAktivitet, AktivitetData aktivitet, Person endretAv) {
        val blittAvtalt = originalAktivitet.isAvtalt() != aktivitet.isAvtalt();
        val transType = blittAvtalt ? AktivitetTransaksjonsType.AVTALT : AktivitetTransaksjonsType.DETALJER_ENDRET;

        val merger = merge(originalAktivitet, aktivitet);
        lagreAktivitet(originalAktivitet
                .toBuilder()
                .fraDato(aktivitet.getFraDato())
                .tilDato(aktivitet.getTilDato())
                .tittel(aktivitet.getTittel())
                .beskrivelse(aktivitet.getBeskrivelse())
                .lagtInnAv(aktivitet.getLagtInnAv())
                .avsluttetKommentar(aktivitet.getAvsluttetKommentar())
                .lenke(aktivitet.getLenke())
                .transaksjonsType(transType)
                .versjon(aktivitet.getVersjon())
                .endretAv(endretAv != null ? endretAv.get() : null)
                .avtalt(aktivitet.isAvtalt())
                .stillingsSoekAktivitetData(merger.map(AktivitetData::getStillingsSoekAktivitetData).merge(this::mergeStillingSok))
                .egenAktivitetData(merger.map(AktivitetData::getEgenAktivitetData).merge(this::mergeEgenAktivitetData))
                .sokeAvtaleAktivitetData(merger.map(AktivitetData::getSokeAvtaleAktivitetData).merge(this::mergeSokeAvtaleAktivitetData))
                .iJobbAktivitetData(merger.map(AktivitetData::getIJobbAktivitetData).merge(this::mergeIJobbAktivitetData))
                .behandlingAktivitetData(merger.map(AktivitetData::getBehandlingAktivitetData).merge(this::mergeBehandlingAktivitetData))
                .moteData(merger.map(AktivitetData::getMoteData).merge(this::mergeMoteData))
                .build()
        );
        FunksjonelleMetrikker.oppdaterAktivitetMetrikk(aktivitet, blittAvtalt, originalAktivitet.isAutomatiskOpprettet());
    }

    private BehandlingAktivitetData mergeBehandlingAktivitetData(BehandlingAktivitetData originalBehandlingAktivitetData, BehandlingAktivitetData behandlingAktivitetData) {
        return originalBehandlingAktivitetData
                .withBehandlingType(behandlingAktivitetData.getBehandlingType())
                .withBehandlingSted(behandlingAktivitetData.getBehandlingSted())
                .withEffekt(behandlingAktivitetData.getEffekt())
                .withBehandlingOppfolging(behandlingAktivitetData.getBehandlingOppfolging());
    }

    private IJobbAktivitetData mergeIJobbAktivitetData(IJobbAktivitetData originalIJobbAktivitetData, IJobbAktivitetData iJobbAktivitetData) {
        return originalIJobbAktivitetData
                .withJobbStatusType(iJobbAktivitetData.getJobbStatusType())
                .withAnsettelsesforhold(iJobbAktivitetData.getAnsettelsesforhold())
                .withArbeidstid(iJobbAktivitetData.getArbeidstid());
    }

    private SokeAvtaleAktivitetData mergeSokeAvtaleAktivitetData(SokeAvtaleAktivitetData originalSokeAvtaleAktivitetData, SokeAvtaleAktivitetData sokeAvtaleAktivitetData) {
        return originalSokeAvtaleAktivitetData
                .withAntallStillingerSokes(sokeAvtaleAktivitetData.getAntallStillingerSokes())
                .withAntallStillingerIUken(sokeAvtaleAktivitetData.getAntallStillingerIUken())
                .withAvtaleOppfolging(sokeAvtaleAktivitetData.getAvtaleOppfolging());
    }

    private EgenAktivitetData mergeEgenAktivitetData(EgenAktivitetData originalEgenAktivitetData, EgenAktivitetData egenAktivitetData) {
        return originalEgenAktivitetData
                .withOppfolging(egenAktivitetData.getOppfolging())
                .withHensikt(egenAktivitetData.getHensikt());
    }

    private MoteData mergeMoteData(MoteData originalMoteData, MoteData moteData) {
        // Referat-felter settes gjennom egne operasjoner, se oppdaterReferat()
        return originalMoteData
                .withAdresse(moteData.getAdresse())
                .withForberedelser(moteData.getForberedelser())
                .withKanal(moteData.getKanal());
    }

    private StillingsoekAktivitetData mergeStillingSok(StillingsoekAktivitetData originalStillingsoekAktivitetData, StillingsoekAktivitetData stillingsoekAktivitetData) {
        return originalStillingsoekAktivitetData
                .withArbeidsgiver(stillingsoekAktivitetData.getArbeidsgiver())
                .withArbeidssted(stillingsoekAktivitetData.getArbeidssted())
                .withKontaktPerson(stillingsoekAktivitetData.getKontaktPerson())
                .withStillingsTittel(stillingsoekAktivitetData.getStillingsTittel());
    }

    private void lagreAktivitet(AktivitetData aktivitetData) {
        try {
            aktivitetDAO.insertAktivitet(aktivitetData);
            if (unleash.isEnabled("veilarbaktivitet.kafka")) {
                kafkaService.sendMelding(of(aktivitetData));
            }
        } catch (DuplicateKeyException e) {
            throw new VersjonsKonflikt();
        }
    }

    @Transactional
    public void settAktiviteterTilHistoriske(Person.AktorId aktoerId, Date sluttDato) {
        hentAktiviteterForAktorId(aktoerId)
                .stream()
                .filter(a -> skalBliHistorisk(a, sluttDato))
                .map(a -> a.withTransaksjonsType(BLE_HISTORISK).withHistoriskDato(sluttDato))
                .forEach(this::lagreAktivitet);
    }

    private boolean skalBliHistorisk(AktivitetData aktivitetData, Date sluttdato) {
        return aktivitetData.getHistoriskDato() == null && aktivitetData.getOpprettetDato().before(sluttdato);
    }

    public AktivitetData settLestAvBrukerTidspunkt(Long aktivitetId) {
        aktivitetDAO.insertLestAvBrukerTidspunkt(aktivitetId);
        return hentAktivitet(aktivitetId);
    }

    @Transactional
    public void settAktiviteterInomKVPPeriodeTilAvbrutt(Person.AktorId aktoerId, String avsluttetBegrunnelse, Date avsluttetDato) {
        hentAktiviteterForAktorId(aktoerId)
                .stream()
                .filter(this::filtrerKontorSperretOgStatusErIkkeAvBruttEllerFullfort)
                .filter(aktitet -> aktitet.getOpprettetDato().before(avsluttetDato))
                .map( aktivitetData -> settKVPAktivitetTilAvbrutt(aktivitetData, avsluttetBegrunnelse, avsluttetDato))
                .forEach(this::lagreAktivitet);
    }

    private boolean filtrerKontorSperretOgStatusErIkkeAvBruttEllerFullfort(AktivitetData aktivitetData) {
        AktivitetStatus aktivitetStatus = aktivitetData.getStatus();
        return aktivitetData.getKontorsperreEnhetId() != null && !(aktivitetStatus.equals(AktivitetStatus.AVBRUTT) || aktivitetStatus.equals(AktivitetStatus.FULLFORT));
    }

    private AktivitetData settKVPAktivitetTilAvbrutt(AktivitetData aktivitetData, String avsluttetBegrunnelse, Date avsluttetDato) {
        return aktivitetData
                .withTransaksjonsType(STATUS_ENDRET)
                .withStatus(AktivitetStatus.AVBRUTT)
                .withAvsluttetKommentar(avsluttetBegrunnelse)
                .withEndretDato(avsluttetDato);
    }

}
