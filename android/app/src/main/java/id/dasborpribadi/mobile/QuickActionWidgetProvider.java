package id.dasborpribadi.mobile;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/**
 * QuickActionWidgetProvider (Java)
 * Penyedia Widget Layar Utama Android ukuran 4x1 (Bar Aksi Cepat).
 * Menyediakan 4 pintasan instan: Catat Pengeluaran, Catat Pemasukan,
 * Tugas/Catatan Kilat, dan Target Impian yang langsung membuka MainActivity.
 */
public class QuickActionWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_quick_action);

            views.setOnClickPendingIntent(
                R.id.widget_qa_btn_keluar,
                buildShortcutIntent(context, "pengeluaran", 101)
            );
            views.setOnClickPendingIntent(
                R.id.widget_qa_btn_masuk,
                buildShortcutIntent(context, "pemasukan", 102)
            );
            views.setOnClickPendingIntent(
                R.id.widget_qa_btn_tugas,
                buildShortcutIntent(context, "tugas", 103)
            );
            views.setOnClickPendingIntent(
                R.id.widget_qa_btn_impian,
                buildShortcutIntent(context, "impian", 104)
            );

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    private PendingIntent buildShortcutIntent(Context context, String actionName, int reqCode) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(MainActivity.EXTRA_WIDGET_ACTION, actionName);

        return PendingIntent.getActivity(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }
}
