package fr.cartulaire.data

data class CityIllum(
    val name: String,
    val lat: Double,
    val lon: Double,
    val icon: String,
)

object IlluminatedPlaces {
    const val ICON_WALL = "illum_city_wall"
    const val ICON_RIVER = "illum_city_river"
    const val ICON_VILLAGE = "illum_village"
    const val ICON_ABBEY = "illum_abbey"
    const val ICON_MARGIN = "illum_margin"
    const val ICON_PEASANTS = "illum_peasants"
    const val ICON_SKELETON = "illum_skeleton"
    const val ICON_DEATH = "illum_death"
    const val ICON_KNIGHT = "illum_knight"
    const val ICON_MILL = "illum_mill"
    const val ICON_TIMBER = "illum_timber"
    const val ICON_PORT = "illum_port"
    const val ICON_BOAR = "illum_boar"
    const val ICON_SHIP = "illum_ship"

    private val SETTLEMENT_ICONS = listOf(
        ICON_WALL, ICON_RIVER, ICON_VILLAGE, ICON_ABBEY, ICON_MILL, ICON_TIMBER, ICON_PORT,
    )

    fun settlementIcon(name: String): String {
        val idx = kotlin.math.abs(name.hashCode()) % SETTLEMENT_ICONS.size
        return SETTLEMENT_ICONS[idx]
    }

    val cities: List<CityIllum> = listOf(
        CityIllum("Le Mans", 48.0077, 0.1981, ICON_WALL),
        CityIllum("Paris", 48.8566, 2.3522, ICON_RIVER),
        CityIllum("Lyon", 45.7640, 4.8357, ICON_RIVER),
        CityIllum("Rouen", 49.4432, 1.0993, ICON_RIVER),
        CityIllum("Strasbourg", 48.5734, 7.7521, ICON_WALL),
        CityIllum("Toulouse", 43.6047, 1.4442, ICON_RIVER),
        CityIllum("Bordeaux", 44.8378, -0.5792, ICON_RIVER),
        CityIllum("Avignon", 43.9493, 4.8055, ICON_WALL),
        CityIllum("Carcassonne", 43.2130, 2.3517, ICON_WALL),
        CityIllum("Chartres", 48.4439, 1.4890, ICON_WALL),
        CityIllum("Reims", 49.2583, 4.0317, ICON_WALL),
        CityIllum("Amiens", 49.8942, 2.2957, ICON_RIVER),
        CityIllum("Bourges", 47.0810, 2.3988, ICON_WALL),
        CityIllum("Dijon", 47.3220, 5.0415, ICON_WALL),
        CityIllum("Tours", 47.3941, 0.6848, ICON_RIVER),
        CityIllum("Orléans", 47.9029, 1.9093, ICON_RIVER),
        CityIllum("Angers", 47.4712, -0.5518, ICON_WALL),
        CityIllum("Poitiers", 46.5802, 0.3404, ICON_WALL),
        CityIllum("Rennes", 48.1173, -1.6778, ICON_WALL),
        CityIllum("Nantes", 47.2184, -1.5536, ICON_RIVER),
        CityIllum("Caen", 49.1829, -0.3707, ICON_WALL),
        CityIllum("Metz", 49.1193, 6.1757, ICON_RIVER),
        CityIllum("Troyes", 48.2973, 4.0744, ICON_WALL),
        CityIllum("Provins", 48.5586, 3.2994, ICON_WALL),
        CityIllum("Sarlat", 44.8892, 1.2167, ICON_VILLAGE),
        CityIllum("Rocamadour", 44.7994, 1.6181, ICON_ABBEY),
        CityIllum("Le Mont-Saint-Michel", 48.6360, -1.5115, ICON_ABBEY),
        CityIllum("Vézelay", 47.4664, 3.7486, ICON_ABBEY),
        CityIllum("Conques", 44.5990, 2.3970, ICON_ABBEY),
        CityIllum("Cordes-sur-Ciel", 44.0636, 1.9508, ICON_VILLAGE),
        CityIllum("Dinan", 48.4564, -2.0478, ICON_WALL),
        CityIllum("Vitré", 48.1239, -1.2147, ICON_WALL),
        CityIllum("Aigues-Mortes", 43.5669, 4.1928, ICON_WALL),
        CityIllum("Albi", 43.9286, 2.1428, ICON_RIVER),
        CityIllum("Cluny", 46.4347, 4.6592, ICON_ABBEY),
        CityIllum("Le Puy-en-Velay", 45.0428, 3.8829, ICON_ABBEY),
        CityIllum("Laon", 49.5641, 3.6203, ICON_WALL),
        CityIllum("Beauvais", 49.4294, 2.0803, ICON_WALL),
        CityIllum("Senlis", 49.2072, 2.5867, ICON_WALL),
        CityIllum("Colmar", 48.0794, 7.3586, ICON_VILLAGE),
        CityIllum("Riquewihr", 48.1667, 7.2972, ICON_VILLAGE),
        CityIllum("Pérouges", 45.9039, 5.1794, ICON_VILLAGE),
        CityIllum("Saint-Malo", 48.6493, -2.0257, ICON_WALL),
        CityIllum("Honfleur", 49.4194, 0.2328, ICON_RIVER),
        CityIllum("Chinon", 47.1681, 0.2428, ICON_WALL),
        CityIllum("Loches", 47.1286, 0.9964, ICON_WALL),
        CityIllum("Foix", 42.9658, 1.6050, ICON_WALL),
        CityIllum("Carcassonne cité", 43.2065, 2.3640, ICON_WALL),
        CityIllum("Bruges", 51.2093, 3.2247, ICON_RIVER),
        CityIllum("Gand", 51.0543, 3.7174, ICON_RIVER),
        CityIllum("Cologne", 50.9375, 6.9603, ICON_RIVER),
        CityIllum("Prague", 50.0755, 14.4378, ICON_RIVER),
        CityIllum("Sienne", 43.3188, 11.3308, ICON_WALL),
        CityIllum("Florence", 43.7696, 11.2558, ICON_RIVER),
        CityIllum("Toledo", 39.8628, -4.0273, ICON_WALL),
        CityIllum("Ségovie", 40.9429, -4.1088, ICON_WALL),
        CityIllum("York", 53.9591, -1.0815, ICON_WALL),
        CityIllum("Canterbury", 51.2802, 1.0789, ICON_WALL),
        CityIllum("Rothenburg", 49.3770, 10.1868, ICON_WALL),
        CityIllum("Tallinn", 59.4370, 24.7536, ICON_WALL),
        CityIllum("Cracovie", 50.0647, 19.9450, ICON_WALL),
        CityIllum("Avila", 40.6565, -4.7003, ICON_WALL),
        CityIllum("San Gimignano", 43.4677, 11.0432, ICON_VILLAGE),
        CityIllum("Assise", 43.0707, 12.6196, ICON_ABBEY),
        CityIllum("Montserrat", 41.5933, 1.8378, ICON_ABBEY),
        CityIllum("Lourdes", 43.0976, -0.0584, ICON_ABBEY),
        CityIllum("Lisieux", 49.1459, 0.2278, ICON_ABBEY),
    )

