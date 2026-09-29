package com.stacknoise.haac.feature.entities.domain

import com.stacknoise.haac.core.database.entity.ExposedEntity
import javax.inject.Inject

/**
 * Builds the control of an entity from its domain, `supported_features` and attributes (concept 8, 17.2). The
 * only place that branches on the domain to decide which control a tile offers.
 */
interface EntityControlFactory {
    /** The control of [entity] in [state], or null if it has none (sensors, unknown state, missing feature). */
    fun create(entity: ExposedEntity, state: EntityState): EntityControl?
}

/** [EntityControlFactory] for the tile controls of v1: switch toggle and climate target temperature. */
class DefaultEntityControlFactory @Inject constructor() : EntityControlFactory {
    /** Switches with a known on/off state toggle; climate entities with flag 1 and a target get − and +. */
    override fun create(entity: ExposedEntity, state: EntityState): EntityControl? = when (entity.domain) {
        EntityDomains.SWITCH -> when (state.state) {
            EntityDomains.ON -> EntityControl.Toggle(on = true)
            EntityDomains.OFF -> EntityControl.Toggle(on = false)
            else -> null
        }
        EntityDomains.CLIMATE -> targetTemperature(entity.supportedFeatures, state)
        else -> null
    }

    /**
     * The target temperature control with HA's limits `min_temp`, `max_temp` and `target_temp_step` (concept
     * 8.4); null without flag 1 or while HA reports no target (e.g. in mode `off`).
     */
    private fun targetTemperature(features: Int, state: EntityState): EntityControl.TargetTemperature? {
        val target = state.attributes.number(EntityDomains.TEMPERATURE)
            ?.takeIf { features and EntityDomains.CLIMATE_TARGET_TEMPERATURE != 0 }
            ?: return null
        return EntityControl.TargetTemperature(
            target = target,
            min = state.attributes.number("min_temp") ?: DEFAULT_MIN,
            max = state.attributes.number("max_temp") ?: DEFAULT_MAX,
            step = state.attributes.number("target_temp_step")?.takeIf { it > 0 } ?: DEFAULT_STEP,
        )
    }

    /** HA's defaults of a climate entity in °C when it leaves out its limits. */
    private companion object {
        const val DEFAULT_MIN = 7.0
        const val DEFAULT_MAX = 35.0
        const val DEFAULT_STEP = 0.5
    }
}
