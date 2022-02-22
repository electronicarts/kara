/*
 * Copyright (C) 2022 Electronic Arts Inc.  All rights reserved.
 */

package com.ea.kara

import java.io.File

import Constants._
import sbt.util.Logger
import com.ea.kara.bindings.{DataTypeBindingsExtractor, ServiceBindingsExtractor}
import com.ea.kara.extensions._
import com.ea.kara.oas.OASBuilder
import com.ea.kara.write.{ResourceWriter, SourceWriter}

class Generator(
  thriftIncludes: Seq[File],
  thriftSources: Seq[File],
  serviceNames: Seq[String],
  karaHeaders: Seq[String],
  val sourcePath: File,
  val resourcePath: File
)(implicit val logger: Logger) {

  private val ctx = CodegenContext(
    baseOutPackage = BaseOutPackage,
    thriftIncludes = thriftIncludes,
    thriftSources = thriftSources,
    serviceNames = serviceNames,
    outPath = sourcePath
  )

  private val supportedStaticResources = Seq(
    "favicon-16x16.png",
    "favicon-32x32.png",
    "index.html",
    "oauth2-redirect.html",
    "swagger-ui.css",
    "swagger-ui.js",
    "swagger-ui-bundle.js",
    "swagger-ui-standalone-preset.js"
  )

  def generateResources(): Unit = {
    val resourceWriter = new ResourceWriter(resourcePath)

    for {
      (document, serviceNames) <- ctx.servicesByDocument
      serviceName              <- serviceNames
    } yield {
      val oas = new OASBuilder(document, serviceName, karaHeaders).build()

      resourceWriter.writeStaticContent(supportedStaticResources)
      resourceWriter.writeOAS(document.javaNamespace, serviceName, oas)
    }
  }

  def generateSources(): Unit = {
    ctx.thriftFiles.foreach { thriftFile =>
      val dataTypeBindings = new DataTypeBindingsExtractor(
        ctx = ctx,
        pkgName = thriftFile.normalizedName,
        document = thriftFile.document
      ).bindings

      val sourceWriter = new SourceWriter(thriftFile.outDirectory)

      sourceWriter.write(
        fileName = PackageObjectFilename,
        template = PackageObjectTemplateFilename,
        bindings = dataTypeBindings
      )
    }

    for {
      document     <- ctx.servicesByDocument.keySet
      thriftFile   <- ctx.thriftFiles.find(_.document == document)
      serviceNames <- ctx.servicesByDocument.get(document)
    } yield {
      val services =
        thriftFile.document.services.filter(service => serviceNames.contains(service.sid.name))

      logger.debug(
        s"Generating services [${services.map(_.sid.name)}] for Thrift file ${thriftFile.name}..."
      )

      services.foreach { service =>
        val thriftServiceName = service.sid.fullName
        val httpServiceName   = thriftServiceName.toHttpServiceName

        val serviceBindings = new ServiceBindingsExtractor(
          ctx = ctx,
          pkg = thriftFile.outPackage,
          document = document,
          service = service,
          thriftServiceName = thriftServiceName,
          httpServiceName = httpServiceName
        ).bindings

        val sourceWriter = new SourceWriter(thriftFile.outDirectory)

        sourceWriter.write(
          fileName = httpServiceName.withScalaExtension,
          template = ServiceTemplateFilename,
          bindings = serviceBindings
        )
      }

      logger.debug(s"Finagle services for Thrift file ${thriftFile.name} generated.")
    }
  }
}
