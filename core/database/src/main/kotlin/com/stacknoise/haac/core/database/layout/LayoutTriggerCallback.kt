package com.stacknoise.haac.core.database.layout

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/** Creates the [LayoutTriggers] in a new database; existing databases get them from `Migration4To5`. */
object LayoutTriggerCallback : RoomDatabase.Callback() {
    /** Runs once, after Room created the tables of a new database. */
    override fun onCreate(db: SupportSQLiteDatabase) {
        LayoutTriggers.statements.forEach(db::execSQL)
    }
}
