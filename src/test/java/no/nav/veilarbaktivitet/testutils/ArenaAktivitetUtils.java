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

    public static AktiviteterDTO.Tiltaksaktivitet createTiltaksaktivitet(LocalDate startDatoOppfølging) {
        return new AktiviteterDTO.Tiltaksaktivitet()
                .setDeltakerStatus(VeilarbarenaMapper.ArenaStatusDTO.GJENN.name())
                .setTiltaksnavn(VeilarbarenaMapper.VANLIG_AMO_NAVN)
                .setStatusSistEndret(startDatoOppfølging.plusDays(1))
                .setDeltakelsePeriode(
                        new AktiviteterDTO.Tiltaksaktivitet.DeltakelsesPeriode()
                                .setFom(startDatoOppfølging.plusDays(1))
                                .setTom(startDatoOppfølging.plusYears(25))
                )
                .setAktivitetId(new ArenaId("ARENATA" + getRandomString()));
    }

    public static AktiviteterDTO.Gruppeaktivitet createGruppeaktivitet(LocalDate startDatoOppfølging) {
        return new AktiviteterDTO.Gruppeaktivitet()
                .setMoteplanListe(List.of(
                                new AktiviteterDTO.Gruppeaktivitet.Moteplan()
                                        .setStartDato(startDatoOppfølging)
                                        .setStartKlokkeslett("10:00:00")
                                        .setSluttDato(startDatoOppfølging)
                                        .setSluttKlokkeslett("12:00:00")
                                ,
                                new AktiviteterDTO.Gruppeaktivitet.Moteplan()
                                        .setStartDato(LocalDate.ofInstant(Instant.now().plus(2, ChronoUnit.DAYS), ZoneId.systemDefault()))
                                        .setStartKlokkeslett("10:00:00")
                                        .setSluttDato(LocalDate.ofInstant(Instant.now().plus(2, ChronoUnit.DAYS), ZoneId.systemDefault()))
                                        .setSluttKlokkeslett("12:00:00")
                        )

                )
                .setAktivitetId(new ArenaId("ARENAGA" + getRandomString()));
    }

    public static AktiviteterDTO.Utdanningsaktivitet createUtdanningsaktivitet(LocalDate startDatoOppfølging) {
        AktiviteterDTO.Utdanningsaktivitet.AktivitetPeriode periode = new AktiviteterDTO.Utdanningsaktivitet.AktivitetPeriode()
                .setFom(startDatoOppfølging.plusDays(2))
                .setTom(startDatoOppfølging.plusDays(4));

        return new AktiviteterDTO.Utdanningsaktivitet()
                .setAktivitetId(new ArenaId("ARENAUA" + getRandomString()))
                .setAktivitetPeriode(periode);
    }

    private static String getRandomString() {
        return String.valueOf(new Random().nextInt());
    }
}
