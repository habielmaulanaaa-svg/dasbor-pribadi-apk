const express = require('express');
const path = require('path');
const fs = require('fs');

const app = express();
const PORT = process.env.PORT || 3000;

const NATIVE_FILES_MAP = {
  'MainActivity.kt': {
    relPath: 'android/app/src/main/java/id/dasborpribadi/mobile/MainActivity.kt',
    lang: 'Kotlin',
    role: 'Host WebView 120FPS, Deep-Link OAuth, Izin Suara & Pemilih File JSON'
  },
  'DasborBridge.kt': {
    relPath: 'android/app/src/main/java/id/dasborpribadi/mobile/DasborBridge.kt',
    lang: 'Kotlin',
    role: '@JavascriptInterface (SharedPreferences, Sinkronisasi Widget & Custom Tabs)'
  },
  'BalanceCardWidgetProvider.kt': {
    relPath: 'android/app/src/main/java/id/dasborpribadi/mobile/BalanceCardWidgetProvider.kt',
    lang: 'Kotlin',
    role: 'AppWidgetProvider Kartu Saldo 4x2 Layar Utama Android'
  },
  'QuickActionWidgetProvider.java': {
    relPath: 'android/app/src/main/java/id/dasborpribadi/mobile/QuickActionWidgetProvider.java',
    lang: 'Java',
    role: 'AppWidgetProvider Bar Aksi Cepat 4x1 (Pengeluaran, Pemasukan, Tugas, Impian)'
  },
  'OtaUpdateManager.java': {
    relPath: 'android/app/src/main/java/id/dasborpribadi/mobile/OtaUpdateManager.java',
    lang: 'Java',
    role: 'Mesin Pembaruan Over-The-Air (OTA) Latar Belakang & Verifikasi SemVer'
  },
  'NativeStorageHelper.java': {
    relPath: 'android/app/src/main/java/id/dasborpribadi/mobile/NativeStorageHelper.java',
    lang: 'Java',
    role: 'Parser JSON dasborSolidV2 & Pemformat Rupiah Native'
  },
  'AndroidManifest.xml': {
    relPath: 'android/app/src/main/AndroidManifest.xml',
    lang: 'XML',
    role: 'Konfigurasi Manifest, Deep-Link dasbor://auth-callback & Receiver Widget'
  },
  'app/build.gradle.kts': {
    relPath: 'android/app/build.gradle.kts',
    lang: 'Kotlin DSL',
    role: 'Konfigurasi Build Gradle Modul Aplikasi Android (Kotlin + Java 17)'
  },
  'widget_balance_card.xml': {
    relPath: 'android/app/src/main/res/layout/widget_balance_card.xml',
    lang: 'XML',
    role: 'Tata Letak Native RemoteViews Widget Kartu Saldo (4x2)'
  },
  'widget_quick_action.xml': {
    relPath: 'android/app/src/main/res/layout/widget_quick_action.xml',
    lang: 'XML',
    role: 'Tata Letak Native RemoteViews Widget Bar Aksi Cepat (4x1)'
  }
};

app.get('/api/native-info', (req, res) => {
  const files = Object.entries(NATIVE_FILES_MAP).map(([key, meta]) => {
    const fullPath = path.join(__dirname, meta.relPath);
    let size = 0;
    let lines = 0;
    if (fs.existsSync(fullPath)) {
      const content = fs.readFileSync(fullPath, 'utf-8');
      size = Buffer.byteLength(content, 'utf-8');
      lines = content.split('\n').length;
    }
    return {
      key,
      path: meta.relPath,
      lang: meta.lang,
      role: meta.role,
      size,
      lines
    };
  });

  const assetIndexExists = fs.existsSync(path.join(__dirname, 'android/app/src/main/assets/index.html'));
  res.json({
    appName: 'Dasbor Pribadi Mobile',
    packageName: 'id.dasborpribadi.mobile',
    version: '1.3.0 (Stable)',
    architecture: 'Hybrid Native Android (Kotlin + Java + WebView Assets)',
    assetSynced: assetIndexExists,
    files
  });
});

app.get('/api/native-source', (req, res) => {
  const fileKey = String(req.query.file || 'MainActivity.kt');
  const meta = NATIVE_FILES_MAP[fileKey];
  if (!meta) {
    return res.status(404).json({ error: 'Berkas sumber tidak ditemukan' });
  }
  const fullPath = path.join(__dirname, meta.relPath);
  if (!fs.existsSync(fullPath)) {
    return res.status(404).json({ error: 'Berkas belum tersedia di disk' });
  }
  const content = fs.readFileSync(fullPath, 'utf-8');
  res.json({
    key: fileKey,
    path: meta.relPath,
    lang: meta.lang,
    role: meta.role,
    content
  });
});

app.get('/api/download-android-bundle', (req, res) => {
  const bundleFiles = {};
  const extraPaths = [
    'android/settings.gradle.kts',
    'android/build.gradle.kts',
    'android/gradle.properties',
    'android/app/proguard-rules.pro',
    'android/app/src/main/res/xml/balance_card_widget_info.xml',
    'android/app/src/main/res/xml/quick_action_widget_info.xml',
    'android/app/src/main/res/values/strings.xml',
    'android/app/src/main/res/values/styles.xml'
  ];

  Object.values(NATIVE_FILES_MAP).forEach((meta) => {
    const fullPath = path.join(__dirname, meta.relPath);
    if (fs.existsSync(fullPath)) {
      bundleFiles[meta.relPath] = fs.readFileSync(fullPath, 'utf-8');
    }
  });

  extraPaths.forEach((relPath) => {
    const fullPath = path.join(__dirname, relPath);
    if (fs.existsSync(fullPath)) {
      bundleFiles[relPath] = fs.readFileSync(fullPath, 'utf-8');
    }
  });

  const payload = {
    project: 'Dasbor Pribadi Mobile - Native Android (Kotlin & Java)',
    packageId: 'id.dasborpribadi.mobile',
    version: '1.3.0 (Stable)',
    generatedAt: new Date().toISOString(),
    instructions: 'Buka folder android/ di Android Studio (JDK 17+) lalu jalankan ./gradlew assembleDebug untuk menghasilkan APK mandiri.',
    files: bundleFiles
  };

  res.setHeader('Content-Type', 'application/json; charset=utf-8');
  res.setHeader('Content-Disposition', 'attachment; filename="dasbor-pribadi-android-kotlin-java-v1.3.0.json"');
  res.send(JSON.stringify(payload, null, 2));
});

app.use(express.static(__dirname, {
  setHeaders: (res, filePath) => {
    if (filePath.endsWith('manifest.json')) {
      res.setHeader('Content-Type', 'application/manifest+json; charset=utf-8');
    }
    res.setHeader('Access-Control-Allow-Origin', '*');
  }
}));

app.get('*', (req, res) => {
  res.sendFile(path.join(__dirname, 'index.html'));
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(`Dasbor Pribadi server running on http://0.0.0.0:${PORT}`);
});

