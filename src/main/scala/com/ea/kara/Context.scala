/*
 * Copyright (C) 2020 Electronic Arts Inc.  All rights reserved.
 */

package com.ea.kara

import java.io.File

import com.twitter.scrooge.ast._
import com.ea.kara.extensions._
import com.ea.kara.parse.ThriftParser

case class ThriftFileContext(
  name: String,
  normalizedName: String,
  outDirectory: File,
  outPackage: String,
  document: Document
)

case class CodegenContext(
  baseOutPackage: String,
  thriftIncludes: Seq[File],
  thriftSources: Seq[File],
  serviceNames: Seq[String],
  outPath: File
) {
  private def packageForFile(basePackage: String, fileName: String): String =
    basePackage + "." + fileName.normalized

  private val parser = new ThriftParser(thriftIncludes)

  val thriftFiles: Seq[ThriftFileContext] =
    thriftSources
      .map(_.getName)
      .filter(_.endsWith(".thrift"))
      .map { name =>
        ThriftFileContext(
          name = name,
          normalizedName = name.normalized,
          outDirectory =
            new File(outPath, packageForFile(baseOutPackage, name).replace(".", File.separator)),
          outPackage = packageForFile(baseOutPackage, name),
          document = parser.parse(name)
        )
      }

  val servicesByDocument: Map[Document, Seq[String]] = {
    val servicePackageAndName: Map[String, Seq[String]] = serviceNames
      .map { serviceName =>
        serviceName.lastIndexOf(".") match {
          case -1    => ("", serviceName)
          case index => serviceName.splitAt(index)
        }
      }
      .groupBy(_._1)
      .map {
        case (k, v) => (k, v.map(_._2.tail))
      }

    val servicesByDocument = thriftFiles
      .flatMap(file =>
        file.document.services.map(service => (file.document, service)).filter {
          case (document, service) =>
            servicePackageAndName
              .get(document.javaNamespace)
              .exists { serviceNames =>
                serviceNames.contains(service.sid.name)
              }
        }
      )
      .groupBy(_._1)
      .map {
        case (k, v) => (k, v.map(_._2.sid.name))
      }

    servicesByDocument
  }

  val allOutPackages: Seq[String] = thriftFiles.map(_.outPackage).distinct
}
