/*
 * Copyright (C) 2022 Electronic Arts Inc.  All rights reserved.
 */

package com.ea.kara

import com.twitter.scrooge.ast._

package object extensions {
  implicit class DocumentOps(document: Document) {
    /*
    TODO: Kara expects Java namespaces to be set in .thrift files.
    TODO: This might be a reasonable assumption, but then we'd need to make it obvious with a meaningful error message.
     */
    val javaNamespace: String        = document.namespace("java").get.fullName
    val underscoredNamespace: String = javaNamespace.replace(".", "_")
    val includes: Seq[Include] = document.headers.collect {
      case include: Include => include
    }
    val includedDocuments: Seq[Document] = includes.map(_.document)
  }

  implicit class FunctionTypeOps(functionType: FunctionType) {
    def scalaType(document: Document): String =
      functionType match {
        case TBool =>
          "Boolean"
        case TByte =>
          "Byte"
        case TI16 =>
          "Short"
        case TI32 =>
          "Int"
        case TI64 =>
          "Long"
        case TDouble =>
          "Double"
        case TString =>
          "String"
        case TBinary =>
          "ByteBuffer"
        case EnumType(enum, _) =>
          enum.sid.fullName
        case SetType(eltType, _) =>
          s"Set[${eltType.scalaType(document)}]"
        case ListType(eltType, _) =>
          s"List[${eltType.scalaType(document)}]"
        case MapType(keyType, valueType, _) =>
          s"Map[${keyType.scalaType(document)},${valueType.scalaType(document)}]"
        case Void | OnewayVoid =>
          "Unit"
        case StructType(struct, _) =>
          struct.sid.toTitleCase.fullName
        case ReferenceType(id) =>
          id.qualifiedIn(document)
      }
  }

  implicit class StringOps(string: String) {
    val escapingReserved: String = {
      string match {
        case "abstract" | "case" | "catch" | "class" | "def" | "do" | "else" | "extends" | "false" |
            "final" | "finally" | "for" | "forSome" | "if" | "implicit" | "import" | "lazy" |
            "match" | "new" | "null" | "object" | "override" | "package" | "private" | "protected" |
            "return" | "sealed" | "super" | "this" | "throw" | "true" | "type" | "val" | "var" |
            "while" | "with" | "yield" =>
          s"`$string`"
        case otherString => otherString
      }
    }

    val toHttpServiceName: String = "Http" + string

    val withScalaExtension: String = string + ".scala"

    val normalized: String = string.toLowerCase
      .stripSuffix(".thrift")
      .replaceAll("[^\\x00-\\xFF]", "$")
      .trim()
  }

  implicit class IdentifierOps(identifier: Identifier) {
    def qualifiedIn(document: Document): String =
      identifier match {
        case simpleId: SimpleID =>
          val namespace = document.javaNamespace
          namespace + "." + simpleId.fullName
        case qualifiedId: QualifiedID =>
          val simpleName = qualifiedId.name.fullName
          document.includes
            .find(_.prefix == qualifiedId.qualifier)
            .map { include =>
              include.document.javaNamespace + "." + simpleName
            }
            .getOrElse(simpleName)
      }
  }
}
