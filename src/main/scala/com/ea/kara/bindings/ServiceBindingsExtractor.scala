/*
 * Copyright (C) 2021 Electronic Arts Inc.  All rights reserved.
 */

package com.ea.kara.bindings

import com.twitter.scrooge.ast.{Document, Function, Service}
import com.ea.kara.CodegenContext
import com.ea.kara.extensions._

class ServiceBindingsExtractor(
  ctx: CodegenContext,
  pkg: String,
  service: Service,
  document: Document,
  thriftServiceName: String,
  httpServiceName: String
) {

  def bindings: Map[String, Any] = {
    val functionBindings = extractFunctionBindings(service.functions)

    Map(
      "httpServiceName"          -> httpServiceName,
      "thriftServiceName"        -> thriftServiceName,
      "package"                  -> pkg,
      "filePackages"             -> ctx.allOutPackages,
      "thriftNamespace"          -> document.javaNamespace,
      "thriftIncludedNamespaces" -> document.includedDocuments.map(_.javaNamespace),
      "zeroArgFunctions"         -> functionBindings.zeroArg,
      "oneArgFunctions"          -> functionBindings.oneArg,
      "multiArgFunctions"        -> functionBindings.multiArg
    )
  }

  private def extractFunctionBindings(functions: Seq[Function]): FunctionBindings = {
    val allFunctions = functions.map { function =>
      val functionName = function.originalName
      val functionType = function.funcType.scalaType(document)

      val exceptions = function.throws.sortBy(_.originalName).map { t =>
        Map(
          "name" -> t.originalName,
          "type" -> t.fieldType.scalaType(document)
        )
      }

      val args = function.args.map { arg =>
        Map(
          "name" -> arg.originalName,
          "type" -> arg.fieldType.scalaType(document)
        )
      }

      Map(
        "name"         -> functionName,
        "responseType" -> functionType,
        "exceptions"   -> exceptions
      ) ++ {
        args match {
          case Nil         => Map.empty
          case Seq(oneArg) => Map("arg" -> oneArg)
          case multiArg    => Map("args" -> multiArg, "numberOfFields" -> multiArg.size)
        }
      }
    }

    val (multiArg, zeroOrOneArg) = allFunctions.partition(_.contains("args"))
    val (oneArg, zeroArg)        = zeroOrOneArg.partition(_.contains("arg"))

    FunctionBindings(zeroArg, oneArg, multiArg)
  }
}

case class FunctionBindings private (
  zeroArg: Seq[Map[String, Any]],
  oneArg: Seq[Map[String, Any]],
  multiArg: Seq[Map[String, Any]]
)
