/*
 * Copyright (C) 2021 Electronic Arts Inc.  All rights reserved.
 */

def buildSuffix: String = {
  val isSnapshot = sys.props.get("kara.isSnapshot").exists(_.toBoolean)
  if (isSnapshot) "-SNAPSHOT" else ""
}

version in ThisBuild := "0.2.0" + buildSuffix
