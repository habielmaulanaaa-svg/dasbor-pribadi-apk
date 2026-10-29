<p align="center">
  <img src="icon-512.png" width="128" height="128" alt="Logo Dasbor Pribadi" style="border-radius: 28px; box-shadow: 0 8px 24px rgba(0,0,0,0.18);" />
</p>

# 📱 Dasbor Pribadi Mobile — Native Android (Kotlin & Java) + Web PWA v1.3.0 (Stable)

[![Android Native](https://img.shields.io/badge/Android%20Native-Kotlin%20%26%20Java-emerald?style=for-the-badge&logo=android&logoColor=white)](./android)
[![Web PWA](https://img.shields.io/badge/Web%20PWA-100%25%20Offline--First-blue?style=for-the-badge&logo=googlechrome&logoColor=white)](https://habielmaulanaaa-svg.github.io/dasbor-pribadi/)
[![Cloud Database](https://img.shields.io/badge/Cloud%20Sync-Firebase%20Firestore-orange?style=for-the-badge&logo=firebase&logoColor=white)](https://firebase.google.com/)

Aplikasi manajemen keuangan, tabungan, impian, dan produktivitas pribadi all-in-one yang berjalan sebagai **Aplikasi Native Android Berbasis Web (Kotlin & Java Hybrid WebView)** sekaligus **Progressive Web App (PWA) 100% Offline-First**. Menggabungkan lapisan native Android (`MainActivity.kt`, `DasborBridge.kt`, `BalanceCardWidgetProvider.kt`, `QuickActionWidgetProvider.java`, `OtaUpdateManager.java`) dengan antarmuka **CSS Grid Fluid Physics (0fr ➔ 1fr)** 120 FPS.

---

## 📂 Struktur Arsitektur Proyek (Native Kotlin/Java & Web)

```text
dasbor-mobile/
├── android/                               # Proyek Native Android Studio (Kotlin & Java)
│   ├── settings.gradle.kts                # Konfigurasi repositori & modul Gradle
│   ├── build.gradle.kts                   # Konfigurasi plugin root Android & Kotlin
│   └── app/
│       ├── build.gradle.kts               # Konfigurasi modul aplikasi (SDK 35, Kotlin + Java 17)
│       └── src/main/
│           ├── AndroidManifest.xml        # Manifest Activity, Deep-Link OAuth & Receiver Widget
│           ├── assets/                    # Bundel Web Offline-First (100% identik dengan root)
│           │   ├── index.html
│           │   ├── native-bridge-logic.js
│           │   └── tailwind.js
│           ├── java/id/dasborpribadi/mobile/
│           │   ├── MainActivity.kt                # (Kotlin) Host WebView 120FPS, Deep-Link & Izin Mikrofon
│           │   ├── DasborBridge.kt                # (Kotlin) @JavascriptInterface & SharedPreferences
│           │   ├── BalanceCardWidgetProvider.kt   # (Kotlin) Widget Layar Utama Kartu Saldo (4x2)
│           │   ├── QuickActionWidgetProvider.java # (Java) Widget Layar Utama Bar Aksi Cepat (4x1)
│           │   ├── OtaUpdateManager.java          # (Java) Mesin Pembaruan Over-The-Air (OTA)
│           │   └── NativeStorageHelper.java       # (Java) Parser JSON State & Pemformat Rupiah
│           └── res/                       # Tata letak XML RemoteViews Widget 4x2 & 4x1
├── index.html                             # Core App: SPA super-app, multi-wallet, budget, subs, cloud
├── native-bridge-logic.js                 # Modul Jembatan Native & Kalkulator Metrik Widget 4x2/4x1
├── server.js                              # Server Express & API Inspektor Kode Native Kotlin/Java
├── sw.js                                  # Service Worker (Cache management v1.3.0 Stable)
└── README.md                              # Dokumentasi resmi proyek
```

---

<div align="center">
  <sub>Dikembangkan dengan ❤️ untuk kemudahan pencatatan finansial & produktivitas harian.</sub><br>
  <sub><b>Dasbor Pribadi Mobile v1.3.0 (Stable) • Native Android (Kotlin & Java) + PWA Offline-First</b></sub>
</div>
