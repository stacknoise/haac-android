package com.stacknoise.haac.core.database.entity

import com.stacknoise.haac.core.database.assignment.EntityAlias

/**
 * The name the app shows for this entity (concept 7.3): the local [alias], else the configured name from the
 * bridge, else the HA name, else the `entity_id`.
 */
fun ExposedEntity.displayName(alias: String?): String =
    alias?.takeIf { it.isNotBlank() } ?: configuredName?.takeIf { it.isNotBlank() } ?: haName.ifBlank { entityId }

/** The local names by `entity_id`, to pass the right one to [displayName] for every entity. */
fun List<EntityAlias>.byEntityId(): Map<String, String> = associate { it.entityId to it.alias }
