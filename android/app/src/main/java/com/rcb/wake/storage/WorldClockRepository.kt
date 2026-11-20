package com.rcb.wake.storage

import android.content.ContentValues
import android.content.Context
import java.time.ZoneId

data class WorldClock(val id: Long, val cityName: String, val zoneId: String, val displayOrder: Int)

class WorldClockRepository(context: Context) {
    private val db = WakeDatabase.get(context)

    fun getAll(): List<WorldClock> =
        db.readableDatabase.query("world_clocks", null, null, null, null, null, "display_order, id").use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(WorldClock(c.getLong(0), c.getString(1), c.getString(2), c.getInt(3)))
                }
            }
        }

    fun add(cityName: String, zoneId: String): WorldClock {
        ZoneId.of(zoneId) // Throws for unknown IANA identifiers.
        val name = cityName.trim().take(80)
        require(name.isNotEmpty()) { "City name required" }
        getAll().firstOrNull { it.zoneId == zoneId && it.cityName == name }?.let { return it }
        val order = (getAll().maxOfOrNull { it.displayOrder } ?: -1) + 1
        val id = db.writableDatabase.insertOrThrow(
            "world_clocks",
            null,
            ContentValues().apply {
                put("city_name", name)
                put("zone_id", zoneId)
                put("display_order", order)
            },
        )
        return WorldClock(id, name, zoneId, order)
    }

    fun remove(id: Long) {
        db.writableDatabase.delete("world_clocks", "id = ?", arrayOf(id.toString()))
    }
}
