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
  val mapper = new ObjectMapper() with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)


  def httpGetFeed(navn: String, uri: Expression[String]): HttpRequestBuilder = {
    http(navn)
      .get(uri)
      .check(status.is(200))
      .check(regex("\"nextPageId\":\"(.*?)\"").saveAs("nextPageVariable"))
      .check(regex("\"id\":\"(.*?)\"").saveAs("lastReceivedId"))
  }

  def traverseFeed(navn: String, uri: Expression[String]) = {
      exec(session => session.set("nextPageVariableUrlEncoded",  URLEncoder.encode(session("nextPageVariable").as[String])))
        .asLongAs(session => session("nextPageVariable").as[String] != session("lastReceivedId").as[String]) {
        pause(1)
        .exec(httpGetFeed(navn, uri))
    }
  }
}