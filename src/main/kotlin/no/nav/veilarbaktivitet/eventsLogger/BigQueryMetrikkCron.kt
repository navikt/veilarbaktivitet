package no.nav.veilarbaktivitet.eventsLogger

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
@EnableScheduling
open class BigQueryMetrikkCron(
    val bigQueryClient: BigQueryClient,
    val isPublisertDAO: IsPublisertDAO
) {

    @Scheduled(cron = "@midnight")
    @SchedulerLock(name = "aktiviteter_bigquery_metrikker", lockAtMostFor = "PT2M")
    open fun hentPublisertCron() {
        val fordeling = isPublisertDAO.hentIsPublisertFordeling()
        bigQueryClient.logSamtalereferatFordeling(fordeling)
    }

}
