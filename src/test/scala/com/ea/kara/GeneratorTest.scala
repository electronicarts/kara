/*
 * Copyright (C) 2020 Electronic Arts Inc.  All rights reserved.
 */

package com.ea.kara

import better.files._
import io.circe.yaml.parser
import io.swagger.v3.parser.OpenAPIV3Parser
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterAll, EitherValues}
import sbt.internal.util.ConsoleLogger

import scala.collection.JavaConverters._
import scala.io.Source

class GeneratorTest extends AnyWordSpec with Matchers with EitherValues with BeforeAndAfterAll {
  private val testResourceFolder = File("src/test/resources")
  private val inResourceFolder   = Seq(testResourceFolder / "in", testResourceFolder / "in_imported")
  private val tmpFolder          = testResourceFolder / "tmp"
  private val tmpSourceFolder    = tmpFolder / "src"
  private val tmpResourceFolder  = tmpFolder / "resrc"

  private val generator = new Generator(
    thriftIncludes = inResourceFolder.map(_.toJava),
    thriftSources = inResourceFolder.flatMap(_.toJava.listFiles()),
    serviceNames = Seq("com.example.ExampleService"),
    karaHeaders = Seq(
      "Finagle-Ctx-Header1",
      "Finagle-Ctx-Header2"
    ),
    sourcePath = tmpSourceFolder.toJava,
    resourcePath = tmpResourceFolder.toJava
  )(ConsoleLogger())

  private def createTmpDirectory(): Unit = tmpFolder.createDirectories()

  private def deleteTmpDirectory(): Unit = tmpFolder.delete(swallowIOExceptions = true)

  override def beforeAll(): Unit = createTmpDirectory()

  override def afterAll(): Unit = deleteTmpDirectory()

  "Generator" should {

    /*
    TODO this test fails for any non-functional modification to the service.mustache template - e.g. spacing or variable names - which is not what they're supposed to test.
    TODO should be removed once we move its value to a less flaky solution.
     */
    "generate sources" in {
      generator.generateSources()

      val importedPackage = tmpSourceFolder / "com/ea/kara/generated/imported_test/package.scala"
      importedPackage.exists() shouldBe true
      importedPackage.contentAsString shouldBe Source
        .fromResource("expected/imported_test_package.expected")
        .mkString

      val mainPackage = tmpSourceFolder / "com/ea/kara/generated/test/package.scala"
      mainPackage.exists() shouldBe true
      mainPackage.contentAsString shouldBe Source
        .fromResource("expected/test_package.expected")
        .mkString

      val svc = tmpSourceFolder / "com/ea/kara/generated/test/HttpExampleService.scala"
      svc.exists() shouldBe true
      svc.contentAsString.trim() shouldBe Source.fromResource("expected/ExampleService.expected").mkString.trim()
    }

    /*
    TODO this test fails for any non-functional modification to the service.mustache template - e.g. spacing or variable names - which is not what they're supposed to test.
    TODO should be removed once we move its value to a less flaky solution.
     */
    "generate resources" in {
      generator.generateResources()

      val svcOas = tmpResourceFolder / "swagger/com.example.ExampleService/service.oas"
      svcOas.exists() shouldBe true
      val svcOasAsYaml = parser.parse(svcOas.contentAsString).right.value
      val expectedSvcOasAsYaml =
        parser.parse(Source.fromResource("expected/ExampleService.oas").mkString).right.value
      assert(svcOasAsYaml.equals(expectedSvcOasAsYaml))
    }

    "generate resources in valid OAS format" in {
      generator.generateResources()

      val svcOas = tmpResourceFolder / "swagger/com.example.ExampleService/service.oas"
      svcOas.exists() shouldBe true
      val svcOasAsYaml = parser.parse(svcOas.contentAsString).right.value

      val parsedSvcOas = new OpenAPIV3Parser().readContents(svcOasAsYaml.spaces2)
      Option(parsedSvcOas.getOpenAPI) should not be empty
    }

    "generate resource whose schema links always resolve" in {
      generator.generateResources()

      val svcOas = tmpResourceFolder / "swagger/com.example.ExampleService/service.oas"
      svcOas.exists() shouldBe true
      val svcOasAsYaml = parser.parse(svcOas.contentAsString).right.value

      val parsedSvcOas = new OpenAPIV3Parser().readContents(svcOasAsYaml.spaces2)

      val paths = parsedSvcOas.getOpenAPI.getPaths.asScala
      val posts = paths.map {
        case (_, pathItem) => pathItem.getPost
      }
      val requestRefs = posts
        .flatMap { post =>
          Option(post.getRequestBody.getContent)
        }
        .flatMap { content =>
          content.asScala.flatMap {
            case (_, mediaType) => Option(mediaType.getSchema.get$ref())
          }
        }
      val responseRefs = posts.flatMap { post =>
        post.getResponses.asScala.flatMap {
          case (_, response) =>
            response.getContent.asScala.flatMap {
              case (_, mediaType) => Option(mediaType.getSchema.get$ref())
            }
        }
      }
      val allRefs      = requestRefs ++ responseRefs
      val schemas      = parsedSvcOas.getOpenAPI.getComponents.getSchemas
      val schemaPrefix = "#/components/schemas/"

      allRefs.foreach { ref =>
        assert(ref.startsWith(schemaPrefix))
        val refWithoutPrefix = ref.stripPrefix(schemaPrefix)
        assert(schemas.containsKey(refWithoutPrefix))
      }
    }
  }
}
