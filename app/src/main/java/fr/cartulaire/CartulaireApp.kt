package fr.cartulaire

import android.app.Application
import org.maplibre.android.MapLibre

class CartulaireApp : Application() {
    override fun onCreate() {
        super.onCreate()
        MapLibre.getInstance(this)
    }
}
