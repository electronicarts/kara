/*
 * Copyright (C) 2021 Electronic Arts Inc.  All rights reserved.
 */

package com.ea.kara.bindings

import com.twitter.scrooge.ast._
import com.ea.kara.CodegenContext
import com.ea.kara.extensions._

class DataTypeBindingsExtractor(ctx: CodegenContext, pkgName: String, document: Document) {
  private val (structsWithArgsAndUnions, structsWithNoArgs) =
    document.structs.partition(_.fields.nonEmpty)
  private val (unions, structsWithArgs) = structsWithArgsAndUnions.partition(_.isInstanceOf[Union])
  private val allOutPackagesExcludingCurrent =
    ctx.allOutPackages.filterNot(
      ctx.thriftFiles.find(_.document == document).map(_.outPackage).contains
    )

  def bindings: Map[String, Object] =
    Map(
      "package"                    -> ctx.baseOutPackage,
      "filePackages"               -> allOutPackagesExcludingCurrent,
      "normalizedThriftFileName"   -> pkgName,
      "thriftNamespace"            -> document.javaNamespace,
      "underscoredThriftNamespace" -> document.underscoredNamespace,
      "structs"                    -> extractStructBindings(structsWithArgs),
      "noArgs"                     -> extractStructBindings(structsWithNoArgs),
      "unions"                     -> extractUnionBindings(unions),
      "enums"                      -> extractEnumBindings(document.enums)
    )

  private def extractEnumBindings(enums: Seq[Enum]): Seq[Map[String, Any]] =
    enums.map { enum =>
      Map(
        "name" -> enum.sid.name
      )
    }

  private def extractStructBindings(structs: Seq[StructLike]): Seq[Map[String, Any]] =
    structs.map { struct =>
      val structName           = struct.sid.toTitleCase.fullName
      val lowerCasedStructName = structName.toLowerCase().escapingReserved

      val fields         = struct.fields
      val numberOfFields = fields.size

      val requiredArgBindings = fields.map { arg =>
        val argType     = arg.fieldType.scalaType(document)
        val name        = arg.originalName
        val escapedName = name.escapingReserved

        Map(
          "type"        -> argType,
          "name"        -> name,
          "escapedName" -> escapedName
        )
      }

      Map(
        "type"           -> structName,
        "lowerCasedType" -> lowerCasedStructName,
        "fields"         -> requiredArgBindings,
        "numberOfFields" -> numberOfFields
      )
    }

  private def extractUnionBindings(structs: Seq[StructLike]): Seq[Map[String, Any]] =
    structs.map { struct =>
      val structName           = struct.sid.toTitleCase.fullName
      val lowerCasedStructName = structName.toLowerCase()

      val numberOfArgs = struct.fields.size
      val fieldBindings = struct.fields.map { arg =>
        val argName            = arg.originalName
        val capitalizedArgName = argName.capitalize

        Map(
          "capitalizedName" -> capitalizedArgName
        )
      }

      Map(
        "type"           -> structName,
        "lowerCasedType" -> lowerCasedStructName,
        "fields"         -> fieldBindings,
        "numberOfArgs"   -> numberOfArgs
      )
    }
}
