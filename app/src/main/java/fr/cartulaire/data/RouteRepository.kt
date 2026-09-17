package fr.cartulaire.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class RouteRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build(),
) {
    suspend fun walkingRoute(from: LatLon, to: LatLon): List<LatLon> = withContext(Dispatchers.IO) {
        val url =
            "https://router.project-osrm.org/route/v1/foot/${from.lon},${from.lat};${to.lon},${to.lat}" +
                "?overview=full&geometries=geojson"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Cartulaire/1.0")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Itinéraire indisponible")
            val root = JSONObject(response.body?.string().orEmpty())
            val coords = root.getJSONArray("routes")
                .getJSONObject(0)
                .getJSONObject("geometry")
                .getJSONArray("coordinates")
            buildList {
                for (i in 0 until coords.length()) {
                    val pair = coords.getJSONArray(i)
                    add(LatLon(lat = pair.getDouble(1), lon = pair.getDouble(0)))
                }
            }
        }
    }
}
