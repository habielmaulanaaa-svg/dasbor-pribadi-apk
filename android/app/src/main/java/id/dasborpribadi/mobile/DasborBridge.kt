package id.dasborpribadi.mobile

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.browser.customtabs.CustomTabsIntent
import org.json.JSONObject

/**
 * DasborBridge (Kotlin)
 * Jembatan @JavascriptInterface Native Android yang menghubungkan aplikasi web
 * (window.DasborBridge) dengan penyimpanan SharedPreferences Native,
 * Widget Layar Utama (4x2 Kartu Saldo & 4x1 Bar Aksi), Google OAuth Custom Tabs,
 * dan Mesin Pembaruan OTA (Over-The-Air).
 */
class DasborBridge(
    private val context: Context,
    private val webView: WebView
) {

    companion object {
        const val PREFS_NAME = "DasborNativePrefs"
        const val AUTH_BRIDGE_URL = "https://habielmaulanaaa-svg.github.io/dasbor-pribadi/auth-bridge.html"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @JavascriptInterface
    fun getStorageItem(key: String?): String? {
        if (key.isNullOrBlank()) return null
        return prefs.getString(key, null)
    }

    @JavascriptInterface
    fun setStorageItem(key: String?, value: String?) {
        if (key.isNullOrBlank()) return
        val safeVal = value ?: ""
        prefs.edit().putString(key, safeVal).apply()

        // Saat state utama dasborSolidV2 diperbarui, ekstrak metrik ke Widget 4x2 secara instan
        if (key == "dasborSolidV2") {
            NativeStorageHelper.syncStateJsonToWidgetPrefs(context, safeVal)
            BalanceCardWidgetProvider.updateAllWidgets(context)
        }
    }

    @JavascriptInterface
    fun removeStorageItem(key: String?) {
        if (key.isNullOrBlank()) return
        prefs.edit().remove(key).apply()
        if (key == "dasborSolidV2") {
            BalanceCardWidgetProvider.updateAllWidgets(context)
        }
    }

    @JavascriptInterface
    fun syncWidgetData(saldoFormatted: String?, pemasukanFormatted: String?, pengeluaranFormatted: String?, activeTasksCount: Int) {
        prefs.edit()
            .putString("widget_saldo_text", saldoFormatted ?: "Rp 0")
            .putString("widget_masuk_text", pemasukanFormatted ?: "Rp 0")
            .putString("widget_keluar_text", pengeluaranFormatted ?: "Rp 0")
            .putInt("widget_tugas_count", activeTasksCount)
            .putLong("widget_last_updated", System.currentTimeMillis())
            .apply()

        BalanceCardWidgetProvider.updateAllWidgets(context)
    }

    @JavascriptInterface
    fun checkOtaUpdate() {
        OtaUpdateManager.checkForUpdates(context, webView, false)
    }

    @JavascriptInterface
    fun launchGoogleAuth() {
        val uri = Uri.parse(AUTH_BRIDGE_URL)
        try {
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
            customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            customTabsIntent.launchUrl(context, uri)
        } catch (e: Exception) {
            val fallbackIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallbackIntent)
        }
    }

    @JavascriptInterface
    fun getNativeRuntimeInfo(): String {
        return JSONObject().apply {
            put("platform", "Android Native Hybrid (Kotlin & Java)")
            put("bridgeVersion", "1.3.0 (Stable)")
            put("sdkInt", Build.VERSION.SDK_INT)
            put("deviceModel", "${Build.MANUFACTURER} ${Build.MODEL}")
            put("widgetsSupported", true)
            put("otaSupported", true)
        }.toString()
    }
}
