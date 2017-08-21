package simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import no.nav.sbl.gatling.login.OpenIdConnectLogin
import org.slf4j.LoggerFactory
import utils.{Helpers, RequestFilter}
import java.util.concurrent.TimeUnit

import io.gatling.core.feeder.RecordSeqFeederBuilder

import scala.concurrent.duration._
import scala.util.Random

class AktivitetSimulation extends Simulation {
  private val logger = LoggerFactory.getLogger(AktivitetSimulation.this.getClass)
  private val veiledere = csv(System.getProperty("VEILEDERE", "veiledere.csv")).circular
  private val brukere = csv(System.getProperty("BRUKERE", "brukere_t.csv")).circular
  private val aktivetTyper = Array (
    Map("aktivitettype" -> "STILLING"),
    Map("aktivitettype" -> "EGEN"),
    Map("aktivitettype" -> "MOTE"),
    Map("aktivitettype" -> "SAMTALEREFERAT"),
    Map("aktivitettype" -> "SOKEAVTALE"),
    Map("aktivitettype" -> "IJOBB")).circular

  private val livslopsStatuser = Array (
    Map("livslopsStatus" -> "BRUKER_ER_INTERESSERT"),
    Map("livslopsStatus" -> "PLANLAGT"),
    Map("livslopsStatus" -> "GJENNOMFORES")).circular

  private val kanaler = Array (
    Map("kanal" -> "OPPMOTE"),
    Map("kanal" -> "TELEFON"),
    Map("kanal" -> "INTERNETT")).circular

  private val usersPerSecEnhet = Integer.getInteger("USERS_PER_SEC",3).toInt
  private val duration = Integer.getInteger("DURATION", 10).toInt
  private val baseUrl = System.getProperty("BASEURL", "https://app-t6.adeo.no")
  private val loginUrl = System.getProperty("LOGINURL", "https://isso-t.adeo.no")
  private val password = "odigM001"
  private val oidcPassword = "0987654321"
  private val enheter = System.getProperty("ENHETER", "1001").split(",") //Norge største enhet Nav Kristiansand

  private val appnavn = "veilarbpersonflatefs"
  private val openIdConnectLogin = new OpenIdConnectLogin("OIDC", oidcPassword, loginUrl, baseUrl, appnavn)
  private val random = new Random()


  private val httpProtocol = http
    .baseURL(baseUrl)
    .inferHtmlResources()
    .acceptHeader("image/png,image/*;q=0.8,*/*;q=0.5")
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("nb-no,nb;q=0.9,no-no;q=0.8,no;q=0.6,nn-no;q=0.5,nn;q=0.4,en-us;q=0.3,en;q=0.1")
    .contentTypeHeader(HttpHeaderValues.ApplicationJson)
    .userAgentHeader("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:30.0) Gecko/20100101 Firefox/30.0")
    .disableWarmUp
    .silentResources

