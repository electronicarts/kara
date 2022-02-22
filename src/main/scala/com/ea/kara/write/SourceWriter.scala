/*
 * Copyright (C) 2022 Electronic Arts Inc.  All rights reserved.
 */

package com.ea.kara.write

import java.io.{File, FileWriter}

import org.fusesource.scalate.TemplateEngine
import sbt.util.Logger

class SourceWriter(outPath: File)(implicit val logger: Logger) {

  private val engine = new TemplateEngine()

  def write(fileName: String, template: String, bindings: Map[String, Any]): Unit = {
    val file = new File(outPath, fileName)

    logger.debug(s"Creating source parent directory [$outPath].")
    outPath.mkdirs()

    val layout = engine.layout(template, bindings)

    logger.debug(s"Creating package file [$file].")
    file.createNewFile()

    val writer = new FileWriter(file)

    writer.write(layout)
    writer.close()
  }
}
