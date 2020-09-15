/*
 * Copyright (C) 2020 Electronic Arts Inc.  All rights reserved.
 */

import sbt._

val twitterVersion = "19.8.0"
val circeVersion   = "0.11.1"

lazy val root = (project in file("."))
  .settings(
    scalaVersion := "2.12.8",
    libraryDependencies ++= Seq(
      "com.twitter" %% "twitter-server" % twitterVersion,
      "com.twitter" %% "scrooge-core"   % twitterVersion,
      "io.circe"    %% "circe-generic"  % circeVersion,
      "io.circe"    %% "circe-parser"   % circeVersion
    ),
    karaServices := Seq(
      "com.local.StubService"
    )
  )
  .enablePlugins(Kara)
