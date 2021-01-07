/*
 * Copyright (C) 2021 Electronic Arts Inc.  All rights reserved.
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
            throw new RuntimeException("Provided service names should be qualified by packages.")
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

    val pkgToSvc = servicePackageAndName.flatMap {
      case (pkg, names) => names.map((pkg, _))
    }
    val servicesByDocument: Map[Document, Seq[String]] = pkgToSvc
      .map {
        case (pkg, name) =>
          docToSvc
            .find {
              case (doc, docSvc) =>
                doc.javaNamespace == pkg &&
                  docSvc.sid.name == name
            }
            .getOrElse {
              throw new RuntimeException(s"No service '$name' found in package '$pkg'.")
            }
      }
      .groupBy(_._1)
      .map {
        case (k, v) => (k, v.map(_._2.sid.name).toSeq)
      }

    servicesByDocument
  }

  val allOutPackages: Seq[String] = thriftFiles.map(_.outPackage).distinct
}
