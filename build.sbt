/*
 * Copyright (C) 2022 Electronic Arts Inc.  All rights reserved.
 */

import sbt._

val finagleVersion = "20.10.0"
val circeVersion   = "0.14.1"

ThisBuild / credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
ThisBuild / envVars ++= Map("CI_PROJECT_DIR" -> sys.env.getOrElse("CI_PROJECT_DIR", "."))
ThisBuild / scalaVersion := "2.12.15"
ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-parser-combinators" % VersionScheme.Always
ThisBuild / versionScheme := Some(VersionScheme.EarlySemVer)

lazy val sbtOps = sys.env
  .get("SBT_OPTS")
  .map(_.split(" "))
  .getOrElse(Array.empty)

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
      "org.scalatra.scalate" %% "scalate-core"   % "1.9.7",
      "com.twitter"          %% "finagle-http"   % finagleVersion % Test,
      "io.circe"             %% "circe-yaml"     % circeVersion,
      "com.github.pathikrit" %% "better-files"   % "3.9.1",
      "io.swagger.parser.v3"  % "swagger-parser" % "2.0.21"       % Test,
      "org.scalatest"        %% "scalatest"      % "3.2.1"        % Test,
    ),
    scriptedLaunchOpts ++= Seq(
      "-Xmx1024M",
      "-Dplugin.version=" + version.value
    ) ++ sbtOps,
    scriptedBufferLog := false,
    name := "kara",
    sbtPlugin := true
  )

lazy val docs = project
  .in(file("mdoc"))
  .settings(
    publish / skip := true,
    mdocOut := file("."),
    mdocVariables := Map(
      "VERSION" -> version.value.stripSuffix("-SNAPSHOT")
    )
  )
  .enablePlugins(MdocPlugin)

addCommandAlias("codecov", "; clean; reload; coverage; test; coverageReport; coverageAggregate; coverageOff")
addCommandAlias("fmt", "; scalafmtAll")