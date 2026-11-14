package com.universalrp.appdisabler.lab;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import rikka.shizuku.Shizuku;

/** CI-only, emulator-only experiment; never accepts a package or command from an intent. */
public final class PersistenceLabActivity extends Activity {
    private static final String TAG = "DisablerPersistLab";
    private static final String PKG = "com.universalrp.appdisabler.fixture";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        final String mode = getIntent().getStringExtra("mode");
        final String nonce = getIntent().getStringExtra("nonce");
        new Thread(() -> {
            try {
                if (!(Build.HARDWARE.equals("ranchu") || Build.HARDWARE.equals("goldfish")))
                    throw new IllegalStateException("emulator required");
                if (!Shizuku.pingBinder() || Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED)
                    throw new IllegalStateException("Shizuku permission unavailable");
                String id = shell("id", "-u");
                if (!id.trim().equals("2000")) throw new IllegalStateException("shell UID required: " + id);
                if ("disable-sync".equals(mode) || "enable-sync".equals(mode)) {
                    probe(mode, PKG);
                } else if ("suspend-flush".equals(mode) || "unsuspend-flush".equals(mode)) {
                    boolean suspend = mode.equals("suspend-flush");
                    String result = shell("pm", suspend ? "suspend" : "unsuspend", "--user", "0", PKG);
                    if (!result.contains("Package " + PKG + " new suspended state: " + suspend))
                        throw new IllegalStateException("Unexpected package state: " + result);
                    probe("flush");
                } else {
                    throw new IllegalArgumentException("Unknown lab mode");
                }
                Log.i(TAG, "LAB_RESULT|" + nonce + "|OK|" + mode);
            } catch (Exception e) {
                Log.e(TAG, "LAB_RESULT|" + nonce + "|ERROR|" + e, e);
            } finally {
                runOnUiThread(this::finish);
            }
        }).start();
    }

    private void probe(String mode, String... extra) throws Exception {
        String[] args = new String[5 + extra.length];
        args[0] = "env";
        args[1] = "CLASSPATH=" + getApplicationInfo().sourceDir;
        args[2] = "app_process";
        args[3] = "/system/bin";
        args[4] = "com.universalrp.appdisabler.lab.PackageProbe";
        String[] full = new String[args.length + 1];
        System.arraycopy(args, 0, full, 0, 5);
        full[5] = mode;
        System.arraycopy(extra, 0, full, 6, extra.length);
        String out = shell(full);
        if (!out.contains("LAB_PROBE_COMPLETED " + mode)) throw new IllegalStateException("probe failed: " + out);
    }

    private String shell(String... args) throws Exception {
        Method method = Shizuku.class.getDeclaredMethod("newProcess", String[].class, String[].class, String.class);
        method.setAccessible(true);
        Process process = (Process) method.invoke(null, args, null, null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        copy(process.getInputStream(), out);
        copy(process.getErrorStream(), out);
        int exit = process.waitFor();
        String result = new String(out.toByteArray(), StandardCharsets.UTF_8);
        if (exit != 0) throw new IllegalStateException("shell exit " + exit + ": " + result);
        return result;
    }

    private static void copy(InputStream in, ByteArrayOutputStream out) throws Exception {
        byte[] buffer = new byte[4096];
        int n;
        while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
    }
}
