package com.stacknoise.haac.core.database

import androidx.room.withTransaction
import javax.inject.Inject

/** Runs several DAO calls in one database transaction (concept 6.2: structural changes are atomic). */
interface DatabaseTransactions {
    /** Runs [block] in one transaction; it is rolled back if [block] throws. */
    suspend fun <T> run(block: suspend () -> T): T
}

/** [DatabaseTransactions] of the app database. */
class RoomTransactions @Inject constructor(private val database: HaacDatabase) : DatabaseTransactions {
    /** Uses Room's transaction, which the DAO calls in [block] join. */
    override suspend fun <T> run(block: suspend () -> T): T = database.withTransaction { block() }
}
