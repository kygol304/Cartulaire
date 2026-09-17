package fr.cartulaire.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.cartulaire.data.AdminArea
import fr.cartulaire.data.CityIllum
import fr.cartulaire.data.ExploreLevel
import fr.cartulaire.data.Geo
import fr.cartulaire.data.HeritageSite
import fr.cartulaire.data.HeritageStore
import fr.cartulaire.data.IlluminatedPlaces
import fr.cartulaire.data.LatLon
import fr.cartulaire.data.LocationRepository
import fr.cartulaire.data.MapBounds
import fr.cartulaire.data.RouteRepository
import fr.cartulaire.data.SiteKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MapUiState(
    val sites: List<HeritageSite> = emptyList(),
    val kinds: Set<SiteKind> = SiteKind.entries.toSet(),
    val selected: HeritageSite? = null,
    val user: LatLon? = null,
    val search: String = "",
    val loading: Boolean = false,
    val banner: String? = "Touchez une province du royaume",
    val route: List<LatLon> = emptyList(),
    val cameraTarget: LatLon? = null,
    val cameraZoom: Double? = null,
    val cameraBounds: MapBounds? = null,
    val recenterNonce: Int = 0,
    val zoom: Double = 5.6,
    val cityIllum: CityIllum? = null,
    val catalogCount: Int = 0,
    val visibleCount: Int = 0,
    val showCities: Boolean = true,
    val showPeople: Boolean = true,
    val showMacabre: Boolean = true,
    val level: ExploreLevel = ExploreLevel.KINGDOM,
    val region: AdminArea? = null,
    val department: AdminArea? = null,
    val regionChoices: List<AdminArea> = emptyList(),
    val departmentChoices: List<AdminArea> = emptyList(),
)

class MapViewModel(application: Application) : AndroidViewModel(application) {
    private val store = runCatching { HeritageStore(application) }.getOrNull()
    private val routes = RouteRepository()
    private val location = LocationRepository(application)

    private val _state = MutableStateFlow(
        MapUiState(
            catalogCount = store?.count() ?: 0,
            banner = "Choisissez une province dans le menu",
            regionChoices = store?.allRegions().orEmpty(),
        ),
    )
    val state: StateFlow<MapUiState> = _state.asStateFlow()

    private var lastBounds: MapBounds? = null

    init {
        if (location.hasPermission()) startLocation()
    }

    fun startLocation() {
        if (!location.hasPermission()) return
        viewModelScope.launch {
            location.updates().collect { pos ->
                _state.update { it.copy(user = pos) }
            }
        }
    }

    fun onSearch(query: String) {
        _state.update { it.copy(search = query) }
        lastBounds?.let { refreshLocal(it) }
    }

    fun toggleKind(kind: SiteKind) {
        _state.update { current ->
            val next = current.kinds.toMutableSet()
            if (kind in next) next.remove(kind) else next.add(kind)
            current.copy(kinds = next, selected = current.selected?.takeIf { it.kind in next })
        }
        lastBounds?.let { refreshLocal(it) }
    }

    fun toggleCities() { _state.update { it.copy(showCities = !it.showCities) } }
    fun togglePeople() { _state.update { it.copy(showPeople = !it.showPeople) } }
    fun toggleMacabre() { _state.update { it.copy(showMacabre = !it.showMacabre) } }

    fun select(site: HeritageSite?) {
        _state.update {
            it.copy(
                selected = site,
                cameraTarget = site?.let { s -> LatLon(s.lat, s.lon) },
                cameraZoom = if (site != null) 15.0 else null,
                route = if (site == null) emptyList() else it.route,
            )
        }
    }

    fun selectRegion(code: String, name: String) {
        val area = store?.region(code.trim()) ?: return
        _state.update {
            it.copy(
                level = ExploreLevel.REGION,
                region = area.copy(name = name.ifBlank { area.name }),
                department = null,
                sites = emptyList(),
                selected = null,
                cameraBounds = area.bounds(7.0),
                banner = "Choisissez un département de ${area.name}",
                visibleCount = 0,
                departmentChoices = store?.departmentsOf(area.code).orEmpty(),
            )
        }
    }

