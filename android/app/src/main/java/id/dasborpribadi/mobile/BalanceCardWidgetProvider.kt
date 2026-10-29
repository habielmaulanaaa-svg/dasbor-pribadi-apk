package id.dasborpribadi.mobile

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * BalanceCardWidgetProvider (Kotlin)
 * Penyedia Widget Layar Utama Android ukuran 4x2 (Kartu Saldo & Ringkasan Arus Kas).
 * Membaca data real-time dari SharedPreferences ("DasborNativePrefs") yang disinkronkan
 * otomatis oleh DasborBridge.kt dan NativeStorageHelper.java.
 */
class BalanceCardWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_REFRESH_WIDGET = "id.dasborpribadi.mobile.ACTION_REFRESH_WIDGET"

        @JvmStatic
        fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, BalanceCardWidgetProvider::class.java)
            val appWidgetIds = manager.getAppWidgetIds(componentName)
            if (appWidgetIds.isNotEmpty()) {
                val intent = Intent(context, BalanceCardWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(intent)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_WIDGET) {
            updateAllWidgets(context)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val prefs = context.getSharedPreferences(DasborBridge.PREFS_NAME, Context.MODE_PRIVATE)
        val rawJson = prefs.getString("dasborSolidV2", null)
        if (!rawJson.isNullOrBlank()) {
            NativeStorageHelper.syncStateJsonToWidgetPrefs(context, rawJson)
        }

        val saldoText = prefs.getString("widget_saldo_text", "Rp 0") ?: "Rp 0"
        val masukText = prefs.getString("widget_masuk_text", "Rp 0") ?: "Rp 0"
        val keluarText = prefs.getString("widget_keluar_text", "Rp 0") ?: "Rp 0"
        val tugasCount = prefs.getInt("widget_tugas_count", 0)
        val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_balance_card)
            views.setTextViewText(R.id.widget_tv_saldo, saldoText)
            views.setTextViewText(R.id.widget_tv_masuk, "+ $masukText")
            views.setTextViewText(R.id.widget_tv_keluar, "- $keluarText")
            views.setTextViewText(R.id.widget_tv_meta, "$tugasCount Tugas Aktif • $timeFmt")

            // Ketuk kartu utama -> Buka Beranda
            views.setOnClickPendingIntent(
                R.id.widget_balance_root,
                createActionPendingIntent(context, "beranda", 200)
            )
            // Ketuk tombol + Masuk -> Buka Tab Keuangan (Pemasukan)
            views.setOnClickPendingIntent(
                R.id.widget_btn_add_masuk,
                createActionPendingIntent(context, "pemasukan", 201)
            )
            // Ketuk tombol - Keluar -> Buka Tab Keuangan (Pengeluaran)
            views.setOnClickPendingIntent(
                R.id.widget_btn_add_keluar,
                createActionPendingIntent(context, "pengeluaran", 202)
            )

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    private fun createActionPendingIntent(context: Context, actionKey: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_WIDGET_ACTION, actionKey)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
