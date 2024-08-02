package no.nav.veilarbaktivitet.stilling_fra_nav

import no.nav.veilarbaktivitet.LocalDatabaseSingleton.postgres
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import javax.sql.DataSource

class BatchTrackingDAOTest {

    private val db: DataSource = postgres
    private val namedParameterJdbcTemplate = NamedParameterJdbcTemplate(db)
    private val batchTrackingDAO = BatchTrackingDAO(namedParameterJdbcTemplate)

    @BeforeEach
    fun setup() {
        batchTrackingDAO.setSisteProsesserteVersjon(BatchJob.Deling_av_cv_avbrutt_eller_fuulfort_uten_svar, 0)
    }

    @Test
    fun should_not_increment_offset_if_nothing_is_processsed() {
        batchTrackingDAO.withOffset(BatchJob.Deling_av_cv_avbrutt_eller_fuulfort_uten_svar) { offset ->
            assertThat(offset).isEqualTo(0)
            emptyList()
        }
        batchTrackingDAO.withOffset(BatchJob.Deling_av_cv_avbrutt_eller_fuulfort_uten_svar) { offset ->
            assertThat(offset).isEqualTo(0)
            emptyList()
        }
    }

    @Test
    fun should_set_offset_to_highest_success_result_processsed() {
        batchTrackingDAO.withOffset(BatchJob.Deling_av_cv_avbrutt_eller_fuulfort_uten_svar) { offset ->
            assertThat(offset).isEqualTo(0)
            listOf(BatchResult.Success(10), BatchResult.Success(20))
        }
        batchTrackingDAO.withOffset(BatchJob.Deling_av_cv_avbrutt_eller_fuulfort_uten_svar) { offset ->
            assertThat(offset).isEqualTo(20)
            emptyList()
        }
    }


    @Test
    fun should_set_offset_to_first_failure_result_processsed() {
        batchTrackingDAO.withOffset(BatchJob.Deling_av_cv_avbrutt_eller_fuulfort_uten_svar) { offset ->
            assertThat(offset).isEqualTo(0)
            listOf(BatchResult.Failure(10), BatchResult.Failure(2), BatchResult.Success(20))
        }
        batchTrackingDAO.withOffset(BatchJob.Deling_av_cv_avbrutt_eller_fuulfort_uten_svar) { offset ->
            assertThat(offset).isEqualTo(1)
            emptyList()
        }
    }

}