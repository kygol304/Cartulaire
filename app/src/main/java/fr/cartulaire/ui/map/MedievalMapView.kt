package fr.cartulaire.ui.map

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.gson.JsonObject
import fr.cartulaire.R
import fr.cartulaire.data.ExploreLevel
import fr.cartulaire.data.HeritageSite
import fr.cartulaire.data.IlluminatedPlaces
import fr.cartulaire.data.LatLon
import fr.cartulaire.data.MapBounds
import fr.cartulaire.data.SiteKind
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

class MapListeners {
    var onBounds: (MapBounds) -> Unit = {}
    var onSelectId: (String?) -> Unit = {}
    var sites: List<HeritageSite> = emptyList()
    var user: LatLon? = null
    var route: List<LatLon> = emptyList()
    var showCities: Boolean = true
    var showPeople: Boolean = true
    var showMacabre: Boolean = true
    var level: ExploreLevel = ExploreLevel.KINGDOM
    var onAdmin: (ExploreLevel, String, String) -> Unit = { _, _, _ -> }
}

class MapFacade {
    var map: MapLibreMap? = null
    private var pendingSites: List<HeritageSite> = emptyList()
    private var pendingUser: LatLon? = null
    private var pendingRoute: List<LatLon> = emptyList()

    fun attach(map: MapLibreMap) {
        this.map = map
        setSites(pendingSites)
        pendingUser?.let { setUser(it) }
        setRoute(pendingRoute)
    }

    fun setSites(sites: List<HeritageSite>) {
        pendingSites = sites
        val style = map?.style ?: return
        val source = style.getSource(SOURCE_POI) as? GeoJsonSource ?: return
        source.setGeoJson(FeatureCollection.fromFeatures(sites.map { it.toFeature() }))
    }

    fun setUser(user: LatLon?) {
        pendingUser = user
        val style = map?.style ?: return
        val source = style.getSource(SOURCE_USER) as? GeoJsonSource ?: return
        if (user == null) {
            source.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
        } else {
            val feature = Feature.fromGeometry(Point.fromLngLat(user.lon, user.lat))
            source.setGeoJson(FeatureCollection.fromFeatures(arrayOf(feature)))
        }
    }

    fun setRoute(points: List<LatLon>) {
        pendingRoute = points
        val style = map?.style ?: return
        val source = style.getSource(SOURCE_ROUTE) as? GeoJsonSource ?: return
        if (points.size < 2) {
            source.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
            return
        }
        val line = LineString.fromLngLats(points.map { Point.fromLngLat(it.lon, it.lat) })
        source.setGeoJson(FeatureCollection.fromFeatures(arrayOf(Feature.fromGeometry(line))))
    }

    fun setDecor(showCities: Boolean, showPeople: Boolean, showMacabre: Boolean) {
        val style = map?.style ?: return
        val source = style.getSource(SOURCE_ILLUM) as? GeoJsonSource ?: return
        val items = buildList {
            if (showCities) addAll(IlluminatedPlaces.cities)
            if (showPeople) addAll(IlluminatedPlaces.people)
            if (showMacabre) addAll(IlluminatedPlaces.macabre)
        }
        val features = items.map { city ->
            val props = JsonObject()
            props.addProperty("illum", city.icon)
            props.addProperty("name", city.name)
            Feature.fromGeometry(Point.fromLngLat(city.lon, city.lat), props, "${city.name}-${city.lat}")
        }
        source.setGeoJson(FeatureCollection.fromFeatures(features))
    }

    fun flyTo(target: LatLon, zoom: Double) {
        map?.animateCamera(
            CameraUpdateFactory.newLatLngZoom(LatLng(target.lat, target.lon), zoom),
            900,
        )
    }

