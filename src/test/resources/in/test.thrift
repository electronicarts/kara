/*
 * Copyright (C) 2020 Electronic Arts Inc.  All rights reserved.
 */

namespace java com.example

include "imported_test.thrift"

enum ExampleEnum {
    camelCase,
    lowercase,
    PascalCase,
    snake_case,
    UPPERCASE
}

struct ExampleField {
    1: bool aBool
    2: string aString
}

union ExampleUnion {
    1: bool aBool
    2: ExampleField anExampleField
}

struct UPPER {
    1: bool aValue
}

struct EmptyStruct {

}

typedef i64 ExternalId
typedef string InternalId

struct ExampleRequest {
    1: ExternalId externalId
    2: ExampleField settings
    3: list<ExampleField> settingsList
    4: set<ExampleField> settingsSet
    5: map<string, ExampleField> settingsMap
    6: optional ExampleEnum logLevel
    7: optional ExampleUnion newUnionField
    8: imported_test.ImportedEnum importedEnum
    9: imported_test.ImportedStruct importedStruct
    10: list<string> aList
    11: map<string,string> aMap
    12: i16 aShort
    13: i32 anInt
    14: i64 aLong
    15: binary aBinary
    16: optional string anOption
    17: EmptyStruct empty
}

struct ExampleResponse {
    1: InternalId id
    2: optional string name
}

exception ExampleException {
    1: string message
}

exception OtherExampleException {
    1: string message
    2: i16 code
}

service ExampleService {
    ExampleResponse exampleMethod1(1: ExampleRequest request)
        throws(
          1: ExampleException error
          2: OtherExampleException otherError
          3: imported_test.ImportedException importedError
        )

    ExampleResponse noArgMethod()

    ExampleResponse multiArgMethod(1: ExampleField one, 2: ExampleEnum two)
}