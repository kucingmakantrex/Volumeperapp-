package com.appvol;

import java.io.*;
import java.lang.reflect.Method;
import java.util.*;

public class Main {
    static final String DIR = System.getProperty("av.dir", "/data/adb/appvolume");
    static final File CONF = new File(DIR, "volumes.conf");
    static final File ACTIVE = new File(DIR, "active.txt");

    static Object audio;
    static Method getConfigs, getUid, getPiid, getProxy;
    static final Map<Class<?>, Method> volMethods = new HashMap<>();
    static final Map<String, Integer> volumes = new HashMap<>();
    static final Map<Integer, String> appIdToPkg = new HashMap<>();
    static final Map<Integer, long[]> applied = new HashMap<>();
    static long confStamp = -1, uidStamp = 0;
    static String lastActive = "", lastLog = "";

    public static void main(String[] args) throws Exception {
        new File(DIR).mkdirs();
        write(new File(DIR, "daemon.pid"), String.valueOf(new File("/proc/self").getCanonicalFile().getName()));
        log("daemon mulai");
        while (true) {
            try {
                if (audio == null) connect();
                loadConf();
                refreshUids();
                tick();
            } catch (Throwable t) {
                Throwable c = t.getCause() != null ? t.getCause() : t;
                log("error: " + c);
                audio = null;
                Thread.sleep(2000);
            }
            Thread.sleep(1000);
        }
    }

    static void connect() throws Exception {
        Object binder = Class.forName("android.os.ServiceManager")
                .getMethod("getService", String.class).invoke(null, "audio");
        Class<?> iface = Class.forName("android.media.IAudioService");
        audio = Class.forName("android.media.IAudioService$Stub")
                .getMethod("asInterface", Class.forName("android.os.IBinder")).invoke(null, binder);
        getConfigs = iface.getMethod("getActivePlaybackConfigurations");
        Class<?> cfg = Class.forName("android.media.AudioPlaybackConfiguration");
        getUid = cfg.getMethod("getClientUid");
        getPiid = cfg.getMethod("getPlayerInterfaceId");
        getProxy = cfg.getMethod("getPlayerProxy");
        applied.clear();
        log("terhubung ke audio service");
    }

    static void tick() throws Exception {
        List<?> list = (List<?>) getConfigs.invoke(audio);
        Set<String> active = new TreeSet<>();
        Set<Integer> seen = new HashSet<>();
        long now = System.currentTimeMillis();
        for (Object c : list) {
            String pkg = appIdToPkg.get(((Integer) getUid.invoke(c)) % 100000);
            if (pkg == null) continue;
            active.add(pkg);
            int piid = (Integer) getPiid.invoke(c);
            seen.add(piid);
            Integer pct = volumes.get(pkg);
            long[] last = applied.get(piid);
            Object proxy = getProxy.invoke(c);
            if (proxy == null) continue;
            if (pct == null) {
                if (last != null) { trySet(proxy, 1f); applied.remove(piid); }
                continue;
            }
            if (last == null || last[0] != pct || now - last[1] > 10000) {
                trySet(proxy, pct / 100f);
                applied.put(piid, new long[]{pct, now});
            }
        }
        applied.keySet().retainAll(seen);
        String joined = String.join("\n", active);
        if (!joined.equals(lastActive)) { write(ACTIVE, joined); lastActive = joined; }
    }

    static void trySet(Object proxy, float v) {
        try {
            Class<?> k = proxy.getClass();
            Method m = volMethods.get(k);
            if (m == null) {
                try { m = k.getMethod("setVolume", float.class); }
                catch (NoSuchMethodException e) { m = k.getDeclaredMethod("setVolume", float.class); }
                m.setAccessible(true);
                volMethods.put(k, m);
                log("setVolume lewat " + k.getName());
            }
            m.invoke(proxy, v);
        } catch (Throwable t) {
            Throwable c = t.getCause() != null ? t.getCause() : t;
            log("setVolume gagal (" + proxy.getClass().getName() + "): " + c);
        }
    }

    static void loadConf() throws IOException {
        long stamp = CONF.lastModified() * 31 + CONF.length();
        if (stamp == confStamp) return;
        confStamp = stamp;
        volumes.clear();
        if (CONF.exists()) {
            try (BufferedReader r = new BufferedReader(new FileReader(CONF))) {
                String l;
                while ((l = r.readLine()) != null) {
                    int i = l.indexOf('=');
                    if (i < 1) continue;
                    try {
                        int v = Integer.parseInt(l.substring(i + 1).trim());
                        if (v >= 0 && v <= 100) volumes.put(l.substring(0, i).trim(), v);
                    } catch (NumberFormatException ignored) { }
                }
            }
        }
        uidStamp = 0;
        applied.clear();
        log("konfigurasi dimuat: " + volumes.size() + " aplikasi");
    }

    static void refreshUids() throws Exception {
        long now = System.currentTimeMillis();
        if (now - uidStamp < 20000) return;
        uidStamp = now;
        Process p = new ProcessBuilder("/system/bin/cmd", "package", "list", "packages", "-U")
                .redirectErrorStream(true).start();
        Map<Integer, String> m = new HashMap<>();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String l;
            while ((l = r.readLine()) != null) {
                int u = l.indexOf(" uid:");
                if (!l.startsWith("package:") || u < 0) continue;
                try {
                    int uid = Integer.parseInt(l.substring(u + 5).trim().split("[ ,]")[0]);
                    m.put(uid % 100000, l.substring(8, u));
                } catch (NumberFormatException ignored) { }
            }
        }
        p.waitFor();
        if (!m.isEmpty()) { appIdToPkg.clear(); appIdToPkg.putAll(m); }
    }

    static void write(File f, String s) throws IOException {
        File t = new File(f.getPath() + ".tmp");
        try (FileWriter w = new FileWriter(t)) { w.write(s); }
        t.renameTo(f);
    }

    static void log(String s) {
        if (s.equals(lastLog)) return;
        lastLog = s;
        try (FileWriter w = new FileWriter(new File(DIR, "daemon.log"), true)) {
            w.write(new Date() + " " + s + "\n");
        } catch (IOException ignored) { }
    }
            }
