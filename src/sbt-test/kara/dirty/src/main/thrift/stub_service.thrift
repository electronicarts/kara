/*
 * Copyright (C) 2020 Electronic Arts Inc.  All rights reserved.
 */

namespace java com.dirty

struct MyStruct {
    1: string id
}

service StubService {
    string stubMethod(1: string request)
}