    fun flyToBounds(bounds: MapBounds) {
        val map = map ?: return
        val south = bounds.south.coerceIn(-85.0, 85.0)
        val north = bounds.north.coerceIn(-85.0, 85.0)
        if (north <= south) return
        val center = LatLng((south + north) / 2.0, (bounds.west + bounds.east) / 2.0)
        val span = maxOf(north - south, kotlin.math.abs(bounds.east - bounds.west))
        val zoom = when {
            span > 8 -> 5.8
            span > 5 -> 6.4
            span > 3 -> 7.0
            span > 1.6 -> 8.0
            span > 0.9 -> 8.8
            else -> 9.5
        }
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(center, zoom), 900)
    }

    fun setLevel(level: ExploreLevel, regionCode: String?, deptCode: String?) {
        val style = map?.style ?: return
        try {
            val regionsFill = style.getLayer(LAYER_REGIONS_FILL) as? FillLayer
            val regionsLine = style.getLayer(LAYER_REGIONS_LINE) as? LineLayer
            val deptsFill = style.getLayer(LAYER_DEPTS_FILL) as? FillLayer
            val deptsLine = style.getLayer(LAYER_DEPTS_LINE) as? LineLayer
            val showAll = Expression.neq(Expression.get("code"), Expression.literal(""))
            val hide = Expression.eq(Expression.get("code"), Expression.literal("__none__"))
            when (level) {
                ExploreLevel.KINGDOM -> {
                    regionsFill?.setFilter(showAll)
                    regionsLine?.setFilter(showAll)
                    deptsFill?.setFilter(hide)
                    deptsLine?.setFilter(hide)
                }
                ExploreLevel.REGION -> {
                    val code = regionCode ?: return
                    regionsFill?.setFilter(Expression.eq(Expression.get("code"), Expression.literal(code)))
                    regionsLine?.setFilter(Expression.eq(Expression.get("code"), Expression.literal(code)))
                    deptsFill?.setFilter(Expression.eq(Expression.get("region"), Expression.literal(code)))
                    deptsLine?.setFilter(Expression.eq(Expression.get("region"), Expression.literal(code)))
                }
                ExploreLevel.DEPARTMENT -> {
                    val code = deptCode ?: return
                    regionsFill?.setFilter(hide)
                    regionsLine?.setFilter(hide)
                    deptsFill?.setFilter(Expression.eq(Expression.get("code"), Expression.literal(code)))
                    deptsLine?.setFilter(Expression.eq(Expression.get("code"), Expression.literal(code)))
                }
            }
        } catch (_: Throwable) {
            // keep previous filters rather than crash
        }
    }

    companion object {
        const val SOURCE_POI = "poi-source"
        const val SOURCE_USER = "user-source"
        const val SOURCE_ROUTE = "route-source"
        const val SOURCE_ILLUM = "illum-source"
        const val SOURCE_REGIONS = "regions-source"
        const val SOURCE_DEPTS = "depts-source"
        const val LAYER_POI = "poi-symbols"
        const val LAYER_ILLUM = "illum-symbols"
        const val LAYER_REGIONS_FILL = "regions-fill"
        const val LAYER_REGIONS_LINE = "regions-line"
        const val LAYER_DEPTS_FILL = "depts-fill"
        const val LAYER_DEPTS_LINE = "depts-line"
    }
}

private fun Feature?.adminCode(): String? {
    val feature = this ?: return null
    return try {
        feature.getStringProperty("code")
            ?: feature.getNumberProperty("code")?.toInt()?.toString()
    } catch (_: Throwable) {
        feature.properties()?.get("code")?.asString
    }
}

private fun Feature?.adminName(): String? {
    val feature = this ?: return null
    return try {
        feature.getStringProperty("nom")
    } catch (_: Throwable) {
        feature.properties()?.get("nom")?.asString
    }
}

private fun HeritageSite.toFeature(): Feature {
    val props = JsonObject()
    props.addProperty("icon", kind.iconId)
    props.addProperty("name", name)
    props.addProperty("id", id)
    props.addProperty("kind", kind.name)
    return Feature.fromGeometry(Point.fromLngLat(lon, lat), props, id)
}

