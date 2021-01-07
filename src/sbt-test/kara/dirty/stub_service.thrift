/*
 * Copyright (C) 2021 Electronic Arts Inc.  All rights reserved.
 */

namespace java com.dirty

struct MyNewStruct {
    1: string new_id
}

service StubService {
    string stubMethod(1: string request)
}
