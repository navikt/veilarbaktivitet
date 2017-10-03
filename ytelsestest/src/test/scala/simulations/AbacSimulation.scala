package simulations

import java.net.URLEncoder

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import no.nav.sbl.gatling.login.OpenIdConnectLogin
import org.slf4j.LoggerFactory
import utils.{Helpers}
import java.util.concurrent.TimeUnit

import io.gatling.core.session.Expression
import io.gatling.core.feeder.RecordSeqFeederBuilder

import scala.concurrent.duration._
import scala.util.Random

class AbacSimulation extends Simulation {

  private val logger = LoggerFactory.getLogger(AbacSimulation.this.getClass)

  ////////////////////////////
  //Variabler settes i Jenkins
  ///////////////////////////
  private val duration = Integer.getInteger("DURATION", 7500).toInt
  //private val baseUrl = System.getProperty("BASEURL", "http://localhost:8080")
  private val baseUrl = System.getProperty("BASEURL", "https://app-t3.adeo.no")
  //private val baseUrl = System.getProperty("BASEURL", "https://wasapp-t3.adeo.no")

  private val loginUrl = System.getProperty("LOGINURL", "https://isso-t.adeo.no")
  val password = System.getProperty("VEILEDER_PASSWD", "Teflon3970") //Teflon3970
  val oidcPassword = System.getProperty("OIDC_PASSWD", "0987654321")


  ///////////////////////////
  //Feeders
  ///////////////////////////
  private val veiledere = csv(System.getProperty("VEILEDERE", "veileder-data/veiledere.csv")).random
  private val brukere_for_sok = csv(System.getProperty("VEILEDER_BRUKERE", "veileder-data/brukere_for_sok.csv")).random


  ///////////////////////////
  //Login
  ///////////////////////////
  private val appnavn = "veilarbpersonflatefs"
  private val openIdConnectLogin = new OpenIdConnectLogin("OIDC", oidcPassword, loginUrl, baseUrl, appnavn)

  private def login() = {
    exec(addCookie(Cookie("ID_token", session => openIdConnectLogin.getIssoToken(session("username").as[String], password))))
      .exec(addCookie(Cookie("refresh_token", session => openIdConnectLogin.getRefreshToken(session("username").as[String], password))))
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
    .contentTypeHeader("application/xacml+json")
    .userAgentHeader("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:30.0) Gecko/20100101 Firefox/30.0")
    .disableWarmUp
    .silentResources
    .extraInfoExtractor {extraInfo => List(Helpers.getInfo(extraInfo))}
    .basicAuth("srvveilarbportefolje","APUkG8YqkzTJ9eJ")



  ///////////////////////////
  //Scenarioer
  ///////////////////////////

  private val loginScenario = scenario("Logger inn")
    .feed(veiledere)
    .exec(login)

  private val oidcScenario = scenario("Tester Abac")
    .feed(veiledere)
    .feed(brukere_for_sok)
    .exec(session => session.set("oidcToken", openIdConnectLogin.getIssoToken(session("username").as[String], password)))

    .exec(
      Helpers.httpPost("Autoriserer", "https://wasapp-t3.adeo.no/asm-pdp/authorize")
        .body(ElFileBody("domain/abac-request.json"))
        .check(regex(".*").saveAs("responseJson"))
    )

  setUp(
    loginScenario.inject(constantUsersPerSec(10) during (140 seconds)),
    oidcScenario.inject(nothingFor(140 seconds), constantUsersPerSec(100) during (140 seconds))
  ).protocols(httpProtocol)
    .assertions(global.successfulRequests.percent.gte(99))

}
