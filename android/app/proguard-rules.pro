# Pertahankan metode @JavascriptInterface pada DasborBridge (Kotlin)
-keepattributes JavascriptInterface
-keepclassmembers class id.dasborpribadi.mobile.DasborBridge {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class id.dasborpribadi.mobile.** { *; }
