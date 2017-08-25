package simulations

import java.net.URLEncoder

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import no.nav.sbl.gatling.login.OpenIdConnectLogin
import org.slf4j.LoggerFactory
import utils.{FeedHelpers, Helpers}
import java.util.concurrent.TimeUnit

import io.gatling.core.session.Expression
import io.gatling.core.feeder.RecordSeqFeederBuilder

import scala.concurrent.duration._
import scala.util.Random

class VeilederOgFeedSimulation extends Simulation {

  private val logger = LoggerFactory.getLogger(VeilederOgFeedSimulation.this.getClass)

  ////////////////////////////
  //Variabler settes i Jenkins
  ///////////////////////////
  private val usersPerSecEnhet = Integer.getInteger("USERS_PER_SEC",1).toInt
  private val duration = Integer.getInteger("DURATION", 7100).toInt
  private val baseUrl = System.getProperty("BASEURL", "https://app-t6.adeo.no")
  private val loginUrl = System.getProperty("LOGINURL", "https://isso-t.adeo.no")
  val password = System.getProperty("VEILEDER_PASSWD", "odigM001") //Teflon3970
  val oidcPassword = System.getProperty("OIDC_PASSWD", "0987654321")
  private val feedUsername = System.getProperty("FEED_USERNAME", "srvveilarbportefolje")
  private val feedPassword = System.getProperty("FEED_PASSWORD", "APUkG8YqkzTJ9eJ")
  private val enheter = System.getProperty("ENHETER", "1001").split(",") //Norge største enhet Nav Kristiansand
  private val initialDateSituasjonsfeed =  System.getProperty("INITIAL_DATE_SITUASJONSFEED", "2017-08-21T17:24:23.882Z")
  private val initialDateDialogfeed =  System.getProperty("INITIAL_DATE_DIALOGFEED", "2017-08-21T17:24:23.882Z")
  private val initialDateAktivitetfeed =  System.getProperty("INITIAL_DATE_AKTIVITETFEED", "2017-08-21T17:24:23.882Z")
  private val feedPageSize =  System.getProperty("FEED_PAGE_SIZE", "100")


  ///////////////////////////
  //Feeders
  ///////////////////////////
  private val veiledere = csv(System.getProperty("VEILEDERE", "veileder-data/veiledere.csv")).circular
  private val brukere_for_sok = csv(System.getProperty("VEILEDER_BRUKERE", "veileder-data/brukere_for_sok.csv")).circular
  private val eksterne_brukere = csv(System.getProperty("EKSTERNE_BRUKERE", "ekstern-bruker-data/brukere.csv")).circular

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

  private val jobbstatus = Array (
    Map("jobbstatus" -> "HELTID"),
    Map("jobbstatus" -> "DELTID")).circular


  ///////////////////////////
  //Login
  ///////////////////////////
  private val appnavn = "veilarbpersonflatefs"
  private val openIdConnectLogin = new OpenIdConnectLogin("OIDC", oidcPassword, loginUrl, baseUrl, appnavn)

  private def login() = {
    exec(addCookie(Cookie("ID_token", session => openIdConnectLogin.getIssoToken(session("username").as[String], password))))
      .exec(addCookie(Cookie("refresh_token", session => openIdConnectLogin.getRefreshToken(session("username").as[String], password))))
      .pause("50", "600", TimeUnit.MILLISECONDS)
  }

  private def loginFeed() = {
    exec(addCookie(Cookie("ID_token", session => openIdConnectLogin.getIssoToken(feedUsername, feedPassword))))
      .exec(addCookie(Cookie("refresh_token", session => openIdConnectLogin.getRefreshToken(feedUsername, feedPassword))))
      .pause("50", "600", TimeUnit.MILLISECONDS)
  }

  ///////////////////////////
  //HTTP-oppsett
  ///////////////////////////
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
    .extraInfoExtractor {extraInfo => List(Helpers.getInfo(extraInfo))}


