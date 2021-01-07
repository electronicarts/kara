/*
 * Copyright (C) 2021 Electronic Arts Inc.  All rights reserved.
 */

package com.ea.kara.oas

import com.twitter.scrooge.ast._
import io.circe._
import io.circe.syntax._
import com.ea.kara.extensions._

class OASBuilder(serviceDocument: Document, serviceName: String, karaHeaders: Seq[String]) {

  private val defaultJsonFailureSchemaName = "DefaultJsonFailureResponse"
  private val service: Service =
    serviceDocument.services.find(_.sid.name == serviceName).getOrElse {
      throw new RuntimeException(
        s"""
           |Document has no service [$serviceName].
           |Available services [${serviceDocument.services.map(_.sid.name)}]."
       """.stripMargin
      )
    }

  def build(): OAS =
    OAS(buildJson())

  private def buildJson(): Json =
    Json.obj(
      "openapi"    -> OAS.Version.asJson,
      "info"       -> buildInfoJson(),
      "paths"      -> buildPathsJson(),
      "components" -> buildComponentsJson()
    )

  private def buildInfoJson(): Json =
    Json.obj(
      "title"   -> s"${service.sid.name} API Overview".asJson,
      "version" -> "v1".asJson
    )

  private def buildPathsJson(): Json =
    service.functions
      .map { function =>
        s"/${function.funcName.name}" -> Json.obj(
          "post" -> Json.obj(
            "responses" -> buildResponsesJson(
              function.funcType,
              function.throws.map(_.fieldType),
              serviceDocument
            ),
            "requestBody" -> {
              function.args match {
                case Nil =>
                  Json.obj()
                case Seq(oneArg) =>
                  buildRequestBodyFromSingleArg(oneArg, serviceDocument)
                case multiArg =>
                  buildRequestBodyFromMultipleArgs(multiArg, serviceDocument)
              }
            },
            "parameters" -> buildRequestHeaders()
          )
        )
      }
      .toMap
      .asJson

  private def buildRequestHeaders(): Json =
    Json.fromValues {
      karaHeaders.map { header =>
        Json.obj(
          "in"   -> "header".asJson,
          "name" -> header.asJson,
          "schema" -> Json.obj(
            "type"     -> "string".asJson,
            "required" -> "false".asJson
          )
        )
      }
    }

  private def buildComponentsJson(): Json =
    Map(
      "schemas" -> buildSchemasJson()
    ).asJson

  private def buildSchemasJson(): Json = {
    val importEnums = serviceDocument.includes
      .flatMap { include =>
        include.document.enums.map((include.document, _))
      }
    val importStructLikes = serviceDocument.includes
      .flatMap { include =>
        include.document.structs.map((include.document, _))
      }
    val importTypedefs = serviceDocument.includes
      .flatMap { include =>
        include.document.defs.collect {
          case typedef: Typedef => (include.document, typedef)
        }
      }

    val localEnums       = serviceDocument.enums.map((serviceDocument, _))
    val localStructLikes = serviceDocument.structs.map((serviceDocument, _))
    val localTypedefs = serviceDocument.defs.collect {
      case typedef: Typedef => (serviceDocument, typedef)
    }

    val allEnums       = importEnums ++ localEnums
    val allStructLikes = importStructLikes ++ localStructLikes
    val allTypedefs    = importTypedefs ++ localTypedefs

    val enumSchemas = allEnums
      .map {
        case (document, enum) =>
          enum.sid.qualifiedIn(document) -> buildEnumSchema(enum)
      }
      .toMap
      .asJson

    val structLikeSchemas = allStructLikes
      .map {
        case (document, struct) =>
          struct.sid.qualifiedIn(document) -> buildStructLikeSchema(struct, document)
      }
      .toMap
      .asJson

    val typedefSchemas = allTypedefs
      .map {
        case (document, typedef) =>
          typedef.sid.qualifiedIn(document) -> buildTypedefSchema(typedef, document)
      }
      .toMap
      .asJson

    val defaultExceptionSchema = buildDefaultExceptionSchema()

    enumSchemas
      .deepMerge(structLikeSchemas)
      .deepMerge(typedefSchemas)
      .deepMerge(defaultExceptionSchema)
  }

  private def buildEnumSchema(enum: Enum): Json =
    Json.obj(
      "type" -> "string".asJson,
      "enum" -> enum.values.map(_.sid.originalName).asJson
    )

  private def buildStructLikeSchema(structLike: StructLike, document: Document): Json =
    structLike match {
      case struct: Struct => buildFieldsSchema(struct.fields, document)
      case ex: Exception_ => buildFieldsSchema(ex.fields, document)
      case union: Union   => buildUnionSchema(union, document)
      case _              => Json.obj() // TODO: support other StructLike implementations
    }

  private def buildTypedefSchema(typedef: Typedef, document: Document): Json =
    buildFunctionTypeJson(typedef.fieldType, document)

  private def buildFieldsSchema(fields: Seq[Field], document: Document): Json = {
    val requiredFields = fields.filter(!_.requiredness.isOptional)

    Json.obj(
      "required"   -> requiredFields.map(_.sid.originalName).asJson,
      "properties" -> buildFieldsJson(fields, document)
    )
  }

  private def buildFieldsJson(fields: Seq[Field], document: Document): Json =
    fields
      .map { field =>
        field.originalName -> buildFunctionTypeJson(field.fieldType, document)
      }
      .toMap
      .asJson

