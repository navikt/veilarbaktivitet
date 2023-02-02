package no.nav.veilarbaktivitet.aktivitetskort.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.aktivitet.AktivitetService;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetIdMappingProducer;
import no.nav.veilarbaktivitet.aktivitetskort.Aktivitetskort;
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.ArenaAktivitetskortBestilling;
import no.nav.veilarbaktivitet.aktivitetskort.idmapping.IdMapping;
import no.nav.veilarbaktivitet.aktivitetskort.idmapping.IdMappingDAO;
import no.nav.veilarbaktivitet.arena.model.ArenaId;
import no.nav.veilarbaktivitet.avtalt_med_nav.ForhaandsorienteringDAO;
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukerNotifikasjonDAO;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.util.DateUtils;
import org.springframework.stereotype.Service;

import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
@Service
public class ArenaAktivitetskortService {

    private final ForhaandsorienteringDAO forhaandsorienteringDAO;
    private final BrukerNotifikasjonDAO brukerNotifikasjonDAO;
    private final IdMappingDAO idMappingDAO;
    private final AktivitetService aktivitetService;
    private final OppfolgingsperiodeService oppfolgingsperiodeService;

    private final AktivitetIdMappingProducer aktivitetIdMappingProducer;

    public AktivitetData opprettAktivitet(ArenaAktivitetskortBestilling bestilling) {
        var aktorId = bestilling.getAktorId();
        var opprettetTidspunkt = bestilling.getAktivitetskort().getEndretTidspunkt().toLocalDateTime();
        var endretAv = bestilling.getAktivitetskort().getEndretAv();

        var oppfolgingsperiode = oppfolgingsperiodeService.finnOppfolgingsperiode(aktorId, opprettetTidspunkt);

        if (oppfolgingsperiode == null) {
            // Fant ingen passende oppfølgingsperiode - ignorerer meldingen
            return null;
        }

        // Opprett via AktivitetService
        var aktivitetsData = bestilling.toAktivitet();
        if (oppfolgingsperiode.sluttDato() != null) {
            aktivitetsData.getEksternAktivitetData().setOpprettetSomHistorisk(true);
            aktivitetsData.getEksternAktivitetData().setOppfolgingsperiodeSlutt(oppfolgingsperiode.sluttDato().toLocalDateTime());
        }
        var opprettetAktivitetsData = aktivitetService.opprettAktivitet(
            aktorId,
            aktivitetsData,
            endretAv,
            opprettetTidspunkt,
            oppfolgingsperiode.uuid()
        );


        // Gjør arena-spesifikk migrering
        arenaspesifikkMigrering(bestilling.getAktivitetskort(), opprettetAktivitetsData, bestilling.getEksternReferanseId());
        return opprettetAktivitetsData;
    }

    private void arenaspesifikkMigrering(Aktivitetskort aktivitetskort, AktivitetData opprettetAktivitet, ArenaId eksternReferanseId) {
        IdMapping idMapping = new IdMapping(
                eksternReferanseId,
                opprettetAktivitet.getId(),
                aktivitetskort.getId()
        );
        idMappingDAO.insert(idMapping);

        Optional.ofNullable(forhaandsorienteringDAO.getFhoForArenaAktivitet(eksternReferanseId))
            .ifPresent(fho -> {
                int updated = forhaandsorienteringDAO.leggTilTekniskId(fho.getId(), opprettetAktivitet.getId());
                if (updated == 0) return;
                aktivitetService.oppdaterAktivitet(
                    opprettetAktivitet,
                    opprettetAktivitet.withFhoId(fho.getId()),
                    Person.navIdent(fho.getOpprettetAv()),
                    DateUtils.dateToLocalDateTime(fho.getOpprettetDato())
                );
                log.debug("La til teknisk id på FHO med id={}, tekniskId={}", fho.getId(), opprettetAktivitet.getId());
            });

        // oppdater alle brukernotifikasjoner med aktivitet arena-ider
        brukerNotifikasjonDAO.updateAktivitetIdForArenaBrukernotifikasjon(opprettetAktivitet.getId(), opprettetAktivitet.getVersjon(), eksternReferanseId);
        // Send idmapping til dialog
        aktivitetIdMappingProducer.publishAktivitetskortIdMapping(idMapping);
    }
}
