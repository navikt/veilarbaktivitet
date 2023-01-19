package no.nav.veilarbaktivitet.config;

import no.nav.common.health.HealthCheckResult;
import no.nav.common.health.selftest.SelfTestChecks;
import no.nav.common.health.selftest.SelfTestUtils;
import no.nav.common.health.selftest.SelftTestCheckResult;
import no.nav.common.health.selftest.SelftestHtmlGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static no.nav.common.health.selftest.SelfTestUtils.checkAllParallel;

@Profile("!dev")
@RestController
@RequestMapping("/internal")
public class InternalController {

    private final JdbcTemplate db;
    private final SelfTestChecks selftestChecks;

    @Autowired
    public InternalController(JdbcTemplate db, SelfTestChecks selftestChecks) {
        this.db = db;
        this.selftestChecks = selftestChecks;
    }

    @GetMapping("/isReady")
    public void isReady() {
    }

    @GetMapping("/isAlive")
    public void isAlive() {
        if(checkDbHealth(db).isUnhealthy()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/selftest")
    public ResponseEntity<String> selftest() {
        List<SelftTestCheckResult> checkResults = checkAllParallel(selftestChecks.getSelfTestChecks());
        String html = SelftestHtmlGenerator.generate(checkResults);
        int status = SelfTestUtils.findHttpStatusCode(checkResults, true);

        return ResponseEntity
                .status(status)
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }

    public static HealthCheckResult checkDbHealth(JdbcTemplate db) {
        try {
            db.query("SELECT 1 FROM DUAL", resultSet -> {});
            return HealthCheckResult.healthy();
        } catch (Exception e) {
            return HealthCheckResult.unhealthy("Fikk ikke kontakt med databasen", e);
        }
    }

}
