package com.iashegh.schoolplanner.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.iashegh.schoolplanner.ui.theme.Skin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

enum class CycleMode { WEEKLY, AB, ROTATING }

enum class SyncRole { NONE, PARENT, CHILD }

data class Settings(
    val skin: Skin = Skin.GALACTIC,
    val soundOn: Boolean = true,
    val hapticsOn: Boolean = true,
    val decoderMode: Boolean = false,
    val firstDay: DayOfWeek = DayOfWeek.MONDAY,
    /** bit (dayOfWeek.value - 1) set = school day */
    val schoolDaysMask: Int = 0b0011111,
    val cycleMode: CycleMode = CycleMode.WEEKLY,
    val cycleLength: Int = 6,
    /** First day of week A (AB mode) or Day 1 of the cycle (rotating mode) */
    val anchorEpochDay: Long = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toEpochDay(),
    val morningAlertOn: Boolean = true,
    val morningMinute: Int = 7 * 60,
    val pinHash: String? = null,
    val pinSalt: String? = null,
    val syncRole: SyncRole = SyncRole.NONE,
    val familyCode: String? = null,
) {
    val anchor: LocalDate get() = LocalDate.ofEpochDay(anchorEpochDay)
    val hasPin: Boolean get() = pinHash != null
    fun isSchoolDay(d: DayOfWeek) = schoolDaysMask and (1 shl (d.value - 1)) != 0
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("planner_settings")

class SettingsRepository(private val context: Context) {
    private object K {
        val skin = stringPreferencesKey("skin")
        val sound = booleanPreferencesKey("sound")
        val haptics = booleanPreferencesKey("haptics")
        val decoder = booleanPreferencesKey("decoder")
        val firstDay = intPreferencesKey("first_day")
        val mask = intPreferencesKey("school_days_mask")
        val mode = stringPreferencesKey("cycle_mode")
        val length = intPreferencesKey("cycle_length")
        val anchor = longPreferencesKey("anchor")
        val morningOn = booleanPreferencesKey("morning_on")
        val morningMin = intPreferencesKey("morning_min")
        val pinHash = stringPreferencesKey("pin_hash")
        val pinSalt = stringPreferencesKey("pin_salt")
        val syncRole = stringPreferencesKey("sync_role")
        val familyCode = stringPreferencesKey("family_code")
    }

    val flow: Flow<Settings> = context.dataStore.data.map { p ->
        val d = Settings()
        Settings(
            skin = p[K.skin]?.let { runCatching { Skin.valueOf(it) }.getOrNull() } ?: d.skin,
            soundOn = p[K.sound] ?: d.soundOn,
            hapticsOn = p[K.haptics] ?: d.hapticsOn,
            decoderMode = p[K.decoder] ?: d.decoderMode,
            firstDay = p[K.firstDay]?.let { DayOfWeek.of(it) } ?: d.firstDay,
            schoolDaysMask = p[K.mask] ?: d.schoolDaysMask,
            cycleMode = p[K.mode]?.let { runCatching { CycleMode.valueOf(it) }.getOrNull() } ?: d.cycleMode,
            cycleLength = p[K.length] ?: d.cycleLength,
            anchorEpochDay = p[K.anchor] ?: d.anchorEpochDay,
            morningAlertOn = p[K.morningOn] ?: d.morningAlertOn,
            morningMinute = p[K.morningMin] ?: d.morningMinute,
            pinHash = p[K.pinHash],
            pinSalt = p[K.pinSalt],
            syncRole = p[K.syncRole]?.let { runCatching { SyncRole.valueOf(it) }.getOrNull() } ?: SyncRole.NONE,
            familyCode = p[K.familyCode],
        )
    }

    suspend fun setSkin(v: Skin) = context.dataStore.edit { it[K.skin] = v.name }
    suspend fun setSound(v: Boolean) = context.dataStore.edit { it[K.sound] = v }
    suspend fun setHaptics(v: Boolean) = context.dataStore.edit { it[K.haptics] = v }
    suspend fun setDecoder(v: Boolean) = context.dataStore.edit { it[K.decoder] = v }
    suspend fun setMorning(on: Boolean, minute: Int) = context.dataStore.edit {
        it[K.morningOn] = on
        it[K.morningMin] = minute
    }

    suspend fun setCalendar(
        firstDay: DayOfWeek,
        mask: Int,
        mode: CycleMode,
        length: Int,
        anchor: LocalDate,
    ) = context.dataStore.edit {
        it[K.firstDay] = firstDay.value
        it[K.mask] = mask
        it[K.mode] = mode.name
        it[K.length] = length.coerceIn(2, 12)
        it[K.anchor] = anchor.toEpochDay()
    }

    suspend fun setPin(hash: String, salt: String) = context.dataStore.edit {
        it[K.pinHash] = hash
        it[K.pinSalt] = salt
    }

    suspend fun setSync(role: SyncRole, code: String?) = context.dataStore.edit {
        it[K.syncRole] = role.name
        if (code == null) it.remove(K.familyCode) else it[K.familyCode] = code
    }

    suspend fun clearAll() = context.dataStore.edit { it.clear() }
}
