package com.stacknoise.haac.core.common.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.stacknoise.haac.core.common.R

/**
 * The icons a home, level or room can have (concept 6.1). [key] is what the database stores; an unknown key
 * (e.g. from a later app version) shows no icon.
 */
enum class PlaceIcon(val key: String, @DrawableRes val drawable: Int, @StringRes val label: Int) {
    HOME("home", R.drawable.ic_place_home, R.string.place_icon_home),
    BUILDING("building", R.drawable.ic_place_building, R.string.place_icon_building),
    CABIN("cabin", R.drawable.ic_place_cabin, R.string.place_icon_cabin),
    LIVING("living", R.drawable.ic_place_living, R.string.place_icon_living),
    BED("bed", R.drawable.ic_place_bed, R.string.place_icon_bed),
    KITCHEN("kitchen", R.drawable.ic_place_kitchen, R.string.place_icon_kitchen),
    DINING("dining", R.drawable.ic_place_dining, R.string.place_icon_dining),
    BATH("bath", R.drawable.ic_place_bath, R.string.place_icon_bath),
    OFFICE("office", R.drawable.ic_place_office, R.string.place_icon_office),
    LAUNDRY("laundry", R.drawable.ic_place_laundry, R.string.place_icon_laundry),
    GARAGE("garage", R.drawable.ic_place_garage, R.string.place_icon_garage),
    GARDEN("garden", R.drawable.ic_place_garden, R.string.place_icon_garden),
    TERRACE("terrace", R.drawable.ic_place_terrace, R.string.place_icon_terrace),
    KIDS("kids", R.drawable.ic_place_kids, R.string.place_icon_kids),
    STAIRS("stairs", R.drawable.ic_place_stairs, R.string.place_icon_stairs),
    LAYERS("layers", R.drawable.ic_place_layers, R.string.place_icon_layers);

    /** Finds icons by their stored key. */
    companion object {
        /** The icon stored as [key], or null for none or an unknown key. */
        fun fromKey(key: String?): PlaceIcon? = entries.firstOrNull { it.key == key }
    }
}
