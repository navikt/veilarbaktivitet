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

class FeedSimulation extends Simulation {

  private val logger = LoggerFactory.getLogger(FeedSimulation.this.getClass)

  ////////////////////////////
  //Variabler settes i Jenkins
  ///////////////////////////
  private val duration = Integer.getInteger("DURATION", 7500).toInt
  private val baseUrl = System.getProperty("BASEURL", "https://app-t4.adeo.no")
  private val loginUrl = System.getProperty("LOGINURL", "https://isso-t.adeo.no")
  private val feedUsername = System.getProperty("FEED_USERNAME", "srvveilarbportefolje")
  private val feedPassword = System.getProperty("FEED_PASSWORD", "!!ChangeMe!!")
  val oidcPassword = System.getProperty("OIDC_PASSWD", "!!ChangeMe!!")
  private val initialDateSituasjonsfeed =  System.getProperty("INITIAL_DATE_SITUASJONSFEED", "2017-08-21T17:24:23.882Z")
  private val initialDateDialogfeed =  System.getProperty("INITIAL_DATE_DIALOGFEED", "2017-08-21T17:24:23.882Z")
  private val initialDateAktivitetfeed =  System.getProperty("INITIAL_DATE_AKTIVITETFEED", "2017-08-21T17:24:23.882Z")
  private val feedPageSize =  System.getProperty("FEED_PAGE_SIZE", "100")


  ///////////////////////////
  //Login
  ///////////////////////////
  private val appnavn = "veilarbpersonflatefs"
  private val openIdConnectLogin = new OpenIdConnectLogin("veilarblogin-t3", oidcPassword, loginUrl, baseUrl, appnavn)


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


  private val situasjonsFeedScenario = scenario ("Situasjonsfeed")
    .exec(loginFeed())
    .exec(FeedHelpers.httpGetFeed("henter portefoljefeed", "/veilarbsituasjon/api/feed/situasjon?id="+initialDateSituasjonsfeed+"&page_size=100"))
    .exec(FeedHelpers.traverseFeed("traverserer situasjonsfeed", session => s"/veilarbsituasjon/api/feed/situasjon?id=${session("nextPage").as[String]}&page_size="+feedPageSize))

  private val dialogFeedScenario = scenario ("Dialogfeed")
    .exec(loginFeed())
    .exec(FeedHelpers.httpGetFeed("henter dialogfeed", "/veilarbdialog/api/feed/dialogaktor?id="+initialDateDialogfeed+"&page_size=100"))
    .exec(FeedHelpers.traverseFeed("traverserer dialogfeed", session => s"/veilarbdialog/api/feed/dialogaktor?id="+URLEncoder.encode(s"${session("nextPage").as[String]}", "UTF-8")+"&page_size="+feedPageSize))

  private val aktivitetFeedScenario = scenario ("Aktivitetfeed")
    .exec(loginFeed())
    .exec(FeedHelpers.httpGetFeed("henter aktivitetfeed", "/veilarbaktivitet/api/feed/aktiviteter?id="+initialDateAktivitetfeed+"&page_size=100"))
    .exec(FeedHelpers.traverseFeed("traverserer aktivitetfeed", session => "/veilarbaktivitet/api/feed/aktiviteter?id="+URLEncoder.encode(s"${session("nextPage").as[String]}", "UTF-8")+"&page_size="+feedPageSize))


  setUp(
    situasjonsFeedScenario.inject(splitUsers(100) into(rampUsers(1) over (60 seconds)) separatedBy (60 seconds)),
    dialogFeedScenario.inject(splitUsers(100) into(rampUsers(1) over (1 seconds)) separatedBy (60 seconds)),
    aktivitetFeedScenario.inject(splitUsers(100) into(rampUsers(1) over (1 seconds)) separatedBy (300 seconds))
  ).protocols(httpProtocol)
    .assertions(global.successfulRequests.percent.gte(99))

}