  private def buildFunctionTypeJson(functionType: FunctionType, document: Document): Json =
    functionType match {
      // TODO: properly support Void and OnewayVoid
      case Void | OnewayVoid            => Json.obj("type" -> "string".asJson)
      case TBool                        => Json.obj("type" -> "boolean".asJson)
      case TByte                        => Json.obj("type" -> "string".asJson, "format" -> "byte".asJson)
      case TI16 | TI32                  => Json.obj("type" -> "integer".asJson, "format" -> "int32".asJson)
      case TI64                         => Json.obj("type" -> "integer".asJson, "format" -> "int64".asJson)
      case TDouble                      => Json.obj("type" -> "number".asJson, "format" -> "double".asJson)
      case TString                      => Json.obj("type" -> "string".asJson)
      case TBinary                      => Json.obj("type" -> "string".asJson, "format" -> "binary".asJson)
      case namedType: NamedType         => buildNamedTypeJson(namedType, document)
      case mapType: MapType             => buildMapTypeJson(mapType, document)
      case setType: SetType             => buildSetTypeJson(setType, document)
      case listType: ListType           => buildListTypeJson(listType, document)
      case referenceType: ReferenceType => buildReferenceTypeJson(referenceType, document)
    }

  private def buildObjectTypeFromArgsJson(args: Seq[Field], document: Document): Json =
    Json
      .obj(
        "type"       -> "object".asJson,
        "required"   -> args.filter(!_.requiredness.isOptional).map(_.originalName).asJson,
        "properties" -> buildFieldsJson(args, document)
      )

  private def buildNamedTypeJson(namedType: NamedType, document: Document): Json =
    Json.obj(
      "$ref" -> s"#/components/schemas/${namedType.sid.qualifiedIn(document)}".asJson
    )

  private def buildMapTypeJson(mapType: MapType, document: Document): Json =
    Json.obj(
      "type"                 -> "object".asJson,
      "additionalProperties" -> buildFunctionTypeJson(mapType.valueType, document)
    )

  private def buildSetTypeJson(setType: SetType, document: Document): Json =
    Json.obj(
      "type"  -> "array".asJson,
      "items" -> buildFunctionTypeJson(setType.eltType, document)
    )

  private def buildListTypeJson(listType: ListType, document: Document): Json =
    Json.obj(
      "type"  -> "array".asJson,
      "items" -> buildFunctionTypeJson(listType.eltType, document)
    )

  private def buildReferenceTypeJson(referenceType: ReferenceType, document: Document): Json =
    Json.obj(
      "$ref" -> s"#/components/schemas/${referenceType.id.qualifiedIn(document)}".asJson
    )

  private def buildUnionSchema(union: Union, document: Document): Json =
    Json.obj(
      "oneOf" -> Json.arr(
        union.fields.map(field => buildFunctionTypeJson(field.fieldType, document)): _*
      )
    )

  private def buildRequestBodyFromSingleArg(arg: Field, document: Document): Json =
    Json.obj(
      "required" -> (!arg.requiredness.isOptional).asJson,
      "content" -> Json.obj(
        "application/json" -> Json.obj(
          "schema" -> Json.obj(
            "oneOf" -> Json.arr(
              // This specifies that, given a single-param request,
              // where param is named 'person' and having schema
              // 'Person(name: String, surname: String)',
              // both the following requests are valid:
              // {
              //   "person": { "name": "Foo", "surname": "Bar" }
              // }
              // and
              // { "name": "Foo", "surname": "Bar" }
              buildFunctionTypeJson(arg.fieldType, document),
              buildObjectTypeFromArgsJson(Seq(arg), document)
            )
          )
        )
      )
    )

  private def buildRequestBodyFromMultipleArgs(args: Seq[Field], document: Document): Json =
    Json.obj(
      "required" -> "true".asJson,
      "content" -> Json.obj(
        "application/json" -> Json.obj(
          "schema" -> buildObjectTypeFromArgsJson(args, document)
        )
      )
    )

  private def buildResponsesJson(
    functionType: FunctionType,
    exceptions: Seq[FieldType],
    document: Document
  ): Json =
    Json
      .obj(
        "200" -> Json
          .obj(
            "description" -> "Successful response".asJson,
            "content" -> Json
              .obj(
                "application/json" -> Map(
                  "schema" -> buildFunctionTypeJson(functionType, document)
                ).asJson
              )
              .asJson
          )
          .asJson,
        "500" -> Json
          .obj(
            "description" -> "Internal Error".asJson,
            "content"     -> jsonErrorContent(exceptions, document)
          )
          .asJson
      )
      .asJson

  private def buildDefaultExceptionSchema(): Json =
    Json.obj(
      defaultJsonFailureSchemaName -> Json.obj(
        "required" -> Json.arr("message".asJson),
        "properties" -> Json.obj(
          "message" -> Json.obj(
            "type" -> "string".asJson
          )
        )
      )
    )

  // Unfortunately this will not show up in the UI but is valid wrt the spec
  // See https://github.com/swagger-api/swagger-ui/issues/3803
  private def jsonErrorContent(
    exceptions: Seq[FieldType],
    document: Document
  ): Json = {
    val defaultExceptionSchemaRef = Json.obj(
      "$ref" -> s"#/components/schemas/$defaultJsonFailureSchemaName".asJson
    )

    Json.obj(
      "application/json" -> Json.obj(
        "schema" -> Json.obj(
          "oneOf" -> Json.arr(
            (exceptions
              .map(buildFunctionTypeJson(_, document))
              :+ defaultExceptionSchemaRef): _*
          )
        )
      )
    )
  }
}