    fun selectDepartment(code: String, name: String) {
        val area = store?.department(code) ?: return
        val region = area.parent?.let { store?.region(it) } ?: _state.value.region
        _state.update {
            it.copy(
                level = ExploreLevel.DEPARTMENT,
                region = region,
                department = area.copy(name = name.ifBlank { area.name }),
                selected = null,
                cameraBounds = area.bounds(9.2),
            )
        }
        lastBounds = area.bounds(9.2)
        refreshLocal(area.bounds(9.2))
    }

    fun goToKingdom() {
        while (_state.value.level != ExploreLevel.KINGDOM) goBack()
    }

    fun goBack() {
        when (_state.value.level) {
            ExploreLevel.DEPARTMENT -> {
                val region = _state.value.region
                _state.update {
                    it.copy(
                        level = ExploreLevel.REGION,
                        department = null,
                        sites = emptyList(),
                        selected = null,
                        cameraBounds = region?.bounds(7.0),
                        banner = "Choisissez un département de ${region?.name ?: "la province"}",
                        visibleCount = 0,
                        departmentChoices = region?.code?.let { store?.departmentsOf(it) }.orEmpty(),
                    )
                }
            }
            ExploreLevel.REGION -> {
                _state.update {
                    it.copy(
                        level = ExploreLevel.KINGDOM,
                        region = null,
                        department = null,
                        sites = emptyList(),
                        selected = null,
                        cameraTarget = LatLon(46.6, 2.2),
                        cameraZoom = 5.6,
                        cameraBounds = null,
                        banner = "Choisissez une province dans le menu",
                        departmentChoices = emptyList(),
                        regionChoices = store?.allRegions().orEmpty(),
                        visibleCount = 0,
                    )
                }
            }
            ExploreLevel.KINGDOM -> Unit
        }
    }

    fun recenter() {
        val user = _state.value.user ?: return
        _state.update {
            it.copy(
                cameraTarget = user,
                cameraZoom = 13.5,
                recenterNonce = it.recenterNonce + 1,
            )
        }
    }

    fun onCameraIdle(bounds: MapBounds) {
        lastBounds = bounds
        if (_state.value.level == ExploreLevel.DEPARTMENT) {
            refreshLocal(bounds)
        } else {
            _state.update { it.copy(zoom = bounds.zoom, sites = emptyList()) }
        }
    }

    fun requestWalk() {
        val selected = _state.value.selected ?: return
        val user = _state.value.user
        if (user == null) {
            _state.update { it.copy(banner = "Accordez le lieu de votre pèlerinage (GPS).") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, banner = "Traçant le chemin…") }
            try {
                val line = routes.walkingRoute(user, LatLon(selected.lat, selected.lon))
                val meters = Geo.haversineMeters(user, LatLon(selected.lat, selected.lon))
                _state.update {
                    it.copy(
                        route = line,
                        loading = false,
                        banner = "Chemin à pied · ${Geo.formatDistance(meters)} à vol d'oiseau",
                    )
                }
            } catch (_: Exception) {
                _state.update { it.copy(loading = false, banner = "Le chemin n'a pu être enluminé.") }
            }
        }
    }

    fun consumeCamera() {
        _state.update { it.copy(cameraTarget = null, cameraZoom = null, cameraBounds = null) }
    }

    private fun refreshLocal(bounds: MapBounds) {
        val s = _state.value
        if (s.level != ExploreLevel.DEPARTMENT) {
            _state.update { it.copy(sites = emptyList(), zoom = bounds.zoom) }
            return
        }
        val dept = s.department?.code ?: return
        val local = store?.query(bounds, s.kinds, dept).orEmpty().filter { it.matches(s.search) }
        val counts = store?.countInDept(dept, s.kinds).orEmpty()
        val summary = SiteKind.entries
            .mapNotNull { kind -> counts[kind]?.let { n -> "$n ${kind.label.lowercase()}" } }
            .joinToString(" · ")
        val city = if (bounds.zoom >= 11.2) {
            IlluminatedPlaces.nearest(
                (bounds.north + bounds.south) / 2.0,
                (bounds.east + bounds.west) / 2.0,
                maxKm = 14.0,
            )
        } else {
            null
        }
        _state.update {
            it.copy(
                sites = local,
                zoom = bounds.zoom,
                cityIllum = city,
                visibleCount = local.size,
                catalogCount = store?.count() ?: 0,
                banner = if (summary.isBlank()) {
                    "Nul lieu de ce type en ${s.department?.name}."
                } else {
                    "${s.department?.name} · $summary"
                },
            )
        }
    }
}
