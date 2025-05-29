package com.vibus.live.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log

object MapsConfigChecker {

    private const val TAG = "MapsConfigChecker"

    fun checkGoogleMapsConfiguration(context: Context): MapsConfigResult {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(
                context.packageName,
                PackageManager.GET_META_DATA
            )

            val metaData = appInfo.metaData
            if (metaData == null) {
                Log.e(TAG, "No meta-data found in AndroidManifest.xml")
                return MapsConfigResult.NoMetaData
            }

            val apiKey = metaData.getString("com.google.android.geo.API_KEY")
            Log.d(TAG, "Found API key in manifest: ${apiKey?.take(20)}...")

            when {
                apiKey.isNullOrBlank() -> {
                    Log.e(TAG, "API key is null or blank")
                    MapsConfigResult.MissingApiKey
                }
                apiKey == "\${MAPS_API_KEY}" -> {
                    Log.e(TAG, "API key is placeholder - not replaced during build")
                    MapsConfigResult.PlaceholderApiKey
                }
                apiKey == "YOUR_ACTUAL_API_KEY_HERE" -> {
                    Log.e(TAG, "API key is template - replace with real key")
                    MapsConfigResult.TemplateApiKey
                }
                apiKey.length < 30 -> {
                    Log.e(TAG, "API key seems too short: ${apiKey.length} chars")
                    MapsConfigResult.InvalidApiKey(apiKey)
                }
                else -> {
                    Log.i(TAG, "API key seems valid: ${apiKey.length} chars")
                    MapsConfigResult.ValidApiKey(apiKey)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Google Maps configuration", e)
            MapsConfigResult.Error(e.message ?: "Unknown error")
        }
    }

    fun checkGooglePlayServices(context: Context): Boolean {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo("com.google.android.gms", 0)
            Log.d(TAG, "Google Play Services found: ${appInfo.enabled}")
            appInfo.enabled
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Google Play Services not found")
            false
        }
    }

    fun getConfigurationReport(context: Context): String {
        val configResult = checkGoogleMapsConfiguration(context)
        val hasPlayServices = checkGooglePlayServices(context)

        return buildString {
            appendLine("=== Google Maps Configuration Report ===")
            appendLine()

            appendLine("📱 App Package: ${context.packageName}")
            appendLine("🔧 Google Play Services: ${if (hasPlayServices) "✓ Available" else "✗ Missing"}")
            appendLine()

            when (configResult) {
                is MapsConfigResult.ValidApiKey -> {
                    appendLine("🔑 API Key: ✓ Valid (${configResult.key.length} chars)")
                    appendLine("   Preview: ${configResult.key.take(20)}...")
                }
                is MapsConfigResult.InvalidApiKey -> {
                    appendLine("🔑 API Key: ⚠️ Invalid or too short")
                    appendLine("   Key: ${configResult.key}")
                }
                MapsConfigResult.MissingApiKey -> {
                    appendLine("🔑 API Key: ✗ Missing or blank")
                }
                MapsConfigResult.PlaceholderApiKey -> {
                    appendLine("🔑 API Key: ✗ Placeholder not replaced")
                    appendLine("   Configure local.properties with MAPS_API_KEY")
                }
                MapsConfigResult.TemplateApiKey -> {
                    appendLine("🔑 API Key: ✗ Template value")
                    appendLine("   Replace YOUR_ACTUAL_API_KEY_HERE with real key")
                }
                MapsConfigResult.NoMetaData -> {
                    appendLine("🔑 API Key: ✗ No meta-data in manifest")
                }
                is MapsConfigResult.Error -> {
                    appendLine("🔑 API Key: ✗ Error checking config")
                    appendLine("   Error: ${configResult.message}")
                }
            }

            appendLine()
            appendLine("=== Recommendations ===")

            if (!hasPlayServices) {
                appendLine("• Install Google Play Services on device/emulator")
            }

            when (configResult) {
                is MapsConfigResult.ValidApiKey -> {
                    appendLine("• Configuration looks good!")
                    appendLine("• If maps still don't work, check API restrictions")
                }
                else -> {
                    appendLine("• Get API key from Google Cloud Console")
                    appendLine("• Enable 'Maps SDK for Android' API")
                    appendLine("• Add API key to AndroidManifest.xml")
                    appendLine("• For restrictions, add app SHA-1 fingerprint")
                }
            }
        }
    }
}

sealed class MapsConfigResult {
    data class ValidApiKey(val key: String) : MapsConfigResult()
    data class InvalidApiKey(val key: String) : MapsConfigResult()
    data class Error(val message: String) : MapsConfigResult()
    object MissingApiKey : MapsConfigResult()
    object PlaceholderApiKey : MapsConfigResult()
    object TemplateApiKey : MapsConfigResult()
    object NoMetaData : MapsConfigResult()
}