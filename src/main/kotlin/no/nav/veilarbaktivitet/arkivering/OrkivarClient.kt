package no.nav.veilarbaktivitet.arkivering

import okhttp3.OkHttpClient
import org.springframework.stereotype.Service

@Service
class OrkivarClient(private val orkivarHttpClient: OkHttpClient) {

    fun arkiver() {

    }

}