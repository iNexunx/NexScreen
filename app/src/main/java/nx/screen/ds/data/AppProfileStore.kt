package nx.screen.ds.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "screendensity")

data class AppProfile(
    val packageName: String,
    val density: Int? = null,
    val width: Int? = null,
    val height: Int? = null,
) {
    fun isEmpty(): Boolean = density == null && width == null && height == null

    fun serialize(): String = buildList {
        density?.let { add("d=$it") }
        width?.let { add("w=$it") }
        height?.let { add("h=$it") }
    }.joinToString(";")

    companion object {
        fun deserialize(pkg: String, raw: String): AppProfile {
            var density: Int? = null
            var w: Int? = null
            var h: Int? = null
            raw.split(";").forEach { pair ->
                val kv = pair.split("=")
                if (kv.size == 2) {
                    when (kv[0]) {
                        "d" -> kv[1].toIntOrNull()?.let { density = it }
                        "w" -> kv[1].toIntOrNull()?.let { w = it }
                        "h" -> kv[1].toIntOrNull()?.let { h = it }
                    }
                }
            }
            return AppProfile(pkg, density, w, h)
        }
    }
}

class AppProfileStore(private val context: Context) {

    val profiles: Flow<Map<String, AppProfile>> = context.dataStore.data.map { prefs ->
        prefs.asMap()
            .mapNotNull { (key, value) ->
                if (key.name.startsWith(PREFIX)) {
                    val pkg = key.name.removePrefix(PREFIX)
                    pkg to AppProfile.deserialize(pkg, value as? String ?: "")
                } else null
            }
            .filter { !it.second.isEmpty() }
            .toMap()
    }

    suspend fun get(pkg: String): AppProfile {
        val key = key(pkg)
        val raw = context.dataStore.data.first()[key] ?: ""
        return AppProfile.deserialize(pkg, raw)
    }

    suspend fun set(profile: AppProfile) {
        context.dataStore.edit { prefs ->
            if (profile.isEmpty()) {
                prefs.remove(key(profile.packageName))
            } else {
                prefs[key(profile.packageName)] = profile.serialize()
            }
        }
    }

    suspend fun remove(pkg: String) {
        context.dataStore.edit { prefs -> prefs.remove(key(pkg)) }
    }

    suspend fun clearAll() {
        context.dataStore.edit { prefs ->
            prefs.asMap().keys
                .filter { it.name.startsWith(PREFIX) }
                .forEach { prefs.remove(it) }
        }
    }

    private fun key(pkg: String) = stringPreferencesKey(PREFIX + pkg)

    companion object {
        private const val PREFIX = "profile_"
    }
}
