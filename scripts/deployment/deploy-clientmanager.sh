#!/bin/bash
# Ejecutado como root (vía sudo) por el usuario "deploy" en 190.7.56.85.
# No recibe argumentos: espera siempre el jar en $INCOMING (subido por scp antes de invocar este script).
set -euo pipefail

APP_DIR="/opt/clientmanager"
JAR_NAME="clientmanager-0.0.1-SNAPSHOT.jar"
JAR_PATH="$APP_DIR/$JAR_NAME"
INCOMING="/home/deploy/incoming/${JAR_NAME}.new"
BACKUP_PREFIX="$APP_DIR/${JAR_NAME}.bak-"
KEEP_BACKUPS=5
HEALTHCHECK_URL="http://localhost:9090/"
HEALTHCHECK_TIMEOUT=200
HEALTHCHECK_INTERVAL=5
ROLLBACK_WAIT=90
LOG="/var/log/clientmanager-deploy.log"

log() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*" | tee -a "$LOG"; }

check_health() {
    curl -s -o /dev/null -w '%{http_code}' "$HEALTHCHECK_URL" 2>/dev/null || echo "000"
}

if [ ! -f "$INCOMING" ]; then
    log "ERROR: no se encontró $INCOMING. ¿Se subió el jar antes de invocar este script?"
    exit 1
fi

NEW_SIZE=$(stat -c%s "$INCOMING")
if [ "$NEW_SIZE" -lt 1000000 ]; then
    log "ERROR: $INCOMING pesa solo $NEW_SIZE bytes, parece incompleto. Abortando sin tocar el servicio."
    exit 1
fi

BACKUP_FILE="${BACKUP_PREFIX}$(date +%Y%m%d-%H%M%S)"
log "Backup: $JAR_PATH -> $BACKUP_FILE"
cp "$JAR_PATH" "$BACKUP_FILE"

log "Podando backups viejos (se conservan los últimos $KEEP_BACKUPS)"
# shellcheck disable=SC2086
ls -1t ${BACKUP_PREFIX}* 2>/dev/null | tail -n +$((KEEP_BACKUPS + 1)) | xargs -r rm -f

log "Deteniendo clientmanager..."
service clientmanager stop

log "Reemplazando jar..."
mv "$INCOMING" "$JAR_PATH"
chown apache:apache "$JAR_PATH"
chmod 664 "$JAR_PATH"

log "Iniciando clientmanager..."
service clientmanager start

log "Healthcheck: esperando hasta ${HEALTHCHECK_TIMEOUT}s en $HEALTHCHECK_URL"
elapsed=0
healthy=0
code="000"
while [ "$elapsed" -lt "$HEALTHCHECK_TIMEOUT" ]; do
    code=$(check_health)
    if [[ "$code" =~ ^[23] ]]; then
        healthy=1
        break
    fi
    sleep "$HEALTHCHECK_INTERVAL"
    elapsed=$((elapsed + HEALTHCHECK_INTERVAL))
    log "  ...HTTP $code, ${elapsed}s transcurridos"
done

if [ "$healthy" -eq 1 ]; then
    log "OK: clientmanager respondió HTTP $code tras ${elapsed}s. Deploy exitoso."
    exit 0
fi

log "FALLO healthcheck tras ${HEALTHCHECK_TIMEOUT}s (último HTTP $code). Iniciando rollback a $BACKUP_FILE"
service clientmanager stop || true
cp "$BACKUP_FILE" "$JAR_PATH"
chown apache:apache "$JAR_PATH"
chmod 664 "$JAR_PATH"
service clientmanager start

log "Esperando ${ROLLBACK_WAIT}s para confirmar que el rollback levantó..."
sleep "$ROLLBACK_WAIT"
code=$(check_health)
if [[ "$code" =~ ^[23] ]]; then
    log "Rollback OK: la versión anterior responde de nuevo (HTTP $code)."
else
    log "ALERTA: ni la versión nueva ni el rollback responden (HTTP $code). Requiere intervención manual YA."
fi

exit 1
