/*
 * Copyright (C) 2021 Electronic Arts Inc.  All rights reserved.
 */

import sbt._

val finagleVersion = "20.10.0"

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

envVars ++= Map("CI_PROJECT_DIR" -> sys.env.getOrElse("CI_PROJECT_DIR", "."))

lazy val sbtOps = sys.env.get("SBT_OPTS").map(_.split(" ")).getOrElse(Array.empty)

lazy val publishSettings = Seq(
  homepage := Some(url("https://github.com/electronicarts/kara")),
  organization := "com.ea.kara",
  licenses := Seq("The 3-Clause BSD License" -> url("http://opensource.org/licenses/BSD-3-Clause")),
  publishMavenStyle := true,
  publishTo := sonatypePublishToBundle.value,
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/electronicarts/kara"),
      "scm:git:git@github.com:electornicarts/kara.git"
    )
  ),
  pomExtra :=
    <developers>
      <developer>
        <name>Electronic Arts Inc.</name>
        <url>https://ea.com</url>
      </developer>
    </developers>
)

lazy val root = (project in file("."))
  .enablePlugins(ScriptedPlugin)
  .settings(
    publishSettings,
    addSbtPlugin("com.twitter" % "scrooge-sbt-plugin" % finagleVersion),
    libraryDependencies ++= Seq(
      "commons-io"            % "commons-io"     % "2.8.0",
      "org.scalatra.scalate" %% "scalate-core"   % "1.9.6",
      "com.twitter"          %% "finagle-http"   % finagleVersion % Test,
      "io.circe"             %% "circe-yaml"     % "0.13.1",
      "com.github.pathikrit" %% "better-files"   % "3.9.1",
      "io.swagger.parser.v3"  % "swagger-parser" % "2.0.21"       % Test,
      "org.scalatest"        %% "scalatest"      % "3.2.1"        % Test
    ),
    scriptedLaunchOpts ++= Seq(
      "-Xmx1024M",
      "-Dplugin.version=" + version.value
    ) ++ sbtOps,
    scriptedBufferLog := false,
    name := "kara",
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
