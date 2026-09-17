package fr.cartulaire.ui.map

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.cartulaire.R
import fr.cartulaire.data.CityIllum
import fr.cartulaire.data.ExploreLevel
import fr.cartulaire.data.Geo
import fr.cartulaire.data.HeritageSite
import fr.cartulaire.data.IlluminatedPlaces
import fr.cartulaire.data.LatLon
import fr.cartulaire.data.SiteKind
import fr.cartulaire.ui.components.GoldRule
import fr.cartulaire.ui.components.ManuscriptCorners
import fr.cartulaire.ui.components.ParchmentPanel
import fr.cartulaire.ui.theme.CinzelFamily
import fr.cartulaire.ui.theme.CormorantFamily
import fr.cartulaire.ui.theme.Gold
import fr.cartulaire.ui.theme.Ink
import fr.cartulaire.ui.theme.InkSoft
import fr.cartulaire.ui.theme.Oxblood
import fr.cartulaire.ui.theme.Vellum
import fr.cartulaire.vm.MapViewModel

@Composable
fun MapScreen(vm: MapViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var legendOpen by remember { mutableStateOf(true) }
    val permission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        if (result.values.any { it }) vm.startLocation()
    }

    LaunchedEffect(Unit) {
        permission.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ),
        )
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF3A2416))) {
        MedievalMapView(
            sites = state.sites,
            user = state.user,
            route = state.route,
            cameraTarget = state.cameraTarget,
            cameraZoom = state.cameraZoom,
            recenterNonce = state.recenterNonce,
            onBounds = vm::onCameraIdle,
            onSelectId = { id ->
                val site = state.sites.firstOrNull { it.id == id }
                vm.select(site)
            },
            onCameraConsumed = vm::consumeCamera,
            showCities = state.showCities && state.level != ExploreLevel.DEPARTMENT,
            showPeople = state.showPeople && state.level == ExploreLevel.KINGDOM,
            showMacabre = state.showMacabre && state.level == ExploreLevel.KINGDOM,
            level = state.level,
            regionCode = state.region?.code,
            deptCode = state.department?.code,
            cameraBounds = state.cameraBounds,
            onAdmin = { next, code, name ->
                when (next) {
                    ExploreLevel.REGION -> vm.selectRegion(code, name)
                    ExploreLevel.DEPARTMENT -> vm.selectDepartment(code, name)
                    ExploreLevel.KINGDOM -> vm.goBack()
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        Image(
            painterResource(R.drawable.illum_corner_tl),
            null,
            Modifier.align(Alignment.TopStart).size(110.dp),
            contentScale = ContentScale.Fit,
        )
        Image(
            painterResource(R.drawable.illum_corner_tr),
            null,
            Modifier.align(Alignment.TopEnd).size(110.dp),
            contentScale = ContentScale.Fit,
        )
        Image(
            painterResource(R.drawable.illum_corner_bl),
            null,
            Modifier.align(Alignment.BottomStart).size(110.dp),
            contentScale = ContentScale.Fit,
        )
        Image(
            painterResource(R.drawable.illum_corner_br),
            null,
            Modifier.align(Alignment.BottomEnd).size(110.dp),
            contentScale = ContentScale.Fit,
        )

        Column(
            Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth(),
        ) {
            ParchmentPanel(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        "CARTULAIRE",
                        fontFamily = CinzelFamily,
                        color = Oxblood,
                        fontSize = 20.sp,
                        letterSpacing = 3.sp,
                    )
                    Breadcrumb(
                        level = state.level,
                        region = state.region?.name,
                        department = state.department?.name,
                        onKingdom = vm::goToKingdom,
                        onRegion = {
                            if (state.level == ExploreLevel.DEPARTMENT) vm.goBack()
                        },
                    )
                    if (state.level == ExploreLevel.DEPARTMENT) {
                        Spacer(Modifier.height(6.dp))
                        BasicTextField(
                            value = state.search,
                            onValueChange = vm::onSearch,
                            singleLine = true,
                            cursorBrush = SolidColor(Oxblood),
                            textStyle = TextStyle(
                                fontFamily = CormorantFamily,
                                fontSize = 16.sp,
                                color = Ink,
                            ),
                            decorationBox = { inner ->
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Gold.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                ) {
                                    if (state.search.isBlank()) {
                                        Text(
                                            "Chercher un lieu…",
                                            fontFamily = CormorantFamily,
                                            color = Ink.copy(alpha = 0.45f),
                                            fontSize = 15.sp,
                                        )
                                    }
                                    inner()
                                }
                            },
                        )
                    }
                }
            }
            if (!state.banner.isNullOrBlank() || state.loading) {
                Spacer(Modifier.height(8.dp))
                ParchmentPanel(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (state.loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Oxblood,
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            state.banner ?: "Enluminure…",
                            fontFamily = CormorantFamily,
                            color = Ink,
                            fontSize = 14.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        Column(
            Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 118.dp, end = 10.dp)
                .width(220.dp),
        ) {
            if (legendOpen) {
                ParchmentPanel(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                "MENU",
                                fontFamily = CinzelFamily,
                                color = Oxblood,
                                fontSize = 15.sp,
                                letterSpacing = 2.sp,
                            )
                            Text(
                                "fermer",
                                fontFamily = CormorantFamily,
                                color = Oxblood,
                                modifier = Modifier.clickable { legendOpen = false },
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        GoldRule(Modifier.fillMaxWidth().height(1.dp))
                        Spacer(Modifier.height(8.dp))
                        when (state.level) {
                            ExploreLevel.KINGDOM -> {
                                Text("Provinces", fontFamily = CinzelFamily, color = Ink, fontSize = 12.sp)
                                state.regionChoices.forEach { area ->
                                    Text(
                                        area.name,
                                        fontFamily = CormorantFamily,
                                        color = Oxblood,
                                        fontSize = 16.sp,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { vm.selectRegion(area.code, area.name) }
                                            .padding(vertical = 4.dp),
                                    )
                                }
                            }
                            ExploreLevel.REGION -> {
                                Text("Départements", fontFamily = CinzelFamily, color = Ink, fontSize = 12.sp)
                                state.departmentChoices.forEach { area ->
                                    Text(
                                        area.name,
                                        fontFamily = CormorantFamily,
                                        color = Oxblood,
                                        fontSize = 16.sp,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { vm.selectDepartment(area.code, area.name) }
                                            .padding(vertical = 4.dp),
                                    )
                                }
                            }
                            ExploreLevel.DEPARTMENT -> {
                                Text("Lieux", fontFamily = CinzelFamily, color = Ink, fontSize = 12.sp)
                                LegendToggle("Églises", SiteKind.CHURCH in state.kinds) { vm.toggleKind(SiteKind.CHURCH) }
                                LegendToggle("Châteaux", SiteKind.CASTLE in state.kinds) { vm.toggleKind(SiteKind.CASTLE) }
                                LegendToggle("Villages", SiteKind.VILLAGE in state.kinds) { vm.toggleKind(SiteKind.VILLAGE) }
                                LegendToggle("Autels", SiteKind.ALTAR in state.kinds) { vm.toggleKind(SiteKind.ALTAR) }
                                LegendToggle("Sanctuaires", SiteKind.SANCTUARY in state.kinds) { vm.toggleKind(SiteKind.SANCTUARY) }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        GoldRule(Modifier.fillMaxWidth().height(1.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Enluminures", fontFamily = CinzelFamily, color = Ink, fontSize = 12.sp)
                        LegendToggle("Miniatures de cités", state.showCities, vm::toggleCities)
                        LegendToggle("Peuple et travaux", state.showPeople, vm::togglePeople)
                        LegendToggle("Danse macabre", state.showMacabre, vm::toggleMacabre)
                    }
                }
            } else {
                ParchmentPanel(Modifier.clickable { legendOpen = true }.fillMaxWidth()) {
                    Text(
                        "MENU",
                        fontFamily = CinzelFamily,
                        color = Oxblood,
                        fontSize = 16.sp,
                        letterSpacing = 3.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    )
                }
            }
        }

        Column(
            Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(12.dp),
        ) {
            CityCartouche(city = state.cityIllum, zoomedIn = state.zoom >= 11.2)
            Image(
                painterResource(R.drawable.compass_rose),
                "Rose des vents",
                Modifier.size(72.dp),
            )
            Image(
                painterResource(R.drawable.wax_seal),
                "Ma position",
                Modifier.size(68.dp).clickable { vm.recenter() },
            )
        }

        AnimatedVisibility(
            visible = state.selected != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(12.dp),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            state.selected?.let { site ->
                SiteCard(
                    site = site,
                    user = state.user,
                    onClose = { vm.select(null) },
                    onWalk = { vm.requestWalk() },
                    onExternal = {
                        val uri = Uri.parse("geo:${site.lat},${site.lon}?q=${Uri.encode(site.name)}")
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                    },
                    onWiki = {
                        val wiki = site.wikipedia ?: return@SiteCard
                        val url = if (wiki.startsWith("http")) wiki
                        else {
                            val parts = wiki.split(':', limit = 2)
                            if (parts.size == 2) {
                                "https://${parts[0]}.wikipedia.org/wiki/${parts[1].replace(' ', '_')}"
                            } else {
                                "https://fr.wikipedia.org/wiki/${wiki.replace(' ', '_')}"
                            }
                        }
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    },
                )
            }
        }
    }
}

@Composable
private fun Breadcrumb(
    level: ExploreLevel,
    region: String?,
    department: String?,
    onKingdom: () -> Unit,
    onRegion: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Royaume",
            fontFamily = CinzelFamily,
            color = Oxblood,
            fontSize = 13.sp,
            modifier = Modifier.clickable(onClick = onKingdom),
        )
        if (region != null && level != ExploreLevel.KINGDOM) {
            Text("  ›  ", color = InkSoft, fontSize = 13.sp)
            Text(
                region,
                fontFamily = CinzelFamily,
                color = Oxblood,
                fontSize = 13.sp,
                modifier = Modifier.clickable(onClick = onRegion),
            )
        }
        if (department != null && level == ExploreLevel.DEPARTMENT) {
            Text("  ›  ", color = InkSoft, fontSize = 13.sp)
            Text(department, fontFamily = CinzelFamily, color = Ink, fontSize = 13.sp)
        }
    }
}

@Composable
private fun LegendToggle(label: String, checked: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (checked) "▣" else "☐",
            color = if (checked) Oxblood else Ink.copy(alpha = 0.4f),
            fontSize = 16.sp,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            fontFamily = CormorantFamily,
            color = if (checked) Ink else Ink.copy(alpha = 0.45f),
            fontSize = 16.sp,
        )
    }
}

@Composable
private fun CityCartouche(city: CityIllum?, zoomedIn: Boolean) {
    AnimatedVisibility(visible = zoomedIn && city != null) {
        val illum = city ?: return@AnimatedVisibility
        val drawable = when (illum.icon) {
            IlluminatedPlaces.ICON_RIVER -> R.drawable.illum_city_river
            IlluminatedPlaces.ICON_VILLAGE -> R.drawable.illum_village
            IlluminatedPlaces.ICON_ABBEY -> R.drawable.illum_abbey
            else -> R.drawable.illum_city_wall
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Image(
                painterResource(drawable),
                contentDescription = illum.name,
                modifier = Modifier
                    .size(96.dp)
                    .shadow(6.dp, RoundedCornerShape(6.dp)),
            )
            Spacer(Modifier.width(6.dp))
            ParchmentPanel {
                Text(
                    illum.name,
                    fontFamily = CinzelFamily,
                    color = Ink,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(end = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun FilterRow(
    selected: Set<SiteKind>,
    onToggle: (SiteKind) -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            FilterSeal(SiteKind.CHURCH, R.drawable.marker_church, selected, onToggle)
            FilterSeal(SiteKind.CASTLE, R.drawable.marker_castle, selected, onToggle)
            FilterSeal(SiteKind.VILLAGE, R.drawable.marker_village, selected, onToggle)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            FilterSeal(SiteKind.ALTAR, R.drawable.marker_altar, selected, onToggle)
            FilterSeal(SiteKind.SANCTUARY, R.drawable.marker_sanctuary, selected, onToggle)
        }
    }
}

@Composable
private fun FilterSeal(
    kind: SiteKind,
    icon: Int,
    selected: Set<SiteKind>,
    onToggle: (SiteKind) -> Unit,
) {
    val on = kind in selected
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onToggle(kind) }
            .padding(4.dp),
    ) {
        Image(
            painterResource(icon),
            contentDescription = kind.label,
            modifier = Modifier
                .size(56.dp)
                .shadow(if (on) 8.dp else 0.dp, CircleShape)
                .clip(CircleShape)
                .border(if (on) 2.dp else 0.dp, Gold, CircleShape),
            contentScale = ContentScale.Fit,
        )
        Text(
            kind.label,
            fontFamily = CinzelFamily,
            fontSize = 10.sp,
            color = if (on) Ink else Ink.copy(alpha = 0.4f),
            letterSpacing = 0.4.sp,
        )
    }
}

@Composable
private fun SiteCard(
    site: HeritageSite,
    user: LatLon?,
    onClose: () -> Unit,
    onWalk: () -> Unit,
    onExternal: () -> Unit,
    onWiki: () -> Unit,
) {
    val distance = user?.let {
        Geo.formatDistance(Geo.haversineMeters(it, LatLon(site.lat, site.lon)))
    }
    ParchmentPanel(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val icon = when (site.kind) {
                    SiteKind.CHURCH -> R.drawable.marker_church
                    SiteKind.CASTLE -> R.drawable.marker_castle
                    SiteKind.VILLAGE -> R.drawable.marker_village
                    SiteKind.ALTAR -> R.drawable.marker_altar
                    SiteKind.SANCTUARY -> R.drawable.marker_sanctuary
                }
                Image(painterResource(icon), null, Modifier.size(52.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        site.name,
                        fontFamily = CinzelFamily,
                        fontSize = 16.sp,
                        color = Ink,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        listOfNotNull(site.kind.label.trimEnd('s'), site.subtitle, distance).joinToString(" · "),
                        fontFamily = CormorantFamily,
                        fontSize = 15.sp,
                        color = InkSoft,
                        maxLines = 2,
                    )
                }
                Text(
                    "✕",
                    color = Oxblood,
                    fontSize = 18.sp,
                    modifier = Modifier
                        .clickable(onClick = onClose)
                        .padding(8.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            GoldRule(Modifier.fillMaxWidth().height(1.dp))
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ActionInk("Chemin", onWalk)
                ActionInk("Boussole", onExternal)
                if (!site.wikipedia.isNullOrBlank()) {
                    ActionInk("Chronique", onWiki)
                }
            }
        }
    }
}

@Composable
private fun ActionInk(label: String, onClick: () -> Unit) {
    Text(
        label.uppercase(),
        fontFamily = CinzelFamily,
        fontSize = 12.sp,
        letterSpacing = 1.4.sp,
        color = Oxblood,
        modifier = Modifier
            .border(1.dp, Gold, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}
