/*
 * Copyright (C) 2020 Electronic Arts Inc.  All rights reserved.
 */

package com.ea.kara.oas

import io.circe.Json
import io.circe.yaml.syntax._

object OAS {
  val Version = "3.0.0"
}

case class OAS(json: Json) {
  def asJsonString(): String = json.toString
  def asYamlString(): String = json.asYaml.spaces2.toString
}
