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
      "com.dirty.StubService"
    ),
    TaskKey[Unit]("check") := {
      // Check sources are regenerated
      val newSource = ((sourceManaged in Compile).value ** "package.scala").get.headOption
        .getOrElse(sys.error("Cannot find generated stub service"))

      val sourceContent = IO.read(newSource)
      if (!sourceContent.contains("MyNewStruct"))
        sys.error("Source files were not re-generated as expected")

      // Check resources are regenerated
      val newResource = ((resourceManaged in Compile).value ** "service.oas").get.headOption
        .getOrElse(sys.error("Cannot find generated stub service"))

      val resourceContent = IO.read(newResource)
      if (!resourceContent.contains("MyNewStruct"))
        sys.error("Resource files were not re-generated as expected")
    }
  )
  .enablePlugins(Kara)
