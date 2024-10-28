package no.nav.veilarbaktivitet.eventsLogger

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
@EnableScheduling
class BigQueryMetrikk(
    val bigQueryClient: BigQueryClient,
    val isPublisertDAO: IsPublisertDAO
) {

//    @Scheduled(cron = "@midnight")
    @Scheduled(cron = "0 */5 * * * *")
    @SchedulerLock(name = "aktiviteter_bigquery_metrikker", lockAtMostFor = "PT2M")
    fun hentPublisertCron() {
        val fordeling = isPublisertDAO.hentIsPublisertFordeling()
        bigQueryClient.logSamtalereferatFordeling(fordeling)
    }

}