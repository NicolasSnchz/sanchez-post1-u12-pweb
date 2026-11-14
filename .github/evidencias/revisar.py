"""Revisa que todas las mediciones den lo esperado. Termina con codigo 1 si algo no cumple, y en ese
caso el flujo no agrega las capturas. uso: python3 revisar.py <carpeta-evid> <carpeta-capturas>"""
import json
import os
import sys

evid, capturas = sys.argv[1:3]
problemas = []


def exigir(condicion, mensaje):
    if not condicion:
        problemas.append(mensaje)


def leer(ruta):
    try:
        with open(ruta, encoding="utf-8") as f:
            return f.read()
    except OSError:
        return ""


por_commit = leer(f"{evid}/compilacion-por-commit.txt")
exigir(por_commit.strip(), "no se midio la compilacion por commit")
for linea in por_commit.splitlines():
    exigir(linea.endswith("exit 0"), "commit que no compila o no pasa: " + linea)

try:
    r = json.loads(leer(f"{evid}/respuestas.json"))
except ValueError:
    r = {"dev": {}, "prod": {}}
    problemas.append("no se pudo leer respuestas.json")

exigir(0 < r.get("tamano", 0) < 300 * 1024 * 1024, f"imagen de {r.get('tamano')} bytes (>= 300 MB)")
exigir(r.get("usuario") == "spring", "la imagen no corre con el usuario spring")
exigir("-jar" in r.get("entrypoint", "") and "app.jar" in r.get("entrypoint", ""), "ENTRYPOINT sin java -jar")
exigir(r.get("sin_mvn") == "si", "la imagen final trae mvn o javac")

dev, prod = r.get("dev", {}), r.get("prod", {})
exigir(dev.get("log") == 'The following 1 profile is active: "dev"', f"dev: {dev.get('log')}")
exigir('"status":"UP"' in dev.get("health", "") and '"components"' in dev.get("health", ""), "dev: health sin UP o sin detalle")
exigir(dev.get("post") == "201", f"dev: POST {dev.get('post')}")
exigir(dev.get("beans") == "200", f"dev: /actuator/beans {dev.get('beans')} (deberia estar expuesto)")

exigir(prod.get("log") == 'The following 1 profile is active: "prod"', f"prod: {prod.get('log')}")
exigir(prod.get("health") == '{"status":"UP"}', f"prod: health {prod.get('health')}")
exigir('"version":"0.0.1-SNAPSHOT"' in prod.get("info", ""), f"prod: info {prod.get('info')}")
exigir(prod.get("beans") == "404" and prod.get("env") == "404", f"prod: beans {prod.get('beans')}, env {prod.get('env')}")
exigir('"total":89250.0' in prod.get("post", ""), f"prod: POST {prod.get('post')}")
exigir(prod.get("filas", "0").strip() not in ("", "0"), "prod: la tabla de PostgreSQL esta vacia")
exigir(prod.get("healthy") == "si", "prod: el HEALTHCHECK no llego a healthy")

for png in ("docker-images", "actuator-health-local", "perfil-prod", "pruebas"):
    ruta = os.path.join(capturas, png + ".png")
    exigir(os.path.isfile(ruta) and os.path.getsize(ruta) > 5000, f"falta o esta vacia la captura {png}.png")
sobrantes = [l for l in leer(f"{evid}/logs/arbol-despues.txt").splitlines()
             if not l[3:].startswith(("capturas/", "evid/"))]
exigir(not sobrantes, "quedaron cambios en el arbol: " + "; ".join(sobrantes))

print(json.dumps(r, ensure_ascii=False, indent=2))
if problemas:
    print("\nNO se agregan las capturas:")
    for p in problemas:
        print(" -", p)
    sys.exit(1)
print("\nTodo correcto: tamano de la imagen", round(r["tamano"] / 1024 / 1024), "MB")
