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
      "com.dirty.StubService"
    ),
    TaskKey[Unit]("check") := {
      // Check sources are regenerated
      val newSource = ((Compile / sourceManaged).value ** "package.scala").get.headOption
        .getOrElse(sys.error("Cannot find generated stub service"))

      val sourceContent = IO.read(newSource)
      if (!sourceContent.contains("MyNewStruct"))
        sys.error("Source files were not re-generated as expected")

      // Check resources are regenerated
      val newResource = ((Compile / resourceManaged).value ** "service.oas").get.headOption
        .getOrElse(sys.error("Cannot find generated stub service"))

      val resourceContent = IO.read(newResource)
      if (!resourceContent.contains("MyNewStruct"))
        sys.error("Resource files were not re-generated as expected")
    }
  )
  .enablePlugins(Kara)
