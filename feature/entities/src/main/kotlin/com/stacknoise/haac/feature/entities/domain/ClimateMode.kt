package com.stacknoise.haac.feature.entities.domain

/**
 * The mode lists of a climate entity shown as chips (concept 8.4): the attribute with the current mode, the one
 * with the choices, the service and its `supported_features` flag. The service data key equals [attribute].
 */
enum class ClimateMode(val attribute: String, val choices: String, val service: String, val flag: Int) {
    FAN("fan_mode", "fan_modes", "set_fan_mode", EntityDomains.CLIMATE_FAN_MODE),
    PRESET("preset_mode", "preset_modes", "set_preset_mode", EntityDomains.CLIMATE_PRESET_MODE),
    SWING("swing_mode", "swing_modes", "set_swing_mode", EntityDomains.CLIMATE_SWING_MODE),
    SWING_HORIZONTAL(
        "swing_horizontal_mode",
        "swing_horizontal_modes",
        "set_swing_horizontal_mode",
        EntityDomains.CLIMATE_SWING_HORIZONTAL,
    ),
}
