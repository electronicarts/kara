/*
 * Copyright (C) 2022 Electronic Arts Inc.  All rights reserved.
 */

libraryDependencies += "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value
addSbtPlugin("com.github.sbt"           % "sbt-pgp"         % "2.1.2")
addSbtPlugin("org.scalameta"            % "sbt-scalafmt"    % "2.4.6")
addSbtPlugin("org.scalameta"            % "sbt-mdoc"        % "2.3.0")
addSbtPlugin("org.scoverage"            % "sbt-scoverage"   % "1.9.3")
addSbtPlugin("org.xerial.sbt"           % "sbt-sonatype"    % "3.9.11")