@Composable
fun MedievalMapView(
    sites: List<HeritageSite>,
    user: LatLon?,
    route: List<LatLon>,
    cameraTarget: LatLon?,
    cameraZoom: Double?,
    recenterNonce: Int,
    onBounds: (MapBounds) -> Unit,
    onSelectId: (String?) -> Unit,
    onCameraConsumed: () -> Unit,
    showCities: Boolean,
    showPeople: Boolean,
    showMacabre: Boolean,
    level: ExploreLevel,
    regionCode: String?,
    deptCode: String?,
    cameraBounds: MapBounds?,
    onAdmin: (ExploreLevel, String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val facade = remember { MapFacade() }
    val listeners = remember { MapListeners() }
    listeners.onBounds = onBounds
    listeners.onSelectId = onSelectId
    listeners.sites = sites
    listeners.user = user
    listeners.route = route
    listeners.showCities = showCities
    listeners.showPeople = showPeople
    listeners.showMacabre = showMacabre
    listeners.level = level
    listeners.onAdmin = onAdmin

    val mapView = remember { MapView(context) }

    AndroidView(
        factory = { view ->
            mapView.apply {
                onCreate(Bundle())
                onStart()
                onResume()
                getMapAsync { map ->
                    val json = context.assets.open("map/medieval_style.json")
                        .bufferedReader()
                        .use { it.readText() }
                    map.uiSettings.isCompassEnabled = false
                    map.uiSettings.isRotateGesturesEnabled = true
                    map.uiSettings.isAttributionEnabled = true
                    map.uiSettings.isLogoEnabled = false
                    map.cameraPosition = CameraPosition.Builder()
                        .target(LatLng(46.6, 2.2))
                        .zoom(5.5)
                        .build()
                    map.setStyle(Style.Builder().fromJson(json)) { style ->
                        addImages(style, context.resources)
                        installAdmin(style, context)
                        installLayers(style)
                        installIlluminations(style)
                        facade.attach(map)
                        facade.setSites(listeners.sites)
                        facade.setUser(listeners.user)
                        facade.setRoute(listeners.route)
                        facade.setDecor(listeners.showCities, listeners.showPeople, listeners.showMacabre)
                        facade.setLevel(listeners.level, null, null)
                        emitBounds(map, listeners)
                    }
                    map.addOnCameraIdleListener { emitBounds(map, listeners) }
                    map.addOnMapClickListener { latLng ->
                        try {
                            val pixel = map.projection.toScreenLocation(latLng)
                            when (listeners.level) {
                                ExploreLevel.KINGDOM -> {
                                    val hit = map.queryRenderedFeatures(
                                        pixel,
                                        MapFacade.LAYER_REGIONS_FILL,
                                        MapFacade.LAYER_REGIONS_LINE,
                                    ).firstOrNull()
                                    val code = hit.adminCode()
                                    val nom = hit.adminName()
                                    if (!code.isNullOrBlank()) {
                                        mapView.post {
                                            listeners.onAdmin(ExploreLevel.REGION, code, nom.orEmpty())
                                        }
                                    }
                                }
                                ExploreLevel.REGION -> {
                                    val hit = map.queryRenderedFeatures(
                                        pixel,
                                        MapFacade.LAYER_DEPTS_FILL,
                                        MapFacade.LAYER_DEPTS_LINE,
                                    ).firstOrNull()
                                    val code = hit.adminCode()
                                    val nom = hit.adminName()
                                    if (!code.isNullOrBlank()) {
                                        mapView.post {
                                            listeners.onAdmin(ExploreLevel.DEPARTMENT, code, nom.orEmpty())
                                        }
                                    }
                                }
                                ExploreLevel.DEPARTMENT -> {
                                    val hits = map.queryRenderedFeatures(pixel, MapFacade.LAYER_POI)
                                    listeners.onSelectId(hits.firstOrNull()?.getStringProperty("id"))
                                }
                            }
                        } catch (_: Throwable) {
                            // ignore a missed tap rather than crashing
                        }
                        true
                    }
                }
            }
        },
        modifier = modifier,
    )

    DisposableEffect(mapView) {
        onDispose {
            runCatching {
                mapView.onPause()
                mapView.onStop()
                mapView.onDestroy()
            }
        }
    }

    LaunchedEffect(sites) { facade.setSites(sites) }
    LaunchedEffect(user) { facade.setUser(user) }
    LaunchedEffect(route) { facade.setRoute(route) }
    LaunchedEffect(showCities, showPeople, showMacabre) {
        facade.setDecor(showCities, showPeople, showMacabre)
    }
    LaunchedEffect(level, regionCode, deptCode) {
        facade.setLevel(level, regionCode, deptCode)
    }
    LaunchedEffect(cameraBounds) {
        val box = cameraBounds ?: return@LaunchedEffect
        facade.flyToBounds(box)
        onCameraConsumed()
    }
    LaunchedEffect(cameraTarget, cameraZoom, recenterNonce) {
        val target = cameraTarget ?: return@LaunchedEffect
        facade.flyTo(target, cameraZoom ?: 14.0)
        onCameraConsumed()
    }
}

private fun emitBounds(map: MapLibreMap, listeners: MapListeners) {
    val bounds = map.projection.visibleRegion.latLngBounds
    listeners.onBounds(
        MapBounds(
            south = bounds.latitudeSouth,
            west = bounds.longitudeWest,
            north = bounds.latitudeNorth,
            east = bounds.longitudeEast,
            zoom = map.cameraPosition.zoom,
        ),
    )
}

private fun decode(resources: android.content.res.Resources, id: Int): Bitmap {
    val opts = BitmapFactory.Options().apply { inScaled = false }
    return BitmapFactory.decodeResource(resources, id, opts)
}

private fun addImages(style: Style, resources: android.content.res.Resources) {
    style.addImage(SiteKind.CHURCH.iconId, decode(resources, R.drawable.marker_church))
    style.addImage(SiteKind.CASTLE.iconId, decode(resources, R.drawable.marker_castle))
    style.addImage(SiteKind.VILLAGE.iconId, decode(resources, R.drawable.marker_village))
    style.addImage(SiteKind.ALTAR.iconId, decode(resources, R.drawable.marker_altar))
    style.addImage(SiteKind.SANCTUARY.iconId, decode(resources, R.drawable.marker_sanctuary))
    style.addImage("pilgrim", decode(resources, R.drawable.marker_pilgrim))
    style.addImage(IlluminatedPlaces.ICON_WALL, decode(resources, R.drawable.illum_city_wall))
    style.addImage(IlluminatedPlaces.ICON_RIVER, decode(resources, R.drawable.illum_city_river))
    style.addImage(IlluminatedPlaces.ICON_VILLAGE, decode(resources, R.drawable.illum_village))
    style.addImage(IlluminatedPlaces.ICON_ABBEY, decode(resources, R.drawable.illum_abbey))
    style.addImage(IlluminatedPlaces.ICON_MARGIN, decode(resources, R.drawable.illum_margin))
    style.addImage(IlluminatedPlaces.ICON_PEASANTS, decode(resources, R.drawable.illum_peasants))
    style.addImage(IlluminatedPlaces.ICON_SKELETON, decode(resources, R.drawable.illum_skeleton))
    style.addImage(IlluminatedPlaces.ICON_DEATH, decode(resources, R.drawable.illum_death))
    style.addImage(IlluminatedPlaces.ICON_KNIGHT, decode(resources, R.drawable.illum_knight))
}

private fun installAdmin(style: Style, context: android.content.Context) {
    val regions = context.assets.open("map/regions.geojson").bufferedReader().use { it.readText() }
    val depts = context.assets.open("map/departements.geojson").bufferedReader().use { it.readText() }
    style.addSource(GeoJsonSource(MapFacade.SOURCE_REGIONS, FeatureCollection.fromJson(regions)))
    style.addSource(GeoJsonSource(MapFacade.SOURCE_DEPTS, FeatureCollection.fromJson(depts)))
    style.addLayer(
        FillLayer(MapFacade.LAYER_REGIONS_FILL, MapFacade.SOURCE_REGIONS).withProperties(
            PropertyFactory.fillColor("#6B2D2D"),
            PropertyFactory.fillOpacity(0.38f),
            PropertyFactory.fillOutlineColor("#C9A227"),
        ),
    )
    style.addLayer(
        LineLayer(MapFacade.LAYER_REGIONS_LINE, MapFacade.SOURCE_REGIONS).withProperties(
            PropertyFactory.lineColor("#8B6914"),
            PropertyFactory.lineWidth(1.8f),
        ),
    )
    style.addLayer(
        FillLayer(MapFacade.LAYER_DEPTS_FILL, MapFacade.SOURCE_DEPTS).withProperties(
            PropertyFactory.fillColor("#2F4F3E"),
            PropertyFactory.fillOpacity(0.32f),
            PropertyFactory.fillOutlineColor("#C9A227"),
        ),
    )
    style.addLayer(
        LineLayer(MapFacade.LAYER_DEPTS_LINE, MapFacade.SOURCE_DEPTS).withProperties(
            PropertyFactory.lineColor("#C9A227"),
            PropertyFactory.lineWidth(1.4f),
        ),
    )
    (style.getLayer(MapFacade.LAYER_DEPTS_FILL) as? FillLayer)?.setFilter(
        Expression.eq(Expression.get("code"), Expression.literal("__none__")),
    )
    (style.getLayer(MapFacade.LAYER_DEPTS_LINE) as? LineLayer)?.setFilter(
        Expression.eq(Expression.get("code"), Expression.literal("__none__")),
    )
}

private fun installLayers(style: Style) {
    style.addSource(GeoJsonSource(MapFacade.SOURCE_POI, FeatureCollection.fromFeatures(emptyArray())))
    style.addSource(GeoJsonSource(MapFacade.SOURCE_USER, FeatureCollection.fromFeatures(emptyArray())))
    style.addSource(GeoJsonSource(MapFacade.SOURCE_ROUTE, FeatureCollection.fromFeatures(emptyArray())))

    style.addLayer(
        LineLayer("route-case", MapFacade.SOURCE_ROUTE).withProperties(
            PropertyFactory.lineColor("#6B2D2D"),
            PropertyFactory.lineWidth(7f),
            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
            PropertyFactory.lineOpacity(0.85f),
        ),
    )
    style.addLayer(
        LineLayer("route-line", MapFacade.SOURCE_ROUTE).withProperties(
            PropertyFactory.lineColor("#C9A227"),
            PropertyFactory.lineWidth(3.4f),
            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
        ),
    )
    style.addLayer(
        SymbolLayer(MapFacade.LAYER_POI, MapFacade.SOURCE_POI).withProperties(
            PropertyFactory.iconImage(Expression.get("icon")),
            PropertyFactory.iconSize(
                Expression.interpolate(
                    Expression.linear(),
                    Expression.zoom(),
                    Expression.literal(5),
                    Expression.literal(0.26),
                    Expression.literal(10),
                    Expression.literal(0.38),
                    Expression.literal(14),
                    Expression.literal(0.50),
                ),
            ),
            PropertyFactory.iconAllowOverlap(true),
            PropertyFactory.iconIgnorePlacement(true),
            PropertyFactory.iconAnchor(Property.ICON_ANCHOR_CENTER),
        ),
    )
    style.addLayer(
        SymbolLayer("user-layer", MapFacade.SOURCE_USER).withProperties(
            PropertyFactory.iconImage("pilgrim"),
            PropertyFactory.iconSize(0.42f),
            PropertyFactory.iconAllowOverlap(true),
            PropertyFactory.iconAnchor(Property.ICON_ANCHOR_CENTER),
        ),
    )
}

private fun installIlluminations(style: Style) {
    style.addSource(
        GeoJsonSource(MapFacade.SOURCE_ILLUM, FeatureCollection.fromFeatures(emptyArray())),
    )
    style.addLayerBelow(
        SymbolLayer(MapFacade.LAYER_ILLUM, MapFacade.SOURCE_ILLUM)
            .withProperties(
                PropertyFactory.iconImage(Expression.get("illum")),
                PropertyFactory.iconSize(
                    Expression.interpolate(
                        Expression.linear(),
                        Expression.zoom(),
                        Expression.literal(4),
                        Expression.literal(0.38),
                        Expression.literal(7),
                        Expression.literal(0.72),
                        Expression.literal(10),
                        Expression.literal(0.55),
                        Expression.literal(12),
                        Expression.literal(0.18),
                    ),
                ),
                PropertyFactory.iconOpacity(
                    Expression.interpolate(
                        Expression.linear(),
                        Expression.zoom(),
                        Expression.literal(4),
                        Expression.literal(0.95),
                        Expression.literal(10.5),
                        Expression.literal(0.85),
                        Expression.literal(12.2),
                        Expression.literal(0.0),
                    ),
                ),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconIgnorePlacement(true),
                PropertyFactory.iconAnchor(Property.ICON_ANCHOR_CENTER),
            ),
        MapFacade.LAYER_POI,
    )
}
