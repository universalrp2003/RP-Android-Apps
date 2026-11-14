package com.universalrp.persistencelab;

/** Lab-only shell app_process probe. Not shipped in the Android app. */
public final class Probe {
    public static void main(String[] args) throws Exception {
        Class<?> serviceManager = Class.forName("android.os.ServiceManager");
        Object binder = serviceManager.getDeclaredMethod("getService", String.class).invoke(null, "package");
        Class<?> stub = Class.forName("android.content.pm.IPackageManager$Stub");
        Class<?> binderType = Class.forName("android.os.IBinder");
        Object manager = stub.getMethod("asInterface", binderType).invoke(null, binder);
        Class<?> ipm = Class.forName("android.content.pm.IPackageManager");
        if (args.length == 2 && (args[0].equals("disable-sync") || args[0].equals("enable-sync"))) {
            int state = args[0].equals("disable-sync") ? 3 : 1;
            ipm.getMethod("setApplicationEnabledSetting", String.class, int.class, int.class,
                    int.class, String.class).invoke(manager, args[1], state, 2, 0, "shell:2000");
        } else if (args.length == 1 && args[0].equals("flush")) {
            ipm.getMethod("flushPackageRestrictionsAsUser", int.class).invoke(manager, 0);
        } else {
            throw new IllegalArgumentException("unknown lab action");
        }
        System.out.println("LAB_PROBE_COMPLETED " + args[0]);
    }
}
