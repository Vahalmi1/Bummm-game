package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.AppPrefs
import com.example.model.Difficulty
import com.example.model.PlayerSlot
import com.example.model.SlotType
import org.json.JSONArray
import org.json.JSONObject

class PreferencesRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("bummm_v1", Context.MODE_PRIVATE)

    fun load(): AppPrefs {
        val raw = prefs.getString("state", null) ?: return AppPrefs()
        return try {
            val o = JSONObject(raw)
            val diffName = o.optString("diff", "normal").uppercase()
            val diff = Difficulty.entries.firstOrNull { it.name == diffName } ?: Difficulty.NORMAL
            val slotsJson = o.optJSONArray("slots")
            val slots = if (slotsJson != null && slotsJson.length() == 6) {
                List(6) { idx ->
                    val s = slotsJson.optJSONObject(idx)
                    val tName = (s?.optString("type", "off") ?: "off").uppercase()
                    val type = SlotType.entries.firstOrNull { it.name == tName } ?: SlotType.OFF
                    PlayerSlot(
                        type = type,
                        name = s?.optString("name", "Player ${idx + 1}") ?: "Player ${idx + 1}",
                        av = (s?.optInt("av", idx) ?: idx).coerceIn(0, 5)
                    )
                }
            } else {
                AppPrefs.defaultSlots()
            }
            AppPrefs(
                sfx = o.optBoolean("sfx", true),
                music = o.optBoolean("music", true),
                vibrate = o.optBoolean("vibrate", true),
                fx = o.optBoolean("fx", true),
                diff = diff,
                startLevel = o.optInt("startLevel", 1).coerceIn(1, 99),
                hints = o.optInt("hints", 0),
                bestLevel = o.optInt("bestLevel", 1).coerceAtLeast(1),
                bestScore = o.optInt("bestScore", 0).coerceAtLeast(0),
                games = o.optInt("games", 0).coerceAtLeast(0),
                slots = slots
            )
        } catch (_: Exception) {
            AppPrefs()
        }
    }

    fun save(state: AppPrefs) {
        try {
            val slotsArr = JSONArray()
            state.slots.forEach { slot ->
                slotsArr.put(
                    JSONObject().apply {
                        put("type", slot.type.name.lowercase())
                        put("name", slot.name)
                        put("av", slot.av)
                    }
                )
            }
            val root = JSONObject().apply {
                put("sfx", state.sfx)
                put("music", state.music)
                put("vibrate", state.vibrate)
                put("fx", state.fx)
                put("diff", state.diff.name.lowercase())
                put("startLevel", state.startLevel)
                put("hints", state.hints)
                put("bestLevel", state.bestLevel)
                put("bestScore", state.bestScore)
                put("games", state.games)
                put("slots", slotsArr)
            }
            prefs.edit().putString("state", root.toString()).apply()
        } catch (_: Exception) {
        }
    }
}
