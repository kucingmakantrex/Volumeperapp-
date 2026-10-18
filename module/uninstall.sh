#!/system/bin/sh
[ -f /data/adb/appvolume/daemon.pid ] && kill "$(cat /data/adb/appvolume/daemon.pid)" 2>/dev/null
