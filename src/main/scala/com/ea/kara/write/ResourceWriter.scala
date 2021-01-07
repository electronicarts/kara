/*
 * Copyright (C) 2021 Electronic Arts Inc.  All rights reserved.
 */

package com.ea.kara.write

import java.io.{File, FileWriter}
import java.nio.charset.Charset

import org.apache.commons.io.IOUtils
import sbt.util.Logger
import com.ea.kara.oas.OAS
import com.ea.kara.Constants.ServiceOasFilename

class ResourceWriter(outPath: File)(implicit val logger: Logger) {

  val swaggerPath = new File(outPath, "swagger")

  def writeStaticContent(resourceNames: Seq[String]): Unit = {
    logger.debug(s"Creating resource parent directory [$swaggerPath].")
    swaggerPath.mkdirs()

    resourceNames.foreach { resourceName =>
      val resourceStream =
        getClass.getClassLoader.getResource(s"swagger/$resourceName").openStream()
      val resourceContent = IOUtils.toCharArray(resourceStream, Charset.defaultCharset())
      resourceStream.close()

      val destinationFile = new File(swaggerPath, resourceName)
      logger.debug(s"Writing content of resource $resourceName to $destinationFile.")
      destinationFile.createNewFile()

      val fileWriter = new FileWriter(destinationFile)
      fileWriter.write(resourceContent)

      fileWriter.close()
    }
  }

  def writeOAS(pkg: String, serviceName: String, oas: OAS): Unit = {
    val currentServicePath = new File(swaggerPath, s"$pkg.$serviceName")

    logger.debug(s"Creating OAS directory for service $serviceName [$currentServicePath].")
    currentServicePath.mkdirs()

    val oasFile = new File(currentServicePath, ServiceOasFilename)

    logger.debug(s"Writing OAS file for service $serviceName to $oasFile.")
    oasFile.createNewFile()

    val writer = new FileWriter(oasFile)
    writer.write(oas.asYamlString())

    writer.close()
  }
}
