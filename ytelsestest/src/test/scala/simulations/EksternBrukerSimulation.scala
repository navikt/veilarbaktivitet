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

class EksternBrukerSimulation extends Simulation {

  private val logger = LoggerFactory.getLogger(EksternBrukerSimulation.this.getClass)

  ////////////////////////////
  //Variabler settes i Jenkins
  ///////////////////////////
  private val usersPerSecReading = Integer.getInteger("USERS_PER_SEC",1).toInt
  private val usersPerSecEditing = Integer.getInteger("USERS_PER_SEC",1).toInt
  private val duration = Integer.getInteger("DURATION", 500).toInt
  private val baseUrl = System.getProperty("BASEURL", "https://tjenester-t3.nav.no")
  private val standard_headers = Map( """Accept""" -> """text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8""")

  ///////////////////////////
  //Feeders
  ///////////////////////////
  private val eksterne_brukere = csv(System.getProperty("EKSTERNE_BRUKERE", "ekstern-bruker-data/brukere.csv")).circular
  private val aktivitetTyper = Array (
    Map("aktivitettype" -> "STILLING"),
    Map("aktivitettype" -> "EGEN"),
    Map("aktivitettype" -> "IJOBB")).circular

  private val jobbstatus = Array (
    Map("jobbstatus" -> "HELTID"),
    Map("jobbstatus" -> "DELTID")).circular

  private val livslopsStatuser = Array (
    Map("livslopsStatus" -> "BRUKER_ER_INTERESSERT"),
    Map("livslopsStatus" -> "PLANLAGT"),
    Map("livslopsStatus" -> "GJENNOMFORES")).circular


  ///////////////////////////
  //Login
  ///////////////////////////
  private def login() = {
    exec(
      http("Get OpenAM")
        .get("""/esso/UI/Login?service=level4Service""")
        .headers(standard_headers)
        .check(status.is(200))
    )
    .exec(
      http("Login OpenAm")
        .post( """/esso/UI/Login""")
        .disableFollowRedirect
        .headers(standard_headers)
        .formParam("""IDToken1""", "${username}")
        .formParam("""IDToken2""", """Eifel123""")
        .formParam("""IDButton""", """Log In""")
        .formParam("""goto""", """***REMOVED***""")
        .formParam("""gotoOnFail""", """""")
        .formParam("""SunQueryParamsString""", """***REMOVED***""")
        .formParam("""encoded""", """true""")
        .formParam("""gx_charset""", """UTF-8""")
        .check(status.is(302))
    )
  }

  ///////////////////////////
  //HTTP-oppsett
  ///////////////////////////
  private val httpProtocol = http
    .baseURL(baseUrl)
    .userAgentHeader( """Mozilla/5.0 (Windows NT 6.1; WOW64; rv:30.0) Gecko/20100101 Firefox/30.0""")
    .disableWarmUp
    .extraInfoExtractor {extraInfo => List(Helpers.getInfo(extraInfo))}

  ///////////////////////////
  //Hjelpemetoder
  ///////////////////////////
  private def hentSituasjonOgSettVariabler() = {
    exec(Helpers.httpGetSuccess("hent situasjon", "/veilarbsituasjonproxy/api/situasjon")
      .check(regex("\"vilkarMaBesvares\":(.*?),").saveAs("vilkarMaBesvares"))
      .check(regex("\"reservasjonKRR\":(.*?),").saveAs("reservasjonKRR"))
      .check(regex("\"manuell\":(.*?),").saveAs("manuell"))
      .check(regex("\"underOppfolging\":(.*?),").saveAs("underOppfolging")))
  }

