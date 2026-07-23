package com.divoomspeed.backpack.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "divoom_speed_settings")

data class AppSettings(
    val deviceAddress: String = "",
    val deviceName: String = "",
    val resolutionWidth: Int = 16,
    val resolutionHeight: Int = 16,
    val rotationDegrees: Int = 0,
    val brightness: Int = 80,
    val showUnit: Boolean = true,
    val colorMode: String = "SPEED_BASED",
    val updateIntervalMs: Long = 1000L,
    val speedSmoothing: Boolean = true,
    val autoReconnect: Boolean = true,
    val customRfcommUuid: String = "00001101-0000-1000-8000-00805F9B34FB",
    val customRfcommChannel: Int = 1,
    val bleWriteCharacteristic: String = "0000ffe1-0000-1000-8000-00805f9b34fb",
    val bleWriteWithoutResponse: Boolean = true,
    val debugMode: Boolean = false,
    val demoMode: Boolean = false
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val DEVICE_ADDRESS = stringPreferencesKey("device_address")
        val DEVICE_NAME = stringPreferencesKey("device_name")
        val RESOLUTION_WIDTH = intPreferencesKey("resolution_width")
        val RESOLUTION_HEIGHT = intPreferencesKey("resolution_height")
        val ROTATION_DEGREES = intPreferencesKey("rotation_degrees")
        val BRIGHTNESS = intPreferencesKey("brightness")
        val SHOW_UNIT = booleanPreferencesKey("show_unit")
        val COLOR_MODE = stringPreferencesKey("color_mode")
        val UPDATE_INTERVAL_MS = longPreferencesKey("update_interval_ms")
        val SPEED_SMOOTHING = booleanPreferencesKey("speed_smoothing")
        val AUTO_RECONNECT = booleanPreferencesKey("auto_reconnect")
        val CUSTOM_RFCOMM_UUID = stringPreferencesKey("custom_rfcomm_uuid")
        val CUSTOM_RFCOMM_CHANNEL = intPreferencesKey("custom_rfcomm_channel")
        val BLE_WRITE_CHARACTERISTIC = stringPreferencesKey("ble_write_characteristic")
        val BLE_WRITE_WITHOUT_RESPONSE = booleanPreferencesKey("ble_write_without_response")
        val DEBUG_MODE = booleanPreferencesKey("debug_mode")
        val DEMO_MODE = booleanPreferencesKey("demo_mode")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            deviceAddress = prefs[Keys.DEVICE_ADDRESS] ?: "",
            deviceName = prefs[Keys.DEVICE_NAME] ?: "",
            resolutionWidth = prefs[Keys.RESOLUTION_WIDTH] ?: 16,
            resolutionHeight = prefs[Keys.RESOLUTION_HEIGHT] ?: 16,
            rotationDegrees = prefs[Keys.ROTATION_DEGREES] ?: 0,
            brightness = prefs[Keys.BRIGHTNESS] ?: 80,
            showUnit = prefs[Keys.SHOW_UNIT] ?: true,
            colorMode = prefs[Keys.COLOR_MODE] ?: "SPEED_BASED",
            updateIntervalMs = prefs[Keys.UPDATE_INTERVAL_MS] ?: 1000L,
            speedSmoothing = prefs[Keys.SPEED_SMOOTHING] ?: true,
            autoReconnect = prefs[Keys.AUTO_RECONNECT] ?: true,
            customRfcommUuid = prefs[Keys.CUSTOM_RFCOMM_UUID] ?: "00001101-0000-1000-8000-00805F9B34FB",
            customRfcommChannel = prefs[Keys.CUSTOM_RFCOMM_CHANNEL] ?: 1,
            bleWriteCharacteristic = prefs[Keys.BLE_WRITE_CHARACTERISTIC] ?: "0000ffe1-0000-1000-8000-00805f9b34fb",
            bleWriteWithoutResponse = prefs[Keys.BLE_WRITE_WITHOUT_RESPONSE] ?: true,
            debugMode = prefs[Keys.DEBUG_MODE] ?: false,
            demoMode = prefs[Keys.DEMO_MODE] ?: false
        )
    }

    suspend fun saveSelectedDevice(address: String, name: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DEVICE_ADDRESS] = address
            prefs[Keys.DEVICE_NAME] = name
        }
    }

    suspend fun updateResolution(width: Int, height: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.RESOLUTION_WIDTH] = width
            prefs[Keys.RESOLUTION_HEIGHT] = height
        }
    }

    suspend fun updateRotation(degrees: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ROTATION_DEGREES] = degrees
        }
    }

    suspend fun updateBrightness(brightness: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BRIGHTNESS] = brightness
        }
    }

    suspend fun updateShowUnit(show: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SHOW_UNIT] = show
        }
    }

    suspend fun updateColorMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.COLOR_MODE] = mode
        }
    }

    suspend fun updateInterval(intervalMs: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.UPDATE_INTERVAL_MS] = intervalMs
        }
    }

    suspend fun updateSpeedSmoothing(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SPEED_SMOOTHING] = enabled
        }
    }

    suspend fun updateAutoReconnect(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTO_RECONNECT] = enabled
        }
    }

    suspend fun updateRfcommSettings(uuid: String, channel: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CUSTOM_RFCOMM_UUID] = uuid
            prefs[Keys.CUSTOM_RFCOMM_CHANNEL] = channel
        }
    }

    suspend fun updateBleSettings(characteristicUuid: String, withoutResponse: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BLE_WRITE_CHARACTERISTIC] = characteristicUuid
            prefs[Keys.BLE_WRITE_WITHOUT_RESPONSE] = withoutResponse
        }
    }

    suspend fun updateDebugMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DEBUG_MODE] = enabled
        }
    }

    suspend fun updateDemoMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DEMO_MODE] = enabled
        }
    }
}
