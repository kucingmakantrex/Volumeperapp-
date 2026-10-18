#!/system/bin/sh
MODDIR=${0%/*}
DATA=/data/adb/appvolume
mkdir -p "$DATA"
[ -f "$DATA/volumes.conf" ] || : > "$DATA/volumes.conf"
(
  until [ "$(getprop sys.boot_completed)" = 1 ]; do sleep 3; done
  sleep 10
  [ -f "$MODDIR/appvol.dex" ] || { echo "appvol.dex tidak ada" >> "$DATA/daemon.log"; exit 0; }
  while true; do
    [ "$(wc -c < "$DATA/daemon.log" 2>/dev/null || echo 0)" -gt 204800 ] && : > "$DATA/daemon.log"
    CLASSPATH="$MODDIR/appvol.dex" /system/bin/app_process /system/bin \
      --nice-name=appvold com.appvol.Main >> "$DATA/daemon.log" 2>&1
    sleep 5
  done
) &
