package id.dasborpribadi.mobile;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.webkit.WebView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OtaUpdateManager (Java)
 * Mesin Pembaruan Over-The-Air (OTA) Hibrida untuk APK Mandiri.
 * Mengunduh bundel antarmuka web terbaru dari repositori rilis resmi di latar belakang,
 * menyimpannya ke penyimpanan internal perangkat, dan memberi tahu WebView via Dynamic Island.
 */
public final class OtaUpdateManager {

    private static final String REMOTE_INDEX_URL = "https://habielmaulanaaa-svg.github.io/dasbor-pribadi/index.html";
    private static final String PREF_KEY_OTA_VERSION = "ota_bundle_version";
    private static final String CURRENT_BUNDLED_VERSION = "1.3.0";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private OtaUpdateManager() {}

    public static void checkForUpdates(final Context context, final WebView webView, final boolean silent) {
        if (!silent) {
            dispatchDynamicIsland(
                webView,
                "Pembaruan OTA Native",
                "Memeriksa bundel rilis terbaru...",
                "fas fa-sync-alt",
                "bg-primary-500/20 text-primary-400"
            );
        }

        EXECUTOR.execute(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(REMOTE_INDEX_URL + "?t=" + System.currentTimeMillis());
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(7000);
                conn.setReadTimeout(10000);

                if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    if (!silent) {
                        dispatchDynamicIsland(
                            webView,
                            "OTA Native",
                            "Menggunakan bundel offline v" + CURRENT_BUNDLED_VERSION + " (Stable)",
                            "fas fa-shield-alt",
                            "bg-emerald-500/20 text-emerald-400"
                        );
                    }
                    return;
                }

                StringBuilder htmlBuilder = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        htmlBuilder.append(line).append("\n");
                    }
                }

                String remoteHtml = htmlBuilder.toString();
                String remoteVersion = extractVersionFromHtml(remoteHtml);
                SharedPreferences prefs = context.getSharedPreferences(DasborBridge.PREFS_NAME, Context.MODE_PRIVATE);
                String installedVersion = prefs.getString(PREF_KEY_OTA_VERSION, CURRENT_BUNDLED_VERSION);

                if (remoteVersion != null && isNewerVersion(remoteVersion, installedVersion)) {
                    File otaDir = new File(context.getFilesDir(), "ota_bundle");
                    if (!otaDir.exists()) {
                        //noinspection ResultOfMethodCallIgnored
                        otaDir.mkdirs();
                    }
                    File targetFile = new File(otaDir, "index.html");
                    try (FileOutputStream fos = new FileOutputStream(targetFile, false)) {
                        fos.write(remoteHtml.getBytes(StandardCharsets.UTF_8));
                    }

                    prefs.edit().putString(PREF_KEY_OTA_VERSION, remoteVersion).apply();

                    MAIN_HANDLER.post(() -> {
                        String safeVer = NativeStorageHelper.escapeJsString("v" + remoteVersion + " (Stable)");
                        webView.evaluateJavascript(
                            "(function() {" +
                                "var badge = document.getElementById('ota-toast-version-badge');" +
                                "if (badge) badge.innerText = '" + safeVer + "';" +
                                "var toast = document.getElementById('ota-update-toast');" +
                                "if (toast) {" +
                                    "toast.classList.remove('scale-0', 'opacity-0', '-translate-y-6');" +
                                    "toast.classList.add('scale-100', 'opacity-100', 'translate-y-0');" +
                                "}" +
                            "})()",
                            null
                        );
                    });
                } else if (!silent) {
                    dispatchDynamicIsland(
                        webView,
                        "Versi Terbaru Aktif",
                        "Dasbor Native v" + installedVersion + " (Stable) sudah mutakhir ✨",
                        "fas fa-check-circle",
                        "bg-emerald-500/20 text-emerald-400"
                    );
                }
            } catch (Exception e) {
                if (!silent) {
                    dispatchDynamicIsland(
                        webView,
                        "Mode Offline Aktif",
                        "Bundel lokal v" + CURRENT_BUNDLED_VERSION + " siap digunakan tanpa internet",
                        "fas fa-wifi",
                        "bg-amber-500/20 text-amber-400"
                    );
                }
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        });
    }

    private static String extractVersionFromHtml(String html) {
        if (html == null) return null;
        Pattern pattern = Pattern.compile("Dasbor Pribadi Premium v(\\d+\\.\\d+\\.\\d+)");
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    public static boolean isNewerVersion(String candidate, String baseline) {
        if (candidate == null || baseline == null) return false;
        String[] cParts = candidate.split("\\.");
        String[] bParts = baseline.split("\\.");
        int len = Math.max(cParts.length, bParts.length);
        for (int i = 0; i < len; i++) {
            int cVal = i < cParts.length ? parsePart(cParts[i]) : 0;
            int bVal = i < bParts.length ? parsePart(bParts[i]) : 0;
            if (cVal > bVal) return true;
            if (cVal < bVal) return false;
        }
        return false;
    }

    private static int parsePart(String part) {
        try {
            return Integer.parseInt(part.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static void dispatchDynamicIsland(
            final WebView webView,
            final String title,
            final String message,
            final String iconClass,
            final String colorClass) {
        MAIN_HANDLER.post(() -> {
            String t = NativeStorageHelper.escapeJsString(title);
            String m = NativeStorageHelper.escapeJsString(message);
            String ic = NativeStorageHelper.escapeJsString(iconClass);
            String cc = NativeStorageHelper.escapeJsString(colorClass);
            webView.evaluateJavascript(
                "if (typeof triggerDynamicIsland === 'function') { " +
                    "triggerDynamicIsland('" + t + "', '" + m + "', '" + ic + "', '" + cc + "', true); " +
                "}",
                null
            );
        });
    }
}
