package com.stacknoise.haac.feature.layout.data

import com.stacknoise.haac.core.database.layout.FloorDao
import com.stacknoise.haac.core.database.layout.HomeDao
import com.stacknoise.haac.core.database.layout.RoomDao
import javax.inject.Inject

/** The DAOs of the tables `home`, `floor` and `room`, grouped to keep constructors short. */
class LayoutDaos @Inject constructor(val homes: HomeDao, val floors: FloorDao, val rooms: RoomDao)
