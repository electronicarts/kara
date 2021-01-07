/*
 * Copyright (C) 2021 Electronic Arts Inc.  All rights reserved.
 */

import java.net.InetSocketAddress

import com.local._
import com.twitter.finagle.http
import com.twitter.finagle.Http
import com.twitter.finagle.Service
import com.twitter.util.{Await, Future}
import io.circe._
import io.circe.generic.auto._
import com.ea.kara.generated.test._

object E2eTest extends App {

  case class GenericError(message: String)

  lazy val svc: Service[http.Request, http.Response] = new HttpTestService(new ThriftService)

  override def main(args: Array[String]): Unit = {
    assert(args.length == 1, "Should pass the name of the test to run.")

    val testName = args.head
    val resource = s"$testName.json"

    val body = resourceToJsonString(resource)

    testName match {
      case "mirror"           => mirror(jsonBodyToFinagleRequest("mirror", body))
      case "oneArgUnwrapped"  => oneArgUnwrapped(jsonBodyToFinagleRequest("oneArg", body))
      case "knownException"   => knownException(jsonBodyToFinagleRequest("throwing", body))
      case "unknownException" => unknownException(jsonBodyToFinagleRequest("throwing", body))
      case "noArg"            => noArg(jsonBodyToFinagleRequest("noArg", body))
      case "oneArgWrapped"    => oneArgWrapped(jsonBodyToFinagleRequest("oneArg", body))
      case "multiArg"         => multiArg(jsonBodyToFinagleRequest("multiArg", body))
    }
  }

  def mirror(request: http.Request): Unit = {
    val requestBody  = parseJson(request.contentString)
    val response     = Await.result(svc(request))
    val responseBody = parseJson(response.contentString)
    assertSuccessfulResponse(response)
    assert(
      requestBody.equals(responseBody),
      s"""
         |Request body should be the same as Response body.
         |Request body:
         |${requestBody}
         |Response body:
         |${responseBody}
       """.stripMargin
    )
  }

  def oneArgUnwrapped(request: http.Request): Unit = {
    val requestBody = parseJson(request.contentString)
    assert(
      requestBody.equals(Json.obj()),
      s"""
         |Request body should be an empty JSON object.
         |Request body:
         |$requestBody
       """.stripMargin
    )
    val response     = Await.result(svc(request))
    val responseBody = parseJson(response.contentString)
    assertSuccessfulResponse(response)
    assert(
      responseBody.equals(Json.obj()),
      s"""
         |Response body should be an empty JSON object.
         |Response body:
         |$responseBody
       """.stripMargin
    )
  }

  def knownException(request: http.Request): Unit = {
    val requestBody  = parseJson(request.contentString)
    val response     = Await.result(svc(request))
    val responseBody = parseJson(response.contentString)
    assertUnsuccessfulResponse(response)
    assertObjectResponse(response)(AnException("As designed.", 418))
  }

  def unknownException(request: http.Request): Unit = {
    val requestBody  = parseJson(request.contentString)
    val response     = Await.result(svc(request))
    val responseBody = parseJson(response.contentString)
    assertUnsuccessfulResponse(response)
    assertObjectResponse(response)(GenericError("Not as designed."))
  }

  def noArg(request: http.Request): Unit = {
    val response     = Await.result(svc(request))
    val responseBody = parseJson(response.contentString)
    assertSuccessfulResponse(response)
    assertObjectResponse(response)(EmptyStruct())
  }

  def oneArgWrapped(request: http.Request): Unit = {
    val requestBody = parseJson(request.contentString)
    assert(
      requestBody.equals(Json.obj("one" -> Json.obj())),
      s"""
         |Request body should be a JSON object with one child empty object "one".
         |Request body:
         |$requestBody
       """.stripMargin
    )
    val response     = Await.result(svc(request))
    val responseBody = parseJson(response.contentString)
    assertSuccessfulResponse(response)
    assert(
      responseBody.equals(Json.obj()),
      s"""
         |Response body should be an empty JSON object.
         |Response body:
         |$responseBody
       """.stripMargin
    )
  }

  def multiArg(request: http.Request): Unit = {
    val requestBody = parseJson(request.contentString)
    assert(
      requestBody.equals(Json.obj("one" -> Json.obj(), "two" -> Json.obj())),
      s"""
         |Request body should be a JSON object with two child empty objects "one" and "two".
         |Request body:
         |$requestBody
       """.stripMargin
    )
    val response     = Await.result(svc(request))
    val responseBody = parseJson(response.contentString)
    assertSuccessfulResponse(response)
    assert(
      responseBody.equals(Json.obj()),
      s"""
         |Response body should be an empty JSON object.
         |Response body:
         |$responseBody
       """.stripMargin
    )
  }

  def assertObjectResponse[T: Decoder](response: http.Response)(expectedObject: T): Unit =
    parser.decode[T](response.contentString) match {
      case Right(obj) =>
        assert(obj == expectedObject, s"Object [$obj] doesn't equal [$expectedObject].")
      case Left(error) =>
        throw new RuntimeException(
          s"Response body ${response.contentString} can't be decoded. Reason: $error."
        )
    }

  def assertSuccessfulResponse(response: http.Response): Unit = {
    assertJsonResponse(response)
    assertResponseStatus(response, http.Status.Ok)
  }

  def assertUnsuccessfulResponse(response: http.Response): Unit = {
    assertJsonResponse(response)
    assertResponseStatus(response, http.Status.InternalServerError)
  }

  def assertResponseStatus(response: http.Response, expectedStatus: http.Status): Unit =
    assert(
      response.status == expectedStatus,
      s"""
         |Status code should be '$expectedStatus',
         |instead was found ${response.status}.
       """.stripMargin
    )

  def assertJsonResponse(response: http.Response): Unit = {
    val applicationJsonContentType = "application/json;charset=utf-8"
    assert(
      response.contentType == Some(applicationJsonContentType),
      s"""
         |Content type should be '$applicationJsonContentType',
         |instead was found '${response.contentType}'.
       """.stripMargin
    )
  }

  def jsonBodyToFinagleRequest(rpcName: String, jsonBody: String): http.Request = {
    val request = http.Request(method = com.twitter.finagle.http.Method.Post, uri = s"/$rpcName")
    request.setContentString(jsonBody)
    request.setContentTypeJson()
    request
  }

  def resourceToJsonString(resource: String): String =
    using(scala.io.Source.fromResource(resource))(_.mkString)

  def parseJson(jsonString: String): Json =
    parser
      .parse(jsonString)
      .getOrElse(throw new RuntimeException(s"Can't parse JSON from $jsonString."))

  private def using[A, B <: java.io.Closeable](closeable: => B)(f: B => A): A =
    try f(closeable)
    finally closeable.close()
}

class ThriftService extends TestService.MethodPerEndpoint {
  override def mirror(aStruct: AStruct): Future[AStruct] =
    Future(aStruct)
  override def oneArg(one: EmptyStruct): Future[EmptyStruct] =
    Future(one)
  override def throwing(boolStruct: BooleanWrapperStruct): Future[Unit] =
    if (boolStruct.isTrue)
      Future.exception(AnException("As designed.", 418))
    else
      Future.exception(new RuntimeException("Not as designed."))
  override def noArg(): Future[EmptyStruct] =
    Future(EmptyStruct())
  override def multiArg(one: EmptyStruct, two: EmptyStruct): Future[EmptyStruct] =
    Future(EmptyStruct())
}
