#!/usr/bin/env bash
# Corre dentro de xvfb-run: una xterm por cada terminal/<nombre>.txt, en orden, y captura esa ventana.
# uso: capturas.sh <carpeta-capturas> <carpeta-temporal>
CAP=$1; TMP=$2
H=$(cd "$(dirname "$0")" && pwd)
cd "$(cd "$H/../.." && pwd)"
for nombre in docker-images actuator-health-local perfil-prod pruebas; do
    rm -f "$TMP/listo-$nombre"
    xterm -geometry 160x58+0+0 -fa 'DejaVu Sans Mono' -fs 9 -bg '#0d1117' -fg '#c9d1d9' -T "$nombre" \
        -e bash "$H/mostrar.sh" "$H/terminal/$nombre.txt" "$TMP/listo-$nombre" > /dev/null 2>&1 &
    pid=$!
    for i in $(seq 1 900); do [ -f "$TMP/listo-$nombre" ] && break; sleep 1; done
    sleep 2
    import -window "$(xwininfo -name "$nombre" | awk '/Window id:/{print $4}')" "$CAP/$nombre.png" && echo "captura $nombre.png"
    kill "$pid" 2>/dev/null; wait "$pid" 2>/dev/null
done
docker rm -f mi-app-dev mi-app-prod > /dev/null 2>&1
true
