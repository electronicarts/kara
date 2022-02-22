/*
 * Copyright (C) 2022 Electronic Arts Inc.  All rights reserved.
 */

namespace java com.imported

struct ImportedStructDependency {
    1: string dependencyId
}

struct ImportedStruct {
    1: ImportedStructDependency legacyId
    2: optional i64 serial
}

enum ImportedEnum {
    camelCase,
    lowercase,
    PascalCase,
    snake_case,
    UPPERCASE
}

exception ImportedException {
    1: string message
}