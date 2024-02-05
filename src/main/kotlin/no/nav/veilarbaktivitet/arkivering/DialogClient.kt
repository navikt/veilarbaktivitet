package no.nav.veilarbaktivitet.arkivering

import no.nav.veilarbaktivitet.aktivitetskort.graphql.OppfolgingsPeriode
import no.nav.veilarbaktivitet.person.Person.Fnr
import okhttp3.OkHttpClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.*

@Service
class DialogClient(private val dialogHttpClient: OkHttpClient) {

    @Value("\${veilarbdialog.url}")
    lateinit var dialogUrl: String;

    fun hentDialoger(oppfolgingsPeriode: OppfolgingsPeriode, fnr: Fnr) {



    }

    data class DialogDTO(
        val id: String,
        val aktivitetId: String,
        val overskrift: String,
        val oppfolgingsPeriode: UUID,
        val henvendelser: List<HenvendelseDTO>,
        val egenskaper: List<Egenskap>
    )

    data class HenvendelseDTO(
        val id: String,
        val dialogId: String,
        val avsender: Avsender,
        val avsenderId: String,
        val sendt: Date,
        val lest: Boolean,
        val viktig: Boolean,
        val tekst: String,
    )

    enum class Egenskap {
        ESKALERINGSVARSEL,
        PARAGRAF8
    }

    enum class Avsender {
        BRUKER,
        VEILEDER
    }
}