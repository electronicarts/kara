/*
 * Copyright (C) 2022 Electronic Arts Inc.  All rights reserved.
 */

namespace java com.local

include "test_import.thrift"

struct ANestedStruct {

}

# A Thrift Enum to play with different casings
enum AnEnum {
  lowercase,
  UPPERCASE,
  PascalCase,
  snake_case,
  else # reserved keyword
}

union AnUion {
  1: string aString
  2: ANestedStruct aStruct
}

struct AStruct {
    1: string aString
    2: bool aBool
    3: byte aByte
    4: i16 anI16
    5: i32 anI32
    6: i64 anI64
    7: double aDouble
    8: binary binaryData
    9: list<string> aListOfPrimitives
    10: list<ANestedStruct> aListOfStructs
    11: map<string, string> aMapOfPrimitives
    12: map<string, ANestedStruct> aMapOfStructs
    13: optional string anOptionalOfPrimitive
    14: optional AnEnum anOptionalOfEnum
    15: AnUion anUnion
    16: test_import.ImportedStruct importedStruct
    17: string object # reserved Scala keyword
}

struct EmptyStruct {

}

struct BooleanWrapperStruct {
    1: bool isTrue
}

exception AnException {
    1: string message
    2: i16 errorCode
}

service TestService {
    # test most of the flow, in particular most combinations
    # of data types' encoding and decoding.
    # Where A is an AStuct's JSON representation, expected outcome is:
    # thriftToJson(thriftService(jsonToThrift(A))) == A
    AStruct mirror(1: AStruct one)
    # test encoding and decoding of empty structs, as well as wrapped one-arg methods.
    EmptyStruct oneArg(1: EmptyStruct one)
    # test exception modeling.
    void throwing(1: BooleanWrapperStruct isKnown) throws(1: AnException one, 2: test_import.ImportedException two)
    # test no-arg methods.
    EmptyStruct noArg()
    # test multi-arg methods.
    EmptyStruct multiArg(1: EmptyStruct one, 2: EmptyStruct two)
}
