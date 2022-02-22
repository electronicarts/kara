/*
 * Copyright (C) 2022 Electronic Arts Inc.  All rights reserved.
 */

def buildSuffix: String = {
  val isSnapshot = sys.props.get("kara.isSnapshot").exists(_.toBoolean)
  if (isSnapshot) "-SNAPSHOT" else ""
}

ThisBuild / version := "0.2.1" + buildSuffix
