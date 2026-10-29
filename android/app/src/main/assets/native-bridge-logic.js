(function (root, factory) {
    if (typeof module === 'object' && module.exports) {
        module.exports = factory();
    } else {
        root.NativeBridgeLogic = factory();
    }
})(typeof self !== 'undefined' ? self : this, function () {
    'use strict';

    function formatNativeRupiah(amount) {
        var num = Number(amount);
        if (!Number.isFinite(num)) num = 0;
        var rounded = Math.round(num);
        var absStr = Math.abs(rounded).toString().replace(/\B(?=(\d{3})+(?!\d))/g, '.');
        return rounded < 0 ? '-Rp ' + absStr : 'Rp ' + absStr;
    }

    function computeNativeWidgetMetrics(stateOrJson) {
        var state = null;
        if (typeof stateOrJson === 'string') {
            try {
                state = JSON.parse(stateOrJson);
            } catch (e) {
                state = {};
            }
        } else if (stateOrJson && typeof stateOrJson === 'object') {
            state = stateOrJson;
        } else {
            state = {};
        }

        var saldo = Number(state.saldo) || 0;
        var totalMasuk = 0;
        var totalKeluar = 0;

        if (Array.isArray(state.transaksi)) {
            for (var i = 0; i < state.transaksi.length; i++) {
                var trx = state.transaksi[i];
                if (!trx) continue;
                var jml = Number(trx.jumlah) || 0;
                var jenis = String(trx.jenis || '').toLowerCase();
                if (jenis === 'masuk') {
                    totalMasuk += jml;
                } else if (jenis === 'keluar') {
                    totalKeluar += jml;
                }
            }
        }

        var tugasAktif = 0;
        if (Array.isArray(state.tugas)) {
            for (var j = 0; j < state.tugas.length; j++) {
                var t = state.tugas[j];
                if (t && !t.selesai) {
                    tugasAktif++;
                }
            }
        }

        var impianAktif = Array.isArray(state.impian) ? state.impian.length : 0;

        return {
            saldo: saldo,
            saldoFormatted: formatNativeRupiah(saldo),
            totalMasuk: totalMasuk,
            masukFormatted: formatNativeRupiah(totalMasuk),
            totalKeluar: totalKeluar,
            keluarFormatted: formatNativeRupiah(totalKeluar),
            tugasAktif: tugasAktif,
            impianAktif: impianAktif
        };
    }

    function isNewerSemanticVersion(candidate, baseline) {
        if (!candidate || !baseline) return false;
        var cParts = String(candidate).split('.');
        var bParts = String(baseline).split('.');
        var len = Math.max(cParts.length, bParts.length);
        for (var i = 0; i < len; i++) {
            var cVal = i < cParts.length ? parseInt(String(cParts[i]).replace(/[^0-9]/g, ''), 10) || 0 : 0;
            var bVal = i < bParts.length ? parseInt(String(bParts[i]).replace(/[^0-9]/g, ''), 10) || 0 : 0;
            if (cVal > bVal) return true;
            if (cVal < bVal) return false;
        }
        return false;
    }

    function createWebDasborBridge(options) {
        var opts = options || {};
        var memPrefs = {};
        var storage = opts.storage || null;

        function readPref(k) {
            if (Object.prototype.hasOwnProperty.call(memPrefs, k)) {
                return memPrefs[k];
            }
            if (storage && typeof storage.getItem === 'function') {
                try {
                    return storage.getItem('__native_pref_' + k);
                } catch (e) {}
            }
            return null;
        }

        function writePref(k, v) {
            var strVal = v === null || v === undefined ? '' : String(v);
            memPrefs[k] = strVal;
            if (storage && typeof storage.setItem === 'function') {
                try {
                    storage.setItem('__native_pref_' + k, strVal);
                } catch (e) {}
            }
        }

        function deletePref(k) {
            delete memPrefs[k];
            if (storage && typeof storage.removeItem === 'function') {
                try {
                    storage.removeItem('__native_pref_' + k);
                } catch (e) {}
            }
        }

        return {
            isHybridBridgeActive: true,
            isNativeApkWebView: Boolean(opts.isNativeApkWebView),
            getStorageItem: function (key) {
                if (!key) return null;
                return readPref(key);
            },
            setStorageItem: function (key, value) {
                if (!key) return;
                writePref(key, value);
                if (key === 'dasborSolidV2') {
                    var metrics = computeNativeWidgetMetrics(value);
                    writePref('widget_saldo_text', metrics.saldoFormatted);
                    writePref('widget_masuk_text', metrics.masukFormatted);
                    writePref('widget_keluar_text', metrics.keluarFormatted);
                    writePref('widget_tugas_count', String(metrics.tugasAktif));
                    writePref('widget_last_updated', String(Date.now()));
                    if (typeof opts.onWidgetUpdate === 'function') {
                        opts.onWidgetUpdate(metrics);
                    }
                }
            },
            removeStorageItem: function (key) {
                if (!key) return;
                deletePref(key);
            },
            syncWidgetData: function (saldoFormatted, pemasukanFormatted, pengeluaranFormatted, activeTasksCount) {
                writePref('widget_saldo_text', saldoFormatted || 'Rp 0');
                writePref('widget_masuk_text', pemasukanFormatted || 'Rp 0');
                writePref('widget_keluar_text', pengeluaranFormatted || 'Rp 0');
                writePref('widget_tugas_count', String(Number(activeTasksCount) || 0));
                writePref('widget_last_updated', String(Date.now()));
                if (typeof opts.onWidgetUpdate === 'function') {
                    opts.onWidgetUpdate({
                        saldoFormatted: saldoFormatted || 'Rp 0',
                        masukFormatted: pemasukanFormatted || 'Rp 0',
                        keluarFormatted: pengeluaranFormatted || 'Rp 0',
                        tugasAktif: Number(activeTasksCount) || 0
                    });
                }
            },
            checkOtaUpdate: function () {
                if (typeof opts.onCheckOta === 'function') {
                    opts.onCheckOta();
                }
            },
            getNativeRuntimeInfo: function () {
                return JSON.stringify({
                    platform: opts.isNativeApkWebView
                        ? 'Android Native Hybrid (Kotlin & Java)'
                        : 'Web Native Hybrid Runtime (Kotlin/Java Bridge Active)',
                    bridgeVersion: '1.3.0 (Stable)',
                    kotlinModules: ['MainActivity.kt', 'DasborBridge.kt', 'BalanceCardWidgetProvider.kt'],
                    javaModules: ['QuickActionWidgetProvider.java', 'OtaUpdateManager.java', 'NativeStorageHelper.java'],
                    widgetsSupported: true,
                    otaSupported: true
                });
            }
        };
    }

    return {
        formatNativeRupiah: formatNativeRupiah,
        computeNativeWidgetMetrics: computeNativeWidgetMetrics,
        isNewerSemanticVersion: isNewerSemanticVersion,
        createWebDasborBridge: createWebDasborBridge
    };
});
