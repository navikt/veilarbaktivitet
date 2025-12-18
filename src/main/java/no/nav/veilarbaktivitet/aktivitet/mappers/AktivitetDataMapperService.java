package no.nav.veilarbaktivitet.aktivitet.mappers;

import lombok.RequiredArgsConstructor;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.types.identer.*;
import no.nav.poao.dab.spring_auth.IAuthService;
import no.nav.veilarbaktivitet.aktivitet.domain.*;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.kvp.KvpService;
import no.nav.veilarbaktivitet.person.Innsender;
import no.nav.veilarbaktivitet.person.UserInContext;
import no.nav.veilarbaktivitet.util.DateUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class AktivitetDataMapperService {

    private final IAuthService authService;
    private final AktorOppslagClient aktorOppslagClient;
    private final UserInContext userInContext;
    private final KvpService kvpService;

    private String getEndretAv(Id bruker) {
        if (bruker instanceof AktorId) return bruker.get();
        if (bruker instanceof NavIdent) return bruker.get();
        if (bruker instanceof Fnr fnr) {
            return aktorOppslagClient.hentAktorId(fnr).get();
        }
        throw new IllegalArgumentException("Bruker må være AktorId, NavIdent eller Fnr");
    }

    public AktivitetData mapTilAktivitetData(AktivitetDTO aktivitetDTO) {
        final var id = Optional.ofNullable(aktivitetDTO.getId())
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .orElse(null);
        final var versjon = Optional.ofNullable(aktivitetDTO.getVersjon()).map(Long::parseLong).orElse(0L);
        final var aktivitetType = Helpers.Type.getData(aktivitetDTO.getType());
        final var innloggetBruker = authService.getLoggedInnUser();
        final var endretAvType = innloggetBruker instanceof EksternBrukerId ? Innsender.BRUKER : Innsender.NAV;
        final var endretAv = getEndretAv(innloggetBruker);
        final var aktorId = userInContext.getAktorId();
        var kontorSperreEnhet = kvpService.getKontorSperreEnhet(aktorId);

        final var aktivitetData = AktivitetData
                .builder()
                .id(id)
                .aktorId(aktorId)
                .endretAv(endretAv)
                .endretAvType(endretAvType)
                .endretDato(DateUtils.localDateTimeToDate(LocalDateTime.now()))
                .opprettetDato(id == null ? DateUtils.localDateTimeToDate(LocalDateTime.now()) : null)
                .versjon(versjon)
                .tittel(aktivitetDTO.getTittel())
                .fraDato(aktivitetDTO.getFraDato())
                .tilDato(aktivitetDTO.getTilDato())
                .beskrivelse(aktivitetDTO.getBeskrivelse())
                .aktivitetType(aktivitetType)
                .status(aktivitetDTO.getStatus())
                .avsluttetKommentar(aktivitetDTO.getAvsluttetKommentar())
                .avtalt(aktivitetDTO.isAvtalt())
                .lenke(aktivitetDTO.getLenke())
                .malid(aktivitetDTO.getMalid())
                .oppfolgingsperiodeId(aktivitetDTO.getOppfolgingsperiodeId())
                .kontorsperreEnhetId(kontorSperreEnhet.map(Id::get).orElse(null));

        switch (aktivitetType) {
            case EGENAKTIVITET -> aktivitetData.egenAktivitetData(egenAktivitetData(aktivitetDTO));
            case JOBBSOEKING -> aktivitetData.stillingsSoekAktivitetData(stillingsoekAktivitetData(aktivitetDTO));
            case SOKEAVTALE -> aktivitetData.sokeAvtaleAktivitetData(sokeAvtaleAktivitetData(aktivitetDTO)
            );
            case IJOBB -> aktivitetData.iJobbAktivitetData(iJobbAktivitetData(aktivitetDTO));
            case BEHANDLING -> aktivitetData.behandlingAktivitetData(behandlingAktivitetData(aktivitetDTO));
            case MOTE, SAMTALEREFERAT -> aktivitetData.moteData(moteData(aktivitetDTO));
            case STILLING_FRA_NAV -> aktivitetData.stillingFraNavData(aktivitetDTO.getStillingFraNavData());
            default -> throw new IllegalStateException("Unexpected value: " + aktivitetType);
        }

        return aktivitetData.build();
    }

    private EgenAktivitetData egenAktivitetData(AktivitetDTO aktivitetDTO) {
        return EgenAktivitetData.builder()
                .hensikt(aktivitetDTO.getHensikt())
                .oppfolging(aktivitetDTO.getOppfolging())
                .build();
    }

    private StillingsoekAktivitetData stillingsoekAktivitetData(AktivitetDTO aktivitetDTO) {
        return StillingsoekAktivitetData.builder()
                .stillingsoekEtikett(Helpers.Etikett.getData(aktivitetDTO.getEtikett()))
                .kontaktPerson(aktivitetDTO.getKontaktperson())
                .arbeidsgiver(aktivitetDTO.getArbeidsgiver())
                .arbeidssted(aktivitetDTO.getArbeidssted())
                .stillingsTittel(aktivitetDTO.getStillingsTittel())
                .build();
    }

    private SokeAvtaleAktivitetData sokeAvtaleAktivitetData(AktivitetDTO aktivitetDTO) {
        return SokeAvtaleAktivitetData.builder()
                .antallStillingerSokes(aktivitetDTO.getAntallStillingerSokes())
                .antallStillingerIUken(aktivitetDTO.getAntallStillingerIUken())
                .avtaleOppfolging(aktivitetDTO.getAvtaleOppfolging())
                .build();
    }

    private IJobbAktivitetData iJobbAktivitetData(AktivitetDTO aktivitetDTO) {
        return IJobbAktivitetData.builder()
                .jobbStatusType(Helpers.JobbStatus.getData(aktivitetDTO.getJobbStatus()))
                .ansettelsesforhold(aktivitetDTO.getAnsettelsesforhold())
                .arbeidstid(aktivitetDTO.getArbeidstid())
                .build();
    }

    private BehandlingAktivitetData behandlingAktivitetData(AktivitetDTO aktivitetDTO) {
        return BehandlingAktivitetData.builder()
                .behandlingType(aktivitetDTO.getBehandlingType())
                .behandlingSted(aktivitetDTO.getBehandlingSted())
                .effekt(aktivitetDTO.getEffekt())
                .behandlingOppfolging(aktivitetDTO.getBehandlingOppfolging())
                .build();
    }

    private MoteData moteData(AktivitetDTO aktivitetDTO) {
        return MoteData.builder()
                .adresse(aktivitetDTO.getAdresse())
                .forberedelser(aktivitetDTO.getForberedelser())
                .kanal(aktivitetDTO.getKanal())
                .referat(aktivitetDTO.getReferat())
                .referatPublisert(aktivitetDTO.isErReferatPublisert())
                .build();
    }
}
