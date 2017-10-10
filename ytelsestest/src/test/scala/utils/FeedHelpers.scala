package utils

import java.net.URLEncoder
import java.util.concurrent.TimeUnit

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import io.gatling.core.Predef._
import io.gatling.core.body.CompositeByteArrayBody
import io.gatling.core.session.Expression
import io.gatling.http.Predef._
import io.gatling.http.request.builder._
import io.gatling.http.request.ExtraInfo

object FeedHelpers {
  def httpGetFeed(navn: String, uri: Expression[String]): HttpRequestBuilder = {
    http(navn)
      .get(uri)
      .check(status.is(200))
      .check(regex("\"nextPageId\" : \"(.*?)\"").saveAs("nextPage"))
  }

  def traverseFeed(navn: String, uri: Expression[String]) = {
        asLongAs(session =>  session("nextPage").asOption[String] != session("lastPage").asOption[String]) {
          pause(1)
          .exec(session => session.set("lastPage", session("nextPage").as[String]))
          .exec(httpGetFeed(navn, uri))
    }
  }
}