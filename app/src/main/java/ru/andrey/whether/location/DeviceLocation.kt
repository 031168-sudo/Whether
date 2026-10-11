package ru.andrey.whether.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager

data class Coordinates(val latitude: Double, val longitude: Double)

/**
 * Последнее известное местоположение устройства. Разрешение нужно запросить
 * в UI до вызова; если его нет или координаты неизвестны, возвращает null.
 */
object DeviceLocation {

    @SuppressLint("MissingPermission")
    fun lastKnown(context: Context): Coordinates? {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        val location = providers
            .mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
            ?: return null
        return Coordinates(location.latitude, location.longitude)
    }
}
