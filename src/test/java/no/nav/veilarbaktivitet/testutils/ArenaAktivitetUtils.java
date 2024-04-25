package no.nav.veilarbaktivitet.testutils;

import no.nav.veilarbaktivitet.arena.VeilarbarenaMapper;
import no.nav.veilarbaktivitet.arena.model.AktiviteterDTO;
import no.nav.veilarbaktivitet.arena.model.ArenaId;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;

public class ArenaAktivitetUtils {

    public static AktiviteterDTO.Tiltaksaktivitet createTiltaksaktivitet() {
        return new AktiviteterDTO.Tiltaksaktivitet()
                .setDeltakerStatus(VeilarbarenaMapper.ArenaStatus.GJENN.name())
                .setTiltaksnavn(VeilarbarenaMapper.VANLIG_AMO_NAVN)
                .setStatusSistEndret(LocalDate.now().minusYears(7))
                .setDeltakelsePeriode(
                        new AktiviteterDTO.Tiltaksaktivitet.DeltakelsesPeriode()
                                // Dette er vanlig på VASV tiltakene, starter før aktivitetplanen, slutter
                                // mange år frem i tid
                                .setFom(LocalDate.now().minusYears(7))
                                .setTom(LocalDate.now().plusYears(7))
                )
                .setAktivitetId(new ArenaId("ARENATA" + getRandomString()));
    }

    public static AktiviteterDTO.Gruppeaktivitet createGruppeaktivitet() {
        return new AktiviteterDTO.Gruppeaktivitet()
                .setMoteplanListe(List.of(
                                new AktiviteterDTO.Gruppeaktivitet.Moteplan()
                                        .setStartDato(LocalDate.ofInstant(Instant.now().minus(7, ChronoUnit.DAYS), ZoneId.systemDefault()))
                                        .setStartKlokkeslett("10:00:00")
                                        .setSluttDato(LocalDate.ofInstant(Instant.now().minus(7, ChronoUnit.DAYS), ZoneId.systemDefault()))
                                        .setSluttKlokkeslett("12:00:00")
                                ,
                                new AktiviteterDTO.Gruppeaktivitet.Moteplan()
                                        .setStartDato(LocalDate.ofInstant(Instant.now().plus(2, ChronoUnit.DAYS), ZoneId.systemDefault()))
                                        .setStartKlokkeslett("10:00:00")
                                        .setSluttDato(LocalDate.ofInstant(Instant.now().plus(2, ChronoUnit.DAYS), ZoneId.systemDefault()))
                                        .setSluttKlokkeslett("12:00:00")
                        )

                )
                .setAktivitetId(new ArenaId("ARENATA" + getRandomString()));
    }

    public static AktiviteterDTO.Utdanningsaktivitet createUtdanningsaktivitet() {
        AktiviteterDTO.Utdanningsaktivitet.AktivitetPeriode periode = new AktiviteterDTO.Utdanningsaktivitet.AktivitetPeriode()
                .setFom(LocalDate.ofInstant(Instant.now().plus(2, ChronoUnit.DAYS), ZoneId.systemDefault()))
                .setTom(LocalDate.ofInstant(Instant.now().plus(4, ChronoUnit.DAYS), ZoneId.systemDefault()));

        return new AktiviteterDTO.Utdanningsaktivitet()
                .setAktivitetId(new ArenaId("ARENAUA" + getRandomString()))
                .setAktivitetPeriode(periode);
    }

    private static String getRandomString() {
        return String.valueOf(new Random().nextInt());
    }
}
