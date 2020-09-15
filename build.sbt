/*
 * Copyright (C) 2020 Electronic Arts Inc.  All rights reserved.
 */

import sbt._

val finagleVersion = "20.5.0"

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

envVars ++= Map("CI_PROJECT_DIR" -> sys.env.getOrElse("CI_PROJECT_DIR", "."))

lazy val sbtOps = sys.env.get("SBT_OPTS").map(_.split(" ")).getOrElse(Array.empty)

lazy val root = (project in file("."))
  .enablePlugins(ScriptedPlugin)
  .settings(
    addSbtPlugin("com.twitter" % "scrooge-sbt-plugin" % finagleVersion),
    libraryDependencies ++= Seq(
      "commons-io"            % "commons-io"   % "2.7",
      "org.scalatra.scalate" %% "scalate-core" % "1.9.6",
      "com.twitter"          %% "finagle-http" % finagleVersion % Test,
      "io.circe"             %% "circe-yaml"   % "0.13.0",
      "com.github.pathikrit" %% "better-files" % "3.9.1",
      "io.swagger.parser.v3" % "swagger-parser" % "2.0.21" % Test,
      "org.scalatest"       %% "scalatest"      % "3.2.1"  % Test
    ),
    scriptedLaunchOpts ++= Seq(
      "-Xmx1024M",
      "-Dplugin.version=" + version.value
    ) ++ sbtOps,
    scriptedBufferLog := false,
    name := "kara",
    organization := "com.ea",
    sbtPlugin := true,
    scalaVersion := "2.12.12"
  )

lazy val docs = project
  .in(file("mdoc"))
  .settings(
    skip in publish := true,
    mdocOut := file("."),
    mdocVariables := Map(
      // TODO: If this runs in the pipeline we can simply use `version.value`?
      "VERSION" -> version.value.stripSuffix("-SNAPSHOT")
    )
  )
  .enablePlugins(MdocPlugin)
