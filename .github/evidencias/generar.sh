#!/usr/bin/env bash
# Mide cada commit, toma las capturas (xterm reales) y verifica por separado las respuestas de los
# contenedores dev y prod. Termina con error si cualquier verificacion no da lo esperado.
set -u
REPO=$PWD
H=$REPO/.github/evidencias
SALIDA=$REPO/evid
CAP=$REPO/capturas
TRABAJO=$(mktemp -d)
mkdir -p "$SALIDA/logs" "$CAP"

{ echo "commit: $(git rev-parse HEAD)"; java -version; docker version --format '{{.Server.Version}}'; } > "$SALIDA/entorno.txt" 2>&1

# 1. Cada commit con proyecto compila y pasa sus pruebas por si solo
for c in $(git rev-list --reverse HEAD); do
    git cat-file -e "$c:pom.xml" 2>/dev/null || continue
    rm -rf "$TRABAJO/c"; mkdir -p "$TRABAJO/c"; git archive "$c" | tar -x -C "$TRABAJO/c"
    (cd "$TRABAJO/c" && ./mvnw -B -ntp verify) > "$SALIDA/logs/commit-${c:0:7}.log" 2>&1
    rc=$?
    echo "${c:0:7} $(git log -1 --format=%s "$c") -> exit $rc" >> "$SALIDA/compilacion-por-commit.txt"
done

# 2. Capturas en terminales reales (la imagen se construye dentro de la primera)
xvfb-run -a -s "-screen 0 1366x900x24" bash "$H/capturas.sh" "$CAP" "$TRABAJO" > "$SALIDA/logs/capturas.log" 2>&1

# 3. Verificacion sin pantalla de los dos perfiles (respuestas en JSON)
bash "$H/verificar.sh" "$SALIDA" > "$SALIDA/logs/verificar.log" 2>&1
git -C "$REPO" status --short > "$SALIDA/logs/arbol-despues.txt"
cp "$CAP"/*.png "$SALIDA/" 2>/dev/null

# 4. Revisar todo; sin esto las capturas no se agregan
python3 "$H/revisar.py" "$SALIDA" "$CAP" 2>&1 | tee "$SALIDA/logs/revisar.log"
exit "${PIPESTATUS[0]}"
