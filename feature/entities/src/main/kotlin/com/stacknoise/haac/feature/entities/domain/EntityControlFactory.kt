package com.stacknoise.haac.feature.entities.domain

import com.stacknoise.haac.core.database.entity.ExposedEntity
import javax.inject.Inject

/**
 * Builds the controls of an entity from its domain, `supported_features` and attributes (concept 8, 17.2). The
 * only place that branches on the domain to decide which controls a tile or the detail screen offers.
 */
interface EntityControlFactory {
    /** The control of [entity]'s tile in [state], or null if it has none (sensors, unknown state, no feature). */
    fun create(entity: ExposedEntity, state: EntityState): EntityControl?

    /** Every control of [entity] in [state] for the detail screen, in the order of concept 8.2 and 8.4. */
    fun detail(entity: ExposedEntity, state: EntityState): List<EntityControl>
}

/** [EntityControlFactory] for switch, sensor (no controls) and climate (concept 1.2, 8). */
class DefaultEntityControlFactory @Inject constructor() : EntityControlFactory {
    /** Switches with a known on/off state toggle; climate entities with flag 1 and a target get − and +. */
    override fun create(entity: ExposedEntity, state: EntityState): EntityControl? = when (entity.domain) {
        EntityDomains.SWITCH -> toggle(state)
        EntityDomains.CLIMATE -> ClimateControls(entity.supportedFeatures, state).targetTemperature()
        else -> null
    }

    /** The switch toggle, or all climate controls the entity supports; sensors are read-only (8.3). */
    override fun detail(entity: ExposedEntity, state: EntityState): List<EntityControl> = when (entity.domain) {
        EntityDomains.SWITCH -> listOfNotNull(toggle(state))
        EntityDomains.CLIMATE -> ClimateControls(entity.supportedFeatures, state).all()
        else -> emptyList()
    }

    /** The toggle of a switch that is `on` or `off`. */
    private fun toggle(state: EntityState): EntityControl.Toggle? = when (state.state) {
        EntityDomains.ON -> EntityControl.Toggle(on = true)
        EntityDomains.OFF -> EntityControl.Toggle(on = false)
        else -> null
    }
}
