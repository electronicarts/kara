/*
 * Copyright (C) 2022 Electronic Arts Inc.  All rights reserved.
 */

import java.net.InetSocketAddress

import com.local._
import com.twitter.finagle.http
import com.twitter.finagle.Http
import com.twitter.finagle.Service
import com.twitter.util.{Await, Future}
import io.circe._
import io.circe.generic.auto._
import com.ea.kara.generated.svc_one._
import com.ea.kara.generated.svc_two._
import com.ea.kara.generated.svcs_three_four._

object MultiServiceTest extends App {

  case class GenericError(message: String)

  lazy val svcOne: Service[http.Request, http.Response]   = new HttpServiceOne(new ServiceOne)
  lazy val svcTwo: Service[http.Request, http.Response]   = new HttpServiceTwo(new ServiceTwo)
  lazy val svcThree: Service[http.Request, http.Response] = new HttpServiceThree(new ServiceThree)
  lazy val svcFour: Service[http.Request, http.Response]  = new HttpServiceFour(new ServiceFour)

  override def main(args: Array[String]): Unit = {
    assert(args.length == 1, "Should pass the name of the test to run.")

    val testName = args.head

    val request = finagleRequest("ping")

    testName match {
      case "pingServiceOne"   => serve(svcOne, request)
      case "pingServiceTwo"   => serve(svcTwo, request)
      case "pingServiceThree" => serve(svcThree, request)
      case "pingServiceFour"  => serve(svcFour, request)
    }
  }

  def serve(svc: Service[http.Request, http.Response], request: http.Request): Unit = {
    val response = Await.result(svc(request))
    assertSuccessfulResponse(response)
  }

  def finagleRequest(rpcName: String): http.Request = {
    val request = http.Request(method = com.twitter.finagle.http.Method.Post, uri = s"/$rpcName")
    request.setContentTypeJson()
    request
  }

  def assertSuccessfulResponse(response: http.Response): Unit = {
    assertJsonResponse(response)
    assertResponseStatus(response, http.Status.Ok)
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
}

class ServiceOne extends ServiceOne.MethodPerEndpoint {
  override def ping(): Future[Unit] = Future.Unit
}

class ServiceTwo extends ServiceTwo.MethodPerEndpoint {
  override def ping(): Future[Unit] = Future.Unit
}

class ServiceThree extends ServiceThree.MethodPerEndpoint {
  override def ping(): Future[Unit] = Future.Unit
}

class ServiceFour extends ServiceFour.MethodPerEndpoint {
  override def ping(): Future[Unit] = Future.Unit
}
