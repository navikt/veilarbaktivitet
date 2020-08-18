package no.nav.veilarbaktivitet.controller;

import no.nav.common.health.selftest.SelfTestChecks;
import no.nav.common.health.selftest.SelfTestUtils;
import no.nav.common.health.selftest.SelftTestCheckResult;
import no.nav.common.health.selftest.SelftestHtmlGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static no.nav.common.health.selftest.SelfTestUtils.checkAllParallel;

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
        /*
        HealthCheckUtils.findFirstFailingCheck(
                selftestChecks.getSelfTestChecks().stream().
                        map(SelfTestCheck::getCheck)
                        .collect(Collectors.toList())
        ).ifPresent((failedCheck) -> {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        });
         */
    }

    @GetMapping("/isAlive")
    public void isAlive() {
    }

    @GetMapping("/selftest")
    public ResponseEntity selftest() {
        List<SelftTestCheckResult> checkResults = checkAllParallel(selftestChecks.getSelfTestChecks());
        String html = SelftestHtmlGenerator.generate(checkResults);
        int status = SelfTestUtils.findHttpStatusCode(checkResults, true);

        return ResponseEntity
                .status(status)
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }

}
