const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const {
    formatNativeRupiah,
    computeNativeWidgetMetrics,
    isNewerSemanticVersion,
    createWebDasborBridge
} = require('./native-bridge-logic.js');

test('formatNativeRupiah formats positive, zero, and negative Rupiah values matching NativeStorageHelper.java', () => {
    assert.equal(formatNativeRupiah(0), 'Rp 0');
    assert.equal(formatNativeRupiah(1500000), 'Rp 1.500.000');
    assert.equal(formatNativeRupiah(-25000), '-Rp 25.000');
});

test('computeNativeWidgetMetrics calculates balance, income, expense, and active tasks accurately', () => {
    const sampleState = {
        saldo: 1475000,
        transaksi: [
            { id: 1, jenis: 'masuk', jumlah: 1500000, catatan: 'Gaji' },
            { id: 2, jenis: 'keluar', jumlah: 25000, catatan: 'Makan siang' }
        ],
        tugas: [
            { id: 10, nama: 'Belajar Kotlin', selesai: false },
            { id: 11, nama: 'Setup Gradle', selesai: true },
            { id: 12, nama: 'Uji Widget 4x2', selesai: false }
        ],
        impian: [
            { id: 20, nama: 'Laptop Baru', target: 10000000 }
        ]
    };

    const metrics = computeNativeWidgetMetrics(JSON.stringify(sampleState));
    assert.equal(metrics.saldo, 1475000);
    assert.equal(metrics.saldoFormatted, 'Rp 1.475.000');
    assert.equal(metrics.totalMasuk, 1500000);
    assert.equal(metrics.masukFormatted, 'Rp 1.500.000');
    assert.equal(metrics.totalKeluar, 25000);
    assert.equal(metrics.keluarFormatted, 'Rp 25.000');
    assert.equal(metrics.tugasAktif, 2);
    assert.equal(metrics.impianAktif, 1);
});

test('isNewerSemanticVersion compares MAJOR.MINOR.PATCH versions accurately', () => {
    assert.equal(isNewerSemanticVersion('1.3.0', '1.2.4'), true);
    assert.equal(isNewerSemanticVersion('1.2.4', '1.3.0'), false);
    assert.equal(isNewerSemanticVersion('1.3.0', '1.3.0'), false);
    assert.equal(isNewerSemanticVersion('2.0.0', '1.9.9'), true);
});

test('createWebDasborBridge persists items and auto-updates widget metrics when dasborSolidV2 is saved', () => {
    let lastWidgetMetrics = null;
    const bridge = createWebDasborBridge({
        onWidgetUpdate: (m) => {
            lastWidgetMetrics = m;
        }
    });

    const stateJson = JSON.stringify({
        saldo: 500000,
        transaksi: [{ id: 1, jenis: 'masuk', jumlah: 500000 }],
        tugas: [{ id: 1, nama: 'Tugas 1', selesai: false }]
    });

    bridge.setStorageItem('dasborSolidV2', stateJson);
    assert.equal(bridge.getStorageItem('dasborSolidV2'), stateJson);
    assert.equal(bridge.getStorageItem('widget_saldo_text'), 'Rp 500.000');
    assert.equal(bridge.getStorageItem('widget_masuk_text'), 'Rp 500.000');
    assert.equal(bridge.getStorageItem('widget_tugas_count'), '1');
    assert.ok(lastWidgetMetrics);
    assert.equal(lastWidgetMetrics.saldoFormatted, 'Rp 500.000');
});

test('100% synchronization between root index.html and android/app/src/main/assets/index.html', () => {
    const rootHtml = fs.readFileSync(path.join(__dirname, 'index.html'), 'utf-8');
    const assetHtml = fs.readFileSync(path.join(__dirname, 'android/app/src/main/assets/index.html'), 'utf-8');
    assert.equal(rootHtml, assetHtml, 'index.html and android/app/src/main/assets/index.html must be 100% identical');
});

test('All inline JavaScript blocks in index.html have valid syntax and all Kotlin/Java native files exist', () => {
    const rootHtml = fs.readFileSync(path.join(__dirname, 'index.html'), 'utf-8');
    const scriptRegex = /<script(?![^>]*\bsrc=)[^>]*>([\s\S]*?)<\/script>/gi;
    let match;
    let scriptCount = 0;
    while ((match = scriptRegex.exec(rootHtml)) !== null) {
        scriptCount++;
        assert.doesNotThrow(() => {
            new vm.Script(match[1]);
        }, `Inline script block #${scriptCount} must compile without syntax errors`);
    }
    assert.ok(scriptCount >= 2, 'Expected inline script blocks in index.html');

    const requiredNativeFiles = [
        'android/app/src/main/java/id/dasborpribadi/mobile/MainActivity.kt',
        'android/app/src/main/java/id/dasborpribadi/mobile/DasborBridge.kt',
        'android/app/src/main/java/id/dasborpribadi/mobile/BalanceCardWidgetProvider.kt',
        'android/app/src/main/java/id/dasborpribadi/mobile/QuickActionWidgetProvider.java',
        'android/app/src/main/java/id/dasborpribadi/mobile/OtaUpdateManager.java',
        'android/app/src/main/java/id/dasborpribadi/mobile/NativeStorageHelper.java',
        'android/app/src/main/AndroidManifest.xml',
        'android/app/build.gradle.kts'
    ];

    for (const rel of requiredNativeFiles) {
        const full = path.join(__dirname, rel);
        assert.equal(fs.existsSync(full), true, `Missing required native file: ${rel}`);
        assert.ok(fs.statSync(full).size > 100, `Native file ${rel} must not be empty`);
    }
});
