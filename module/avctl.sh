#!/system/bin/sh
DATA=${AV_DATA:-/data/adb/appvolume}
CONF=$DATA/volumes.conf
mkdir -p "$DATA"; [ -f "$CONF" ] || : > "$CONF"

valid_pkg() { case "$1" in ""|*[!A-Za-z0-9_.]*) return 1;; esac; }
drop() { esc=$(printf %s "$1" | sed 's/\./\\./g'); sed "/^$esc=/d" "$CONF" > "$CONF.tmp"; }

case "$1" in
  set)
    valid_pkg "$2" || exit 1
    case "$3" in ""|*[!0-9]*) exit 1;; esac
    [ "$3" -le 100 ] || exit 1
    drop "$2"; echo "$2=$3" >> "$CONF.tmp"; mv "$CONF.tmp" "$CONF" ;;
  remove)
    valid_pkg "$2" || exit 1
    drop "$2"; mv "$CONF.tmp" "$CONF" ;;
  get)    cat "$CONF" ;;
  active) cat "$DATA/active.txt" 2>/dev/null ;;
  status)
    pid=$(cat "$DATA/daemon.pid" 2>/dev/null)
    if [ -n "$pid" ] && [ -d "/proc/$pid" ]; then echo running; else echo stopped; fi ;;
  apps)
    if [ "$2" = all ]; then pm list packages; else pm list packages -3; fi | sed 's/^package://' | sort ;;
  restart) kill "$(cat "$DATA/daemon.pid" 2>/dev/null)" 2>/dev/null; echo ok ;;
  log)    tail -n 20 "$DATA/daemon.log" 2>/dev/null ;;
  *) exit 2 ;;
esac
