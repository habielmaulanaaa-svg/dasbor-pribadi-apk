package id.dasborpribadi.mobile

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File

/**
 * MainActivity (Kotlin)
 * Aplikasi Native Android Berbasis Web (Hybrid WebView + Native Bridge)
 * Menyediakan wadah akselerasi perangkat keras 120 FPS, penyimpanan persisten ganda
 * (localStorage + SharedPreferences), dukungan izin mikrofon (FAB Voice Input),
 * pemilih berkas JSON (Backup/Restore), deep-link Google OAuth (dasbor://auth-callback),
 * serta pemicu aksi langsung dari Widget Layar Utama Android (4x2 & 4x1).
 */
class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var dasborBridge: DasborBridge
    private var fileUploadCallback: ValueCallback<Array<Uri>>? = null
    private var pendingWebPermissionRequest: PermissionRequest? = null
    private var isPageLoaded: Boolean = false
    private var pendingWidgetAction: String? = null
    private var pendingAuthCallbackUri: Uri? = null

    companion object {
        const val EXTRA_WIDGET_ACTION = "extra_widget_action"
        private const val REQUEST_CODE_FILE_CHOOSER = 1001
        private const val REQUEST_CODE_RECORD_AUDIO = 1002
        private const val ASSET_INDEX_URL = "file:///android_asset/index.html"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.parseColor("#0e1621")
        window.navigationBarColor = Color.parseColor("#0e1621")

        webView = WebView(this).apply {
            setBackgroundColor(Color.parseColor("#0e1621"))
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
        }
        setContentView(webView)

        configureWebViewSettings(webView.settings)

        dasborBridge = DasborBridge(this, webView)
        webView.addJavascriptInterface(dasborBridge, "DasborBridge")

        setupWebViewClients()
        setupBackNavigation()

        // Tangani intent awal (Deep-link OAuth atau ketukan dari Widget 4x1/4x2)
        captureIncomingIntent(intent)

        // Muat bundel OTA terbaru jika tersedia, atau fallback ke assets/index.html
        val otaCachedFile = File(filesDir, "ota_bundle/index.html")
        if (otaCachedFile.exists() && otaCachedFile.canRead()) {
            webView.loadUrl("file://${otaCachedFile.absolutePath}")
        } else {
            webView.loadUrl(ASSET_INDEX_URL)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebViewSettings(settings: WebSettings) {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        @Suppress("DEPRECATION")
        settings.allowFileAccessFromFileURLs = true
        @Suppress("DEPRECATION")
        settings.allowUniversalAccessFromFileURLs = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        settings.textZoom = 100
        settings.userAgentString = "${settings.userAgentString} DasborPribadiNative/1.3.0 (Android; Kotlin)"
    }

    private fun setupWebViewClients() {
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                isPageLoaded = true
                dispatchPendingActions()
                // Periksa pembaruan OTA secara hening di latar belakang setelah halaman siap
                OtaUpdateManager.checkForUpdates(this@MainActivity, webView, true)
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val targetUri = request?.url ?: return false
                val scheme = targetUri.scheme?.lowercase() ?: ""

                if (scheme == "dasbor" && targetUri.host == "auth-callback") {
                    handleAuthCallbackUri(targetUri)
                    return true
                }

                if (scheme == "mailto" || (scheme.startsWith("http") && !targetUri.toString().contains("android_asset"))) {
                    return try {
                        startActivity(Intent(Intent.ACTION_VIEW, targetUri))
                        true
                    } catch (e: Exception) {
                        false
                    }
                }
                return false
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest?) {
                if (request == null) return
                val resources = request.resources
                if (resources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) {
                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        request.grant(resources)
                    } else {
                        pendingWebPermissionRequest = request
                        ActivityCompat.requestPermissions(
                            this@MainActivity,
                            arrayOf(Manifest.permission.RECORD_AUDIO),
                            REQUEST_CODE_RECORD_AUDIO
                        )
                    }
                } else {
                    request.grant(resources)
                }
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                fileUploadCallback?.onReceiveValue(null)
                fileUploadCallback = filePathCallback

                val chooserIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/json"
                }
                return try {
                    @Suppress("DEPRECATION")
                    startActivityForResult(Intent.createChooser(chooserIntent, "Pilih Berkas Cadangan JSON"), REQUEST_CODE_FILE_CHOOSER)
                    true
                } catch (e: Exception) {
                    fileUploadCallback = null
                    false
                }
            }
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                webView.evaluateJavascript(
                    "(function() { " +
                        "var openSheet = document.querySelector('.bottom-sheet-backdrop.open');" +
                        "if (openSheet) { closeBottomSheet(openSheet.id); return 'closed_sheet'; }" +
                        "var sidebar = document.getElementById('sidebar');" +
                        "if (sidebar && !sidebar.classList.contains('-translate-x-full')) { toggleSidebar(); return 'closed_sidebar'; }" +
                        "return 'exit';" +
                    "})()"
                ) { result ->
                    if (result != null && result.contains("exit")) {
                        if (webView.canGoBack()) {
                            webView.goBack()
                        } else {
                            isEnabled = false
                            onBackPressedDispatcher.onBackPressed()
                        }
                    }
                }
            }
        })
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        captureIncomingIntent(intent)
        if (isPageLoaded) {
            dispatchPendingActions()
        }
    }

    private fun captureIncomingIntent(intent: Intent?) {
        if (intent == null) return
        val data = intent.data
        if (data != null && data.scheme == "dasbor" && data.host == "auth-callback") {
            pendingAuthCallbackUri = data
        }
        val widgetAction = intent.getStringExtra(EXTRA_WIDGET_ACTION) ?: data?.getQueryParameter("action")
        if (!widgetAction.isNullOrBlank()) {
            pendingWidgetAction = widgetAction
        }
    }

    private fun dispatchPendingActions() {
        pendingAuthCallbackUri?.let { uri ->
            pendingAuthCallbackUri = null
            handleAuthCallbackUri(uri)
        }
        pendingWidgetAction?.let { action ->
            pendingWidgetAction = null
            val safeAction = action.replace("'", "")
            webView.post {
                webView.evaluateJavascript("if (typeof handleNativeAction === 'function') handleNativeAction('$safeAction');", null)
            }
        }
    }

    private fun handleAuthCallbackUri(uri: Uri) {
        val idToken = NativeStorageHelper.escapeJsString(uri.getQueryParameter("idToken") ?: "")
        val name = NativeStorageHelper.escapeJsString(uri.getQueryParameter("name") ?: "Pengguna")
        val email = NativeStorageHelper.escapeJsString(uri.getQueryParameter("email") ?: "")
        val photo = NativeStorageHelper.escapeJsString(uri.getQueryParameter("photo") ?: "")
        val uid = NativeStorageHelper.escapeJsString(uri.getQueryParameter("uid") ?: "")

        webView.post {
            webView.evaluateJavascript(
                "if (typeof handleAuthBridgeSuccess === 'function') { " +
                    "handleAuthBridgeSuccess('$idToken', '$name', '$email', '$photo', '$uid'); " +
                "}",
                null
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_RECORD_AUDIO) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pendingWebPermissionRequest?.grant(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE))
            } else {
                pendingWebPermissionRequest?.deny()
            }
            pendingWebPermissionRequest = null
        }
    }

    @Deprecated("Deprecated in Android Activity")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_FILE_CHOOSER) {
            val results = if (resultCode == Activity.RESULT_OK && data?.data != null) {
                arrayOf(data.data!!)
            } else {
                null
            }
            fileUploadCallback?.onReceiveValue(results)
            fileUploadCallback = null
        }
    }

    override fun onResume() {
        super.onResume()
        BalanceCardWidgetProvider.updateAllWidgets(this)
    }
}
