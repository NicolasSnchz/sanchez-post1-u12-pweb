#!/usr/bin/env bash
# Vuelve a levantar la imagen ya construida con los dos perfiles y guarda las respuestas en
# <evid>/respuestas.json para revisar.py. uso: verificar.sh <carpeta-evid>
EVID=$1
IMG=mi-spring-app:local
esperar() { for i in $(seq 1 60); do curl -sf localhost:8080/actuator/health > /dev/null && return 0; sleep 2; done; return 1; }
cod() { curl -s -o /dev/null -w '%{http_code}' "$@"; }
docker rm -f v-dev v-prod > /dev/null 2>&1

# Imagen
TAMANO=$(docker image inspect $IMG --format '{{.Size}}')
USUARIO=$(docker image inspect $IMG --format '{{.Config.User}}')
ENTRY=$(docker image inspect $IMG --format '{{json .Config.Entrypoint}}')
SIN_MVN=$(docker run --rm --entrypoint sh $IMG -c 'which mvn javac >/dev/null 2>&1 && echo no || echo si')

# Perfil dev, sin variables
docker run -d --name v-dev -p 8080:8080 $IMG > /dev/null
esperar; DEV_HEALTH=$(curl -s localhost:8080/actuator/health)
DEV_POST=$(cod -X POST localhost:8080/api/pedidos -H 'Content-Type: application/json' -d '{"clienteEmail":"a@b.co","tipoCliente":"VIP","subtotal":100000}')
DEV_BEANS=$(cod localhost:8080/actuator/beans)
DEV_LOG=$(docker logs v-dev 2>&1 | grep -o 'The following 1 profile is active: "[a-z]*"')
docker rm -f v-dev > /dev/null

# Perfil prod, contra PostgreSQL
docker run -d --name v-prod --network host -e SPRING_PROFILES_ACTIVE=prod \
  -e DATASOURCE_URL=jdbc:postgresql://localhost:5432/appdb -e DATASOURCE_USER=appuser -e DATASOURCE_PASSWORD=apppass $IMG > /dev/null
esperar; PROD_HEALTH=$(curl -s localhost:8080/actuator/health)
PROD_INFO=$(curl -s localhost:8080/actuator/info)
PROD_BEANS=$(cod localhost:8080/actuator/beans)
PROD_ENV=$(cod localhost:8080/actuator/env)
PROD_POST=$(curl -s -X POST localhost:8080/api/pedidos -H 'Content-Type: application/json' -d '{"clienteEmail":"a@b.co","tipoCliente":"MAYORISTA","subtotal":100000}')
PROD_FILAS=$(PGPASSWORD=apppass psql -h localhost -U appuser -d appdb -tAc 'SELECT count(*) FROM pedido')
PROD_LOG=$(docker logs v-prod 2>&1 | grep -o 'The following 1 profile is active: "[a-z]*"')
SANO=no
for i in $(seq 1 30); do [ "$(docker inspect --format '{{.State.Health.Status}}' v-prod)" = healthy ] && { SANO=si; break; }; sleep 5; done
docker logs v-prod > "$EVID/logs/v-prod.log" 2>&1
docker rm -f v-prod > /dev/null

jq -n --arg tam "$TAMANO" --arg usr "$USUARIO" --arg ent "$ENTRY" --arg sinmvn "$SIN_MVN" \
  --arg dh "$DEV_HEALTH" --arg dp "$DEV_POST" --arg db "$DEV_BEANS" --arg dl "$DEV_LOG" \
  --arg ph "$PROD_HEALTH" --arg pi "$PROD_INFO" --arg pb "$PROD_BEANS" --arg pe "$PROD_ENV" \
  --arg pp "$PROD_POST" --arg pf "$PROD_FILAS" --arg pl "$PROD_LOG" --arg sano "$SANO" \
  '{tamano:($tam|tonumber), usuario:$usr, entrypoint:$ent, sin_mvn:$sinmvn,
    dev:{health:$dh, post:$dp, beans:$db, log:$dl},
    prod:{health:$ph, info:$pi, beans:$pb, env:$pe, post:$pp, filas:$pf, log:$pl, healthy:$sano}}' \
  > "$EVID/respuestas.json"
cat "$EVID/respuestas.json"