  ///////////////////////////
  //Scenarioer
  ///////////////////////////
  private val henterAktivitetsplanScenario = scenario ("Henter aktivitetsplan - ekstern bruker")
    .feed(eksterne_brukere)
    .exec(login)
    .exec(Helpers.httpGetSuccess("henter innloggingslinje", "/innloggingslinje/auth"))
    .exec(Helpers.httpGetSuccess("henter tekster", "/aktivitetsplan/api/tekster"))
    .exec(Helpers.httpGetSuccess("me", "/veilarbsituasjonproxy/api/situasjon/me"))
    .exec(hentSituasjonOgSettVariabler)
    .doIfEquals("${vilkarMaBesvares}", "true") {
      exec(Helpers.httpGetSuccess("henter vilkaar", "/veilarbsituasjonproxy/api/situasjon/vilkar").check(regex("\"hash\":\"(.*?)\"").saveAs("hash")))
      .exec(Helpers.httpPost("godtar vilkaar", session => s"/veilarbsituasjonproxy/api/situasjon/godta/${session("hash").as[String]}"))
    }
    .exec(Helpers.httpGetSuccess("hent situasjon","/veilarbsituasjonproxy/api/situasjon"))
    .exec(Helpers.httpGetSuccess("hent dialog", "/veilarbdialogproxy/api/dialog"))
    .exec(Helpers.httpGetSuccess("hent aktiviteter", "/veilarbaktivitetproxy/api/aktivitet"))
    .exec(Helpers.httpGetSuccess("hent arena-aktiviteter", "/veilarbaktivitetproxy/api/aktivitet/arena"))
    .exec(Helpers.httpGetSuccess("hent maal", "/veilarbsituasjonproxy/api/situasjon/mal"))
    .exec(Helpers.httpGetSuccess("hent maal-historikk", "/veilarbsituasjonproxy/api/situasjon/malListe"))
    .exec(Helpers.httpGetSuccess("hent vilkaar", "/veilarbsituasjonproxy/api/situasjon/hentVilkaarStatusListe"))


  private val editererInfoScenario = scenario ("Editerer og verifiserer aktiviteter og dialoger - ekstern bruker")
    .feed(eksterne_brukere)
    .feed(aktivitetTyper)
    .feed(livslopsStatuser)
    .feed(jobbstatus)
    .exec(login)
    .exec(hentSituasjonOgSettVariabler)
    .exec(Helpers.httpPost("registrerer maal", session => s"/veilarbsituasjonproxy/api/situasjon/mal")
      .body(StringBody("{\"mal\":\"Ytelsestest - Lager et nytt maal\"}")).asJSON
    )
    .exec(
      Helpers.httpPost("registrer aktivitet", "/veilarbaktivitetproxy/api/aktivitet/ny")
        .body(ElFileBody("domain/liten-aktivitet.json"))
        .check(regex(".*").saveAs("responseJson"))
        .check(regex("\"id\":\"(.*?)\"").saveAs("aktivitet_id"))
    )
    .doIf(session => session("postFeilet").asOption[String].isEmpty) {
      exec(Helpers.httpGetSuccess("hent nylig lagret aktivitet", session => s"/veilarbaktivitetproxy/api/aktivitet/${session("aktivitet_id").as[String]}")
          .check(regex("\"beskrivelse\":\"${username}\""))
      )
      .exec(Helpers.httpPut("kaller endre-aktivitet-endepunkt", session => s"/veilarbaktivitetproxy/api/aktivitet/${session("aktivitet_id").as[String]}")
          .body(StringBody("""${responseJson}""")).asJSON
      )
      .exec(Helpers.httpPut("kaller endre-status-endepunkt", session => s"/veilarbaktivitetproxy/api/aktivitet/${session("aktivitet_id").as[String]}/status")
          .body(StringBody("""${responseJson}""")).asJSON
      )
      .exec(Helpers.httpGetSuccess("kaller versjoner(historikk)-endepunkt", session => s"/veilarbaktivitetproxy/api/aktivitet/${session("aktivitet_id").as[String]}/versjoner"))
      .doIfEquals("${underOppfolging}", "false") {
        exec(Helpers.httpDeleteSuccess("slett aktivitet", session => s"/veilarbaktivitetproxy/api/aktivitet/${session("aktivitet_id").as[String]}"))
      }
    }
    .exec(
      Helpers.httpPost("oppretter ny dialog", session => s"/veilarbdialogproxy/api/dialog")
        .body(StringBody("{\"overskrift\":\"Overskrift\",\"tekst\":\"Ytelsestest\"}")).asJSON
    )

  setUp(
    henterAktivitetsplanScenario.inject(constantUsersPerSec(usersPerSecReading) during (duration seconds)),
    editererInfoScenario.inject(constantUsersPerSec(usersPerSecEditing) during (duration seconds))
  ).protocols(httpProtocol)
    .assertions(global.successfulRequests.percent.gte(99))

}

