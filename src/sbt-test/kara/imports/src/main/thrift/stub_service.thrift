/*
 * Copyright (C) 2020 Electronic Arts Inc.  All rights reserved.
 */

namespace java com.local

include "import_stub_service.thrift"

struct MyStruct {
    1: string id
    2: import_stub_service.ImportedStruct imp
}

service StubService {
    string stubMethod(1: MyStruct one)
}
