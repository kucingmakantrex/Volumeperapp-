#!/system/bin/sh
ui_print "- Volume Per Aplikasi"
[ "$API" -ge 33 ] || abort "! Butuh Android 13 atau lebih baru"
[ -f "$MODPATH/appvol.dex" ] || abort "! appvol.dex tidak ada"
mkdir -p /data/adb/appvolume
[ -f /data/adb/appvolume/daemon.pid ] && kill "$(cat /data/adb/appvolume/daemon.pid)" 2>/dev/null
set_perm "$MODPATH/appvol.dex" 0 0 0444
set_perm "$MODPATH/service.sh" 0 0 0755
set_perm "$MODPATH/avctl.sh" 0 0 0755
ui_print "- Reboot, lalu buka WebUI dari daftar modul"
