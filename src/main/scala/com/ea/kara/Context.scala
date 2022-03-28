/*
 * Copyright (C) 2022 Electronic Arts Inc.  All rights reserved.
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
          case -1 =>
            throw new RuntimeException(
              s"Provided service names should be qualified by packages, but found $serviceName."
            )
          case index =>
            serviceName.splitAt(index)
        }
      }
      .groupBy(_._1)
      .map {
        case (k, v) => (k, v.map(_._2.tail))
      }

    val docToSvc = thriftFiles
      .flatMap(file => file.document.services.map(service => (file.document, service)))

    val servicesByDocument: Map[Document, Seq[String]] = servicePackageAndName
      .toList
      .flatMap { case (pkg, svcNames) => svcNames.map((pkg, _)) }
      .map { case (pkg, svcName) =>
        docToSvc
          .find {
            case (doc, docSvc) =>
              doc.javaNamespace == pkg &&
              docSvc.sid.name == svcName
          }
          .getOrElse {
            throw new RuntimeException(
              s"No service '$svcName' found in package '$pkg'." +
              "Valid alternatives are: " +
              docToSvc
                .filter { case (doc, _) => doc.javaNamespace == pkg }
                .map { case (_, svc) =>  svc.sid.name }
                .mkString(", ")
            )
          }
      }
      .groupBy(_._1)
      .map { case (k, v) => (k, v.map(_._2.sid.name)) }

    servicesByDocument
  }

  val allOutPackages: Seq[String] = thriftFiles.map(_.outPackage).distinct
}
