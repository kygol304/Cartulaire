package fr.cartulaire.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class OverpassRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS)
        .build(),
) {
    private val endpoints = listOf(
        "https://overpass-api.de/api/interpreter",
        "https://overpass.kumi.systems/api/interpreter",
        "https://overpass.openstreetmap.fr/api/interpreter",
    )

    suspend fun fetch(bounds: MapBounds): List<HeritageSite> = withContext(Dispatchers.IO) {
        val query = buildQuery(bounds)
        var lastError: Exception? = null
        for (url in endpoints) {
            try {
                val body = FormBody.Builder().add("data", query).build()
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Cartulaire/1.0 (medieval heritage map)")
                    .post(body)
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        lastError = IllegalStateException("HTTP ${response.code}")
                        return@use
                    }
                    val text = response.body?.string().orEmpty()
                    return@withContext parse(text)
                }
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw lastError ?: IllegalStateException("Overpass indisponible")
    }

    private fun buildQuery(bounds: MapBounds): String {
        val bbox = "${bounds.south},${bounds.west},${bounds.north},${bounds.east}"
        val parts = mutableListOf(
            """node["historic"="castle"]($bbox);""",
            """node["historic"="fort"]($bbox);""",
            """node["historic"="altar"]($bbox);""",
            """node["man_made"="altar"]($bbox);""",
            """node["historic"="shrine"]($bbox);""",
            """node["pilgrimage"="yes"]($bbox);""",
        )
        if (bounds.zoom >= 10) {
            parts += """node["building"="church"]["name"]($bbox);"""
            parts += """node["building"="cathedral"]($bbox);"""
            parts += """node["amenity"="place_of_worship"]["religion"="christian"]["name"]($bbox);"""
            parts += """node["historic"="wayside_shrine"]["name"]($bbox);"""
            parts += """node["place"~"^(village|town)$"]["historic"]($bbox);"""
        }
        return """
            [out:json][timeout:25];
            (
              ${parts.joinToString("\n              ")}
            );
            out center tags;
        """.trimIndent()
    }

    private fun parse(json: String): List<HeritageSite> {
        val root = JSONObject(json)
        val elements = root.optJSONArray("elements") ?: return emptyList()
        val out = LinkedHashMap<String, HeritageSite>()
        for (i in 0 until elements.length()) {
            val el = elements.getJSONObject(i)
            val tags = el.optJSONObject("tags") ?: continue
            val kind = classify(tags) ?: continue
            val (lat, lon) = coordinates(el) ?: continue
            val name = tags.optString("name:fr")
                .ifBlank { tags.optString("name") }
                .ifBlank { defaultName(kind, tags) }
            val id = "${el.optString("type")}:${el.optLong("id")}"
            out.putIfAbsent(
                id,
                HeritageSite(
                    id = id,
                    name = name,
                    kind = kind,
                    lat = lat,
                    lon = lon,
                    subtitle = subtitle(tags, kind),
                    wikipedia = tags.optString("wikipedia").ifBlank { null },
                    wikidata = tags.optString("wikidata").ifBlank { null },
                    startDate = tags.optString("start_date").ifBlank { null },
                    heritage = tags.optString("heritage").ifBlank { null },
                    source = "overpass",
                ),
            )
        }
        return out.values.toList()
    }

    private fun classify(tags: JSONObject): SiteKind? {
        val building = tags.optString("building")
        val historic = tags.optString("historic")
        val amenity = tags.optString("amenity")
        val religion = tags.optString("religion")
        val place = tags.optString("place")
        val ruins = tags.optString("ruins")
        val worship = tags.optString("place_of_worship")
        val name = (tags.optString("name:fr").ifBlank { tags.optString("name") }).lowercase()
        val isCathedralOrParish = building in setOf("church", "cathedral") || historic == "church"

        if (historic == "altar" ||
            tags.optString("man_made") == "altar" ||
            worship == "altar"
        ) {
            return SiteKind.ALTAR
        }

        val looksLikeSanctuary =
            historic in setOf("shrine", "wayside_shrine") ||
                building == "shrine" ||
                worship == "shrine" ||
                name.contains("sanctuaire") ||
                name.contains("oratoire") ||
                (tags.optString("pilgrimage") == "yes" && !isCathedralOrParish)

        if (looksLikeSanctuary) {
            return SiteKind.SANCTUARY
        }

        if (building in setOf("church", "cathedral", "chapel") ||
            historic == "church" ||
            (amenity == "place_of_worship" && religion == "christian")
        ) {
            return SiteKind.CHURCH
        }
        if (historic in setOf("castle", "fort", "manor") ||
            ruins == "castle" ||
            tags.has("castle_type")
        ) {
            return SiteKind.CASTLE
        }
        if (place in setOf("village", "town", "hamlet") ||
            historic in setOf("citywalls", "city_gate", "yes") ||
            tags.has("heritage")
        ) {
            return SiteKind.VILLAGE
        }
        if (tags.optString("historic:civilization") == "medieval") {
            return SiteKind.VILLAGE
        }
        return null
    }

    private fun coordinates(el: JSONObject): Pair<Double, Double>? {
        if (el.has("lat") && el.has("lon")) {
            return el.getDouble("lat") to el.getDouble("lon")
        }
        val center = el.optJSONObject("center") ?: return null
        if (center.has("lat") && center.has("lon")) {
            return center.getDouble("lat") to center.getDouble("lon")
        }
        return null
    }

    private fun defaultName(kind: SiteKind, tags: JSONObject): String {
        val building = tags.optString("building")
        return when {
            building == "cathedral" -> "Cathédrale"
            building == "chapel" -> "Chapelle"
            tags.optString("historic") == "wayside_shrine" -> "Oratoire"
            kind == SiteKind.CHURCH -> "Église"
            kind == SiteKind.CASTLE -> "Château"
            kind == SiteKind.ALTAR -> "Autel"
            kind == SiteKind.SANCTUARY -> "Sanctuaire"
            else -> "Cité médiévale"
        }
    }

    private fun subtitle(tags: JSONObject, kind: SiteKind): String? {
        val bits = listOfNotNull(
            tags.optString("start_date").ifBlank { null }?.let { "Élevé vers $it" },
            tags.optString("castle_type").ifBlank { null },
            tags.optString("denomination").ifBlank { null },
            tags.optString("heritage:operator").ifBlank { null },
            when (tags.optString("historic")) {
                "wayside_shrine" -> "Oratoire"
                "altar" -> "Autel"
                "shrine" -> "Sanctuaire"
                else -> null
            },
        )
        return bits.firstOrNull() ?: kind.label.trimEnd('s')
    }
}