  private val personflateScenario = scenario("Veileder åpner Personflate / Aktivitetsplan")
    .feed(veiledere)
    .feed(brukere)
    .exec(addCookie(Cookie("ID_token", session => openIdConnectLogin.getIssoToken(session("username").as[String], password))))
    .exec(addCookie(Cookie("refresh_token", session => openIdConnectLogin.getRefreshToken(session("username").as[String], password))))
    .exec(Helpers.httpGetSuccess("forside personflate", "/veilarbpersonflatefs"))
    .pause("50", "600", TimeUnit.MILLISECONDS)
    .exec(Helpers.httpGetSuccess("tekster personflate", "/veilarbpersonfs/tjenester/tekster?lang=nb"))
    .exec(Helpers.httpGetSuccess("me", "/veilarbsituasjon/api/situasjon/me"))
    .exec(Helpers.httpGetSuccess("hent situasjon", session => s"/veilarbsituasjon/api/situasjon?fnr=${session("user").as[String]}"))
    .exec(Helpers.httpGetSuccess("hent persondetaljer", session => s"/veilarbperson/api/person/${session("user").as[String]}"))
    .exec(Helpers.httpGetSuccess("hent dialog", session => s"/veilarbdialog/api/dialog?fnr=${session("user").as[String]}"))
    .exec(Helpers.httpGetSuccess("hent arbeidsliste", session => s"/veilarbportefolje/tjenester/arbeidsliste/${session("user").as[String]}"))
    .exec(Helpers.httpGetSuccess("hent aktiviteter", session => s"/veilarbaktivitet/api/aktivitet?fnr=${session("user").as[String]}"))
    .exec(Helpers.httpGetSuccess("hent arena-aktiviteter", session => s"/veilarbaktivitet/api/aktivitet/arena?fnr=${session("user").as[String]}"))
    .exec(Helpers.httpGetSuccess("henter maal", session => s"/veilarbsituasjon/api/situasjon/mal?fnr=${session("user").as[String]}"))
    .exec(Helpers.httpPost("endrer maal til bruker", session => s"/veilarbsituasjon/api/situasjon/mal?fnr=${session("user").as[String]}")
      .body(StringBody("{\"mal\":\"Lager et nytt maal\"}")).asJSON
    )
    .exec(Helpers.httpGetSuccess("henter maal-historikk", session => s"/veilarbsituasjon/api/situasjon/malListe?fnr=${session("user").as[String]}"))
    .exec(Helpers.httpGetSuccess("hent vilkaar", session => s"/veilarbsituasjon/api/situasjon/hentVilkaarStatusListe?fnr=${session("user").as[String]}"))

  private val regAktivitetScenario = scenario("Veileder oppretter aktiviteter og gjør endringer på dem")
    .feed(veiledere)
    .feed(brukere)
    .feed(aktivetTyper)
    .feed(livslopsStatuser)
    .feed(kanaler)
    .exec(addCookie(Cookie("ID_token", session => openIdConnectLogin.getIssoToken(session("username").as[String], password))))
    .exec(addCookie(Cookie("refresh_token", session => openIdConnectLogin.getRefreshToken(session("username").as[String], password))))
    .exec(Helpers.httpGetSuccess("forside personflate", "/veilarbpersonflatefs"))
    .exec(Helpers.httpGetSuccess("forside personflate", session => s"/veilarbaktivitet/api/aktivitet/kanaler?fnr=${session("user").as[String]}"))
    .pause("50", "600", TimeUnit.MILLISECONDS)
    .exec(
      Helpers.httpPost("registrer aktivitet", session => s"/veilarbaktivitet/api/aktivitet/ny?fnr=${session("user").as[String]}")
        .body(ElFileBody("domain/aktivitet.json"))
        .check(regex(".*").saveAs("responseJson"))
        .check(regex("\"id\":\"(.*?)\"").saveAs("aktivitet_id"))
    )
    .exec(
      Helpers.httpGetSuccess("hent nylig lagret aktivitet", session => s"/veilarbaktivitet/api/aktivitet/${session("aktivitet_id").as[String]}?fnr=${session("user").as[String]}")
      .check(regex("\"beskrivelse\":\"${user}\""))
    )
    .exec(
      Helpers.httpPut("kaller endre-aktivitet-endepunkt", session => s"/veilarbaktivitet/api/aktivitet/${session("aktivitet_id").as[String]}?fnr=${session("user").as[String]}")
      .body(StringBody("""${responseJson}""")).asJSON
    )
    .exec(
      Helpers.httpPut("kaller endre-status-endepunkt", session => s"/veilarbaktivitet/api/aktivitet/${session("aktivitet_id").as[String]}/status?fnr=${session("user").as[String]}")
      .body(StringBody("""${responseJson}""")).asJSON
    )
    .exec(
      Helpers.httpGetSuccess("kaller versjoner(historikk)-endepunkt", session => s"/veilarbaktivitet/api/aktivitet/${session("aktivitet_id").as[String]}/versjoner?fnr=${session("user").as[String]}"))

  setUp(
    personflateScenario.inject(constantUsersPerSec(usersPerSecEnhet) during duration.minutes),
    regAktivitetScenario.inject(constantUsersPerSec(usersPerSecEnhet) during (duration seconds))
  )
    .protocols(httpProtocol)
    .assertions(global.successfulRequests.percent.gte(99))
}
