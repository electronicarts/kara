/*
 * Copyright (C) 2020 Electronic Arts Inc.  All rights reserved.
 */

// Make 'version' a SNAPSHOT only when publishing on a developer machine,
// or when it's forced in CI via 'kara.isSnapshot' flag.
def buildSuffix: String = {
  val isSnapshotForced = sys.props.get("kara.isSnapshot").exists(_.toBoolean)
  val isNotCI          = Option(java.lang.System.getenv("BUILD_NUMBER")).isEmpty
  if (isSnapshotForced || isNotCI) "-SNAPSHOT" else ""
}

version in ThisBuild := "0.1.0" + buildSuffix
