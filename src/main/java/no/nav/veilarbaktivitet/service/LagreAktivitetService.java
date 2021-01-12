package no.nav.veilarbaktivitet.service;

import lombok.AllArgsConstructor;
import no.nav.common.types.feil.VersjonsKonflikt;
import no.nav.veilarbaktivitet.db.dao.AktivitetDAO;
import no.nav.veilarbaktivitet.domain.AktivitetData;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@AllArgsConstructor
public class LagreAktivitetService {
    private final AktivitetDAO aktivitetDAO;

    @Transactional
    public void lagreAktivitet(AktivitetData aktivitetData) {
        try {
            aktivitetDAO.insertAktivitet(aktivitetData);
        } catch (DuplicateKeyException e) {
            throw new VersjonsKonflikt();
        }
    }
}
