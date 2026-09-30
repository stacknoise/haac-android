package com.stacknoise.haac.feature.entities.data

import com.stacknoise.haac.core.database.entity.ExposedEntity
import com.stacknoise.haac.core.database.entity.ExposedEntityDao
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.ErrorFactory
import com.stacknoise.haac.core.error.ValidationException
import com.stacknoise.haac.core.error.database

/** The cached entity [entityId] of instance [serverId]; HAAC-ENT-001 if the cache does not know it. */
internal suspend fun ExposedEntityDao.require(serverId: String, entityId: String, errors: ErrorFactory): ExposedEntity =
    errors.database { all(serverId) }.firstOrNull { it.entityId == entityId }
        ?: throw ValidationException(ErrorCode.ENT_NOT_FOUND)
