/*
 * Copyright (C) 2021 Electronic Arts Inc.  All rights reserved.
 */

namespace java com.example

struct TeamInfo {
    1: string team
    2: list<TeamMember> members
}

struct TeamMember {
    1: string user
    2: string role
}

service StubService {
    string stubMethod(1: string request)
}
