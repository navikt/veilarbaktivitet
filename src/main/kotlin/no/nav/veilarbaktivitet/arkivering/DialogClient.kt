package no.nav.veilarbaktivitet.arkivering

import okhttp3.OkHttpClient
import org.springframework.stereotype.Service

@Service
class DialogClient(private val dialogHttpClient: OkHttpClient) {

}