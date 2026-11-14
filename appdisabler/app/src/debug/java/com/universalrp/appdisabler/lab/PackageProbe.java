package com.universalrp.appdisabler.lab;

/** Invoked by app_process as shell. Debug source set only, never included in the release APK. */
public final class PackageProbe {
    public static void main(String[] args) throws Exception {
        Class<?> sm = Class.forName("android.os.ServiceManager");
        Object binder = sm.getDeclaredMethod("getService", String.class).invoke(null, "package");
        Class<?> stub = Class.forName("android.content.pm.IPackageManager$Stub");
        Object manager = stub.getMethod("asInterface", Class.forName("android.os.IBinder"))
                .invoke(null, binder);
        Class<?> ipm = Class.forName("android.content.pm.IPackageManager");
        if (args.length == 2 && (args[0].equals("disable-sync") || args[0].equals("enable-sync"))
                && args[1].equals("com.universalrp.appdisabler.fixture")) {
            int state = args[0].equals("disable-sync") ? 3 : 1;
            ipm.getMethod("setApplicationEnabledSetting", String.class, int.class, int.class,
                    int.class, String.class).invoke(manager, args[1], state, 2, 0, "shell:2000");
        } else if (args.length == 1 && args[0].equals("flush")) {
            ipm.getMethod("flushPackageRestrictionsAsUser", int.class).invoke(manager, 0);
        } else {
            throw new IllegalArgumentException("Only the disposable fixture is permitted");
        }
        System.out.println("LAB_PROBE_COMPLETED " + args[0]);
    }
}
