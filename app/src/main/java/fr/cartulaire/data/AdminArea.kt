package fr.cartulaire.data

enum class ExploreLevel { KINGDOM, REGION, DEPARTMENT }

data class AdminArea(
    val code: String,
    val name: String,
    val parent: String? = null,
    val south: Double,
    val west: Double,
    val north: Double,
    val east: Double,
) {
    fun bounds(zoom: Double) = MapBounds(south, west, north, east, zoom)
}
