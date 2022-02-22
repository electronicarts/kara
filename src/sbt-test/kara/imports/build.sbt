/*
 * Copyright (C) 2022 Electronic Arts Inc.  All rights reserved.
 */

import sbt._

val twitterVersion = "20.10.0"
val circeVersion   = "0.14.1"

lazy val root = (project in file("."))
  .settings(
    scalaVersion := "2.12.15",
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
