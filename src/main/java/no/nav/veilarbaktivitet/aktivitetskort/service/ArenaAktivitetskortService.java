package no.nav.veilarbaktivitet.aktivitetskort.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.aktivitet.AktivitetService;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitetskort.Aktivitetskort;
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortMapper;
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.ArenaAktivitetskortBestilling;
import no.nav.veilarbaktivitet.aktivitetskort.idmapping.IdMapping;
import no.nav.veilarbaktivitet.aktivitetskort.idmapping.IdMappingDAO;
import no.nav.veilarbaktivitet.arena.model.ArenaId;
import no.nav.veilarbaktivitet.avtalt_med_nav.ForhaandsorienteringDAO;
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukerNotifikasjonDAO;
import no.nav.veilarbaktivitet.oppfolging.client.OppfolgingPeriodeMinimalDTO;
import no.nav.veilarbaktivitet.oppfolging.client.OppfolgingV2Client;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.util.DateUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@Service
public class ArenaAktivitetskortService {

    private final ForhaandsorienteringDAO forhaandsorienteringDAO;
    private final BrukerNotifikasjonDAO brukerNotifikasjonDAO;
    private final IdMappingDAO idMappingDAO;
    private final AktivitetService aktivitetService;
    private final OppfolgingV2Client oppfolgingV2Client;

    public AktivitetData opprettAktivitet(ArenaAktivitetskortBestilling bestilling) {
        // Opprett som vanlig
        var enrichedArenaBestilling = bestilling
            .withOppfolgingsPeriode(
                hentOppfolgingsperiode(bestilling.getAktorId(), bestilling.getAktivitetskort().getEndretTidspunkt()).orElse(null)
            );

        var opprettetTidspunkt = bestilling.getAktivitetskort().getEndretTidspunkt();
        var aktivitetsData = AktivitetskortMapper.mapTilAktivitetData(enrichedArenaBestilling, opprettetTidspunkt);

        var opprettetAktivitetsData = aktivitetService.opprettAktivitet(
            enrichedArenaBestilling.getAktorId(),
            aktivitetsData,
            enrichedArenaBestilling.getAktivitetskort().getEndretAv().toPerson(),
            opprettetTidspunkt,
            enrichedArenaBestilling.getOppfolgingsPeriode()
        );
        // Gjør arena-spesifikk migrering
        arenaspesifikkMigrering(bestilling.getAktivitetskort(), aktivitetsData, bestilling.getEksternReferanseId());
        return opprettetAktivitetsData;
    }
    public Optional<UUID> hentOppfolgingsperiode(Person.AktorId person, LocalDateTime endretTidspunkt) {
        var oppfolgingsPerioder = oppfolgingV2Client
            .hentOppfolgingsperioder(person);
        var matchingOppfolginger = oppfolgingsPerioder
            .map(
                result -> result.stream().filter(oppfolgingsPeriode ->
                    oppfolgingsPeriode.getStartDato().toLocalDateTime().isBefore(endretTidspunkt) &&
                        (oppfolgingsPeriode.getSluttDato() == null || oppfolgingsPeriode.getSluttDato().toLocalDateTime().isAfter(endretTidspunkt))
                ).toList()
            ).orElse(List.of());
        if (oppfolgingsPerioder.isPresent() && matchingOppfolginger.isEmpty()) {
            log.info("Fant ikke matchende oppfølgingsperioder for aktorId {}, {}", person.get(), oppfolgingsPerioder.get());
        }
        if (matchingOppfolginger.size() > 1) {
            log.info("Fant flere matchende oppfølgingsperioder for aktorId {}, {}", person.get(), oppfolgingsPerioder.get());
            return Optional.empty();
        }
        return matchingOppfolginger.stream().findFirst()
            .map(OppfolgingPeriodeMinimalDTO::getUuid);
    }

    private void arenaspesifikkMigrering(Aktivitetskort aktivitetskort, AktivitetData opprettetAktivitet, ArenaId eksternReferanseId) {
        idMappingDAO.insert(new IdMapping(
                eksternReferanseId,
                opprettetAktivitet.getId(),
                aktivitetskort.getId()
        ));

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
    }
}
