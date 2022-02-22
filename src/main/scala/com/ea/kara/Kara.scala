/*
 * Copyright (C) 2022 Electronic Arts Inc.  All rights reserved.
 */

package com.ea.kara

import com.twitter.scrooge.ScroogeSBT
import sbt._
import sbt.Keys._

object Kara extends AutoPlugin {

  override def requires: Plugins = ScroogeSBT

  object autoImport {
    lazy val karaServices = SettingKey[Seq[String]](
      "kara-services",
      "The Thrift services and related Swagger UIs Kara should generate."
    )

    lazy val karaCodeGen = TaskKey[Seq[File]](
      "kara-code-gen",
      "Generate code for HTTP/JSON services."
    )

    lazy val karaHeaders = SettingKey[Seq[String]](
      "kara-headers",
      "Add HTTP headers to the open api specification"
    )

    lazy val karaResourceGen = TaskKey[Seq[File]](
      "kara-resource-gen",
      "Generate resources for Swagger UI."
    )

    private[kara] lazy val karaLogger = TaskKey[Logger](
      "kara-logger",
      "Kara logger instance"
    )

    private[kara] lazy val karaGenerator = TaskKey[Generator](
      "kara-generator",
      "Kara entry point for code generation"
    )
  }

  import autoImport._
  import ScroogeSBT.autoImport._

  val circeVersion: String = "0.14.1"

  override lazy val globalSettings: Seq[Setting[_]] = Seq(
    Compile / karaHeaders := Seq.empty
  )

  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    libraryDependencies ++=
      Seq("finagle-http", "scrooge-serializer", "finagle-thrift")
        .map("com.twitter" %% _)
        .map(_ % com.twitter.BuildInfo.version) ++
        Seq("circe-generic", "circe-parser")
          .map("io.circe" %% _)
          .map(_ % circeVersion),
    karaLogger := streams.value.log,
    karaGenerator := new Generator(
      (Compile / scroogeThriftIncludes).value,
      (Compile / scroogeThriftSources).value,
      (Compile / karaServices).value,
      (Compile / karaHeaders).value,
      (Compile / sourceManaged).value,
      (Compile / resourceManaged).value
    )((Compile / karaLogger).value),
    Compile / karaCodeGen := {
      val logger    = karaLogger.value
      val generator = karaGenerator.value

      def listSources(): Seq[File] = (generator.sourcePath ** "kara" ** "*").filter(_.isFile).get

      val oldSources = listSources()

      if ((Compile / scroogeIsDirty).value) {
        if (oldSources.nonEmpty) {
          val karaDir = (generator.sourcePath ** "kara").filter(_.isDirectory).get.headOption
          logger.info(
            s"Generated Kara sources in $karaDir are out of date, deleting them and re-generating"
          )
          karaDir.foreach(IO.delete)
        }
        generator.generateSources()
        val sources = listSources()
        logger.success(
          s"Generated Kara sources: " +
            sources.map(_.getPath()).mkString(", ")
        )
        sources
      } else {
        logger.info(
          s"Kara sources won't be generated, already existing and up to date: " +
            oldSources.map(_.getPath()).mkString(", ")
        )
        oldSources
      }
    },
    Compile / karaCodeGen := (Compile / karaCodeGen)
      .dependsOn(Compile / scroogeGen)
      .value,
    Compile / karaResourceGen := {
      val logger    = karaLogger.value
      val generator = karaGenerator.value

      def listResources(): Seq[File] =
        (generator.resourcePath ** "swagger" ** "*").filter(_.isFile).get

      val oldResources = listResources()

      if ((Compile / scroogeIsDirty).value) {
        if (oldResources.nonEmpty) {
          val swaggerDir =
            (generator.resourcePath ** "swagger").filter(_.isDirectory).get.headOption
          logger.info(
            s"Generated Kara resources in $swaggerDir are out of date, deleting them and re-generating"
          )
          swaggerDir.foreach(IO.delete)
        }
        generator.generateResources()
        val resources = listResources()
        logger.success(
          s"Generated Kara resources: " +
            resources.map(_.getPath()).mkString(", ")
        )
        resources
      } else {
        logger.info(
          s"Kara resources won't be generated, already existing and up to date: " +
            oldResources.map(_.getPath()).mkString(", ")
        )
        oldResources
      }
    },
    Compile / karaResourceGen := (Compile / karaResourceGen)
      .dependsOn(Compile / scroogeGen)
      .value,
    Compile / sourceGenerators += (Compile / karaCodeGen).taskValue,
    Compile / resourceGenerators += (Compile / karaResourceGen).taskValue
  )
}
