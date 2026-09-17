package fr.cartulaire.data

enum class SiteKind(val iconId: String, val label: String) {
    CHURCH("church", "Églises"),
    CASTLE("castle", "Châteaux"),
    VILLAGE("village", "Villages"),
    ALTAR("altar", "Autels"),
    SANCTUARY("sanctuary", "Sanctuaires"),
}

data class LatLon(val lat: Double, val lon: Double)

data class HeritageSite(
    val id: String,
    val name: String,
    val kind: SiteKind,
    val lat: Double,
    val lon: Double,
    val subtitle: String? = null,
    val wikipedia: String? = null,
    val wikidata: String? = null,
    val startDate: String? = null,
    val heritage: String? = null,
    val source: String = "osm",
    val dept: String? = null,
    val region: String? = null,
) {
    fun matches(query: String): Boolean {
        if (query.isBlank()) return true
        val q = query.trim().lowercase()
        return name.lowercase().contains(q) ||
            (subtitle?.lowercase()?.contains(q) == true) ||
            kind.label.lowercase().contains(q)
    }
}

data class MapBounds(
    val south: Double,
    val west: Double,
    val north: Double,
    val east: Double,
    val zoom: Double,
)
