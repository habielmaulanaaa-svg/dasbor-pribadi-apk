package id.dasborpribadi.mobile;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * NativeStorageHelper (Java)
 * Utilitas pemrosesan JSON state "dasborSolidV2" secara efisien di lapisan Java Native
 * untuk menyinkronkan Total Saldo, Pemasukan, Pengeluaran, dan Tugas Aktif
 * ke SharedPreferences Widget Layar Utama Android.
 */
public final class NativeStorageHelper {

    private NativeStorageHelper() {}

    public static void syncStateJsonToWidgetPrefs(Context context, String rawJson) {
        if (context == null || rawJson == null || rawJson.trim().isEmpty()) {
            return;
        }
        try {
            JSONObject state = new JSONObject(rawJson);
            long saldo = state.optLong("saldo", 0L);

            long totalMasuk = 0L;
            long totalKeluar = 0L;
            JSONArray transaksiArr = state.optJSONArray("transaksi");
            if (transaksiArr != null) {
                for (int i = 0; i < transaksiArr.length(); i++) {
                    JSONObject trx = transaksiArr.optJSONObject(i);
                    if (trx == null) continue;
                    String jenis = trx.optString("jenis", "");
                    long jumlah = trx.optLong("jumlah", 0L);
                    if ("masuk".equalsIgnoreCase(jenis)) {
                        totalMasuk += jumlah;
                    } else if ("keluar".equalsIgnoreCase(jenis)) {
                        totalKeluar += jumlah;
                    }
                }
            }

            int tugasAktif = 0;
            JSONArray tugasArr = state.optJSONArray("tugas");
            if (tugasArr != null) {
                for (int i = 0; i < tugasArr.length(); i++) {
                    JSONObject t = tugasArr.optJSONObject(i);
                    if (t != null && !t.optBoolean("selesai", false)) {
                        tugasAktif++;
                    }
                }
            }

            SharedPreferences prefs = context.getSharedPreferences(DasborBridge.PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit()
                .putString("widget_saldo_text", formatRupiah(saldo))
                .putString("widget_masuk_text", formatRupiah(totalMasuk))
                .putString("widget_keluar_text", formatRupiah(totalKeluar))
                .putInt("widget_tugas_count", tugasAktif)
                .putLong("widget_last_updated", System.currentTimeMillis())
                .apply();
        } catch (Exception ignored) {
            // Abaikan bila JSON belum lengkap
        }
    }

    public static String formatRupiah(long amount) {
        NumberFormat nf = NumberFormat.getInstance(new Locale("id", "ID"));
        if (amount < 0) {
            return "-Rp " + nf.format(Math.abs(amount));
        }
        return "Rp " + nf.format(amount);
    }

    public static String escapeJsString(String input) {
        if (input == null) return "";
        return input
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "");
    }
}