    val people: List<CityIllum> = listOf(
        CityIllum("moissons", 47.35, 1.85, ICON_PEASANTS),
        CityIllum("moissons", 44.55, 0.55, ICON_PEASANTS),
        CityIllum("moissons", 48.35, -1.15, ICON_PEASANTS),
        CityIllum("moissons", 45.85, 4.55, ICON_PEASANTS),
        CityIllum("moissons", 43.85, 3.55, ICON_PEASANTS),
        CityIllum("chevalier", 47.85, 3.55, ICON_KNIGHT),
        CityIllum("chevalier", 44.95, 2.15, ICON_KNIGHT),
        CityIllum("chevalier", 49.15, 2.85, ICON_KNIGHT),
        CityIllum("chasse", 47.2, 2.1, ICON_MARGIN),
        CityIllum("chasse", 44.8, 1.4, ICON_MARGIN),
        CityIllum("chasse", 48.7, -0.8, ICON_MARGIN),
        CityIllum("chasse", 45.5, 5.5, ICON_MARGIN),
        CityIllum("sanglier", 46.4, 3.2, ICON_BOAR),
        CityIllum("sanglier", 44.1, 3.8, ICON_BOAR),
        CityIllum("sanglier", 48.2, 5.1, ICON_BOAR),
        CityIllum("nef", 47.4, -2.8, ICON_SHIP),
        CityIllum("nef", 43.4, 5.0, ICON_SHIP),
        CityIllum("nef", 49.4, -0.1, ICON_SHIP),
    )

    val macabre: List<CityIllum> = listOf(
        CityIllum("danse", 47.05, 0.95, ICON_SKELETON),
        CityIllum("danse", 45.15, 1.35, ICON_SKELETON),
        CityIllum("danse", 48.95, 4.15, ICON_SKELETON),
        CityIllum("danse", 43.55, 2.85, ICON_SKELETON),
        CityIllum("mort", 46.55, 2.85, ICON_DEATH),
        CityIllum("mort", 44.25, 4.15, ICON_DEATH),
        CityIllum("mort", 49.35, 1.55, ICON_DEATH),
        CityIllum("mort", 42.95, 2.15, ICON_DEATH),
    )

    val doodles: List<CityIllum> get() = people + macabre

    fun nearest(lat: Double, lon: Double, maxKm: Double = 12.0): CityIllum? {
        var best: CityIllum? = null
        var bestD = maxKm * 1000
        for (city in cities) {
            val d = Geo.haversineMeters(LatLon(lat, lon), LatLon(city.lat, city.lon))
            if (d < bestD) {
                bestD = d
                best = city
            }
        }
        return best
    }
}
