package no.nav.veilarbaktivitet.veilarbportefolje.gammel;

import io.micrometer.core.annotation.Timed;
import lombok.AllArgsConstructor;
import no.nav.veilarbaktivitet.config.database.Database;
import no.nav.veilarbaktivitet.veilarbportefolje.KafkaAktivitetDAO;
import no.nav.veilarbaktivitet.veilarbportefolje.KafkaAktivitetMeldingV4;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@AllArgsConstructor
public class GamelKakfaAktivietDao {
    private final Database database;

    @Timed
    public List<KafkaAktivitetMeldingV4> hentOppTil5000MeldingerSomIkkeErSendtOnprem() {
        // language=sql
        return database.query("" +
                        " SELECT *" +
                        " FROM AKTIVITET" +
                        " where PORTEFOLJE_KAFKA_OFFSET IS NULL" +
                        " order by VERSJON " +
                        " FETCH NEXT 5000 ROWS ONLY",
                KafkaAktivitetDAO::mapKafkaAktivitetMeldingV4
        );
    }

    @Timed
    public void updateSendtPaKafkaOnprem(Long versjon, Long kafkaOffset) {
        // language=sql
        database.update("" +
                        " update AKTIVITET " +
                        " set PORTEFOLJE_KAFKA_OFFSET = ?" +
                        " where VERSJON = ?",
                kafkaOffset, versjon);
    }
}
