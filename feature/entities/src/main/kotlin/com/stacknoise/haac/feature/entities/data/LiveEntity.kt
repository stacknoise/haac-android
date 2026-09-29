package com.stacknoise.haac.feature.entities.data

import com.stacknoise.haac.core.database.entity.ExposedEntity
import com.stacknoise.haac.core.database.entity.ExposedEntityDao
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.ErrorFactory
import com.stacknoise.haac.core.error.NetworkException
import com.stacknoise.haac.core.error.ValidationException
import com.stacknoise.haac.core.error.database
import com.stacknoise.haac.core.network.connection.BridgeChannel
import com.stacknoise.haac.core.network.connection.LiveConnection

/** The cached entity [entityId] of instance [serverId]; HAAC-ENT-001 if the cache does not know it. */
internal suspend fun ExposedEntityDao.require(serverId: String, entityId: String, errors: ErrorFactory): ExposedEntity =
    errors.database { all(serverId) }.firstOrNull { it.entityId == entityId }
        ?: throw ValidationException(ErrorCode.ENT_NOT_FOUND)

/** The open connection; HAAC-NET-002 while there is none (concept 14.1). */
internal fun LiveConnection.requireOpen(): BridgeChannel =
    connection.value?.takeIf { it.isOpen } ?: throw NetworkException(ErrorCode.NET_CONNECTION_LOST)
