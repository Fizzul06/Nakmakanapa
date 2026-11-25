package com.spg.nakmakanapa.model

/**
 * The three quick-filter chips on the Home screen.
 *
 * Each one is exclusive — tapping one clears the others. All of them now drive
 * their own Google query rather than filtering only the already-loaded list,
 * because client-side filtering silently hid valid results:
 *
 *  - BUDGET  : the loaded 10 might all be pricey, so ask Google for cheap ones.
 *  - NEARBY  : the loaded 10 might all be 5-8km away, so ask Google to restrict
 *              the radius to 5km.
 *  - FANCY   : only ~1 in 20 places is tagged expensive, so ask Google directly.
 */
enum class QuickFilter(val label: String) {
    BUDGET("Under"),
    NEARBY("Within"),
    FANCY("Fancy")
}