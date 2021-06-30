package no.nav.veilarbaktivitet.deling_av_cv;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class DelingAvCvService {
    private final DelingAvCvDAO delingAvCvDAO;

    public boolean aktivitetAlleredeOpprettetForBestillingsId(String bestillingsId) {
        return delingAvCvDAO.eksistererDelingAvCv(bestillingsId);
    }

}