  ///////////////////////////
  //Scenarioer
  ///////////////////////////
  private val personflateScenario = scenario("Veileder aapner Personflate / Aktivitetsplan")
    .feed(veiledere)
    .feed(brukere_for_sok)
    .exec(login)
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
      .body(StringBody("{\"mal\":\"Ytelsestest - Lager et nytt maal\"}")).asJSON
    )
    .exec(Helpers.httpGetSuccess("henter maal-historikk", session => s"/veilarbsituasjon/api/situasjon/malListe?fnr=${session("user").as[String]}"))
    .exec(Helpers.httpGetSuccess("hent vilkaar", session => s"/veilarbsituasjon/api/situasjon/hentVilkaarStatusListe?fnr=${session("user").as[String]}"))

  private val regAktivitetScenario = scenario("Veileder oppretter aktiviteter og gjoer endringer paa dem")
    .feed(veiledere)
    .feed(brukere_for_sok)
    .feed(aktivetTyper)
    .feed(livslopsStatuser)
    .feed(jobbstatus)
    .feed(kanaler)
    .exec(login)
    .exec(
      Helpers.httpPost("registrer aktivitet", session => s"/veilarbaktivitet/api/aktivitet/ny?fnr=${session("user").as[String]}")
        .body(ElFileBody("domain/stor-aktivitet.json"))
        .check(regex(".*").saveAs("responseJson"))
        .check(regex("\"id\":\"(.*?)\"").saveAs("aktivitet_id"))
    )
    .doIf("${postFeilet.isUndefined()}") {
      exec(
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
    }

  private val dialogScenario = scenario("Veileder oppretter og endrer dialog")
    .feed(veiledere)
    .feed(brukere_for_sok)
    .exec(login)
    .exec(
      Helpers.httpPost("ny dialog", session => s"/veilarbdialog/api/dialog?fnr=${session("user").as[String]}")
        .body(StringBody("{\"overskrift\":\"Overskrift\",\"tekst\":\"Ytelsestest\"}")).asJSON
        .check(regex("\"id\":\"(.*?)\"").saveAs("dialog_id"))
    )
    .doIf("${postFeilet.isUndefined()}") {
      exec(Helpers.httpPut("setter bruker maa svare til true", session => s"/veilarbdialog/api/dialog/${session("dialog_id").as[String]}/venter_pa_svar/true?fnr=${session("user").as[String]}"))
        .exec(Helpers.httpPut("setter bruker maa svare til false", session => s"/veilarbdialog/api/dialog/${session("dialog_id").as[String]}/venter_pa_svar/false?fnr=${session("user").as[String]}"))
        .exec(Helpers.httpPut("setter ferdig behandlet til false", session => s"/veilarbdialog/api/dialog/${session("dialog_id").as[String]}/ferdigbehandlet/false?fnr=${session("user").as[String]}"))
        .exec(Helpers.httpPut("setter ferdig behandlet til true", session => s"/veilarbdialog/api/dialog/${session("dialog_id").as[String]}/ferdigbehandlet/true?fnr=${session("user").as[String]}"))
    }

  private val innstillingerScenario = scenario("Veileder henter historikk og setter til manuell / under oppfoelging")
    .feed(veiledere)
    .feed(brukere_for_sok)
    .exec(login)
    .exec(Helpers.httpGetSuccess("henter innstillingerhistorikk", session => s"/veilarbsituasjon/api/situasjon/innstillingsHistorikk?fnr=${session("user").as[String]}"))
    .exec(Helpers.httpGetSuccess("henter reservert-status", session => s"/veilarbsituasjon/api/situasjon?fnr=${session("user").as[String]}")
      .check(regex("\"reservasjonKRR\":(.*?),").saveAs("erReservert")))

    .doIfEquals("${erReservert}", "false") {
        exec(
          Helpers.httpPost("setter bruker til manuell oppfolging", session => s"/veilarbsituasjon/api/situasjon/settManuell?fnr=${session("user").as[String]}")
            .body(StringBody("""{"begrunnelse":"setter ${user} til manuell","veilederId":"Ytelesestest-veileder"}""")).asJSON
        )
        .exec(
          Helpers.httpPost("setter bruker til digital oppfolging", session => s"/veilarbsituasjon/api/situasjon/settDigital?fnr=${session("user").as[String]}")
            .body(StringBody("""{"begrunnelse":"setter ${user} til digital","veilederId":"Ytelesestest-veileder"}""")).asJSON
        )
        .exec(
          Helpers.httpGetSuccess("sjekker innstillingerhistorikk", session => s"/veilarbsituasjon/api/situasjon/innstillingsHistorikk?fnr=${session("user").as[String]}")
            .check(regex("${user}").count.greaterThan(1))
        )
      }
    .exec(Helpers.httpGetSuccess("sjekker avslutningsstatus", session => s"/veilarbsituasjon/api/situasjon/avslutningStatus?fnr=${session("user").as[String]}"))

    private val situasjonsFeedScenario = scenario ("Situasjonsfeed")
      .exec(loginFeed())
      .exec(FeedHelpers.httpGetFeed("henter portefoljefeed", "/veilarbsituasjon/api/feed/situasjon?id="+initialDateSituasjonsfeed+"&page_size=100"))
      .exec(FeedHelpers.traverseFeed("traverserer portefoljefeed", session => s"/veilarbsituasjon/api/feed/situasjon?id=${session("nextPage").as[String]}&page_size="+feedPageSize))

    private val dialogFeedScenario = scenario ("Dialogfeed")
      .exec(loginFeed())
      .exec(FeedHelpers.httpGetFeed("henter dialogfeed", "/veilarbdialog/api/feed/dialogaktor?id="+initialDateSituasjonsfeed+"&page_size=100"))
      .exec(FeedHelpers.traverseFeed("traverserer dialogfeed", session => s"/veilarbdialog/api/feed/dialogaktor?id="+URLEncoder.encode(s"${session("nextPage").as[String]}", "UTF-8")+"&page_size="+feedPageSize))

    private val aktivitetFeedScenario = scenario ("Aktivitetfeed")
      .exec(loginFeed())
      .exec(FeedHelpers.httpGetFeed("henter aktivitetfeed", "/veilarbaktivitet/api/feed/aktiviteter?id="+initialDateAktivitetfeed+"&page_size=100"))
      .exec(FeedHelpers.traverseFeed("traverserer aktivitetfeed", session => "/veilarbaktivitet/api/feed/aktiviteter?id="+URLEncoder.encode(s"${session("nextPage").as[String]}", "UTF-8")+"&page_size="+feedPageSize))


  setUp(
    personflateScenario.inject(constantUsersPerSec(usersPerSecEnhet) during (duration seconds)),
    regAktivitetScenario.inject(constantUsersPerSec(usersPerSecEnhet) during (duration seconds)),
    dialogScenario.inject(constantUsersPerSec(usersPerSecEnhet) during (duration seconds)),
    innstillingerScenario.inject(constantUsersPerSec(usersPerSecEnhet) during (duration seconds)),
    situasjonsFeedScenario.inject(constantUsersPerSec(1) during (1 seconds)),
    dialogFeedScenario.inject(constantUsersPerSec(1) during (1 seconds)),
    aktivitetFeedScenario.inject(constantUsersPerSec(1) during (1 seconds))
  ).protocols(httpProtocol)
    .assertions(global.successfulRequests.percent.gte(99))

}
