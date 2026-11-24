#!/bin/bash
# Ejecutado como root (vía sudo) por el usuario "deploy" en 190.7.56.85.
# No recibe argumentos: espera siempre el jar en $INCOMING (subido por scp antes de invocar este script).
set -euo pipefail

APP_DIR="/opt/clientmanager"
APP_PROPS="$APP_DIR/application.properties"
JAR_NAME="clientmanager-0.0.1-SNAPSHOT.jar"
JAR_PATH="$APP_DIR/$JAR_NAME"
INCOMING="/home/deploy/incoming/${JAR_NAME}.new"
BACKUP_PREFIX="$APP_DIR/${JAR_NAME}.bak-"
KEEP_BACKUPS=5
DB_BACKUP_DIR="/var/backups/clientmanager-db"
DB_BACKUP_KEEP=15
HEALTHCHECK_URL="http://localhost:9090/"
HEALTHCHECK_TIMEOUT=200
HEALTHCHECK_INTERVAL=5
ROLLBACK_WAIT=90
LOG="/var/log/clientmanager-deploy.log"

log() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*" | tee -a "$LOG"; }

check_health() {
    local code
    code=$(curl -s -o /dev/null -w '%{http_code}' "$HEALTHCHECK_URL" 2>/dev/null)
    echo "${code:-000}"
}

# Vuelca la base de la app (mismas credenciales que ya usa application.properties,
# no un usuario nuevo) justo mientras el servicio está parado, antes de arriesgar
# cualquier migración de Liquibase del arranque siguiente. Si falla, no sigue.
backup_database() {
    mkdir -p "$DB_BACKUP_DIR"
    chmod 700 "$DB_BACKUP_DIR"

    local url user pass dbname
    url=$(grep -E '^spring\.datasource\.url=' "$APP_PROPS" | head -1 | cut -d'=' -f2-)
    user=$(grep -E '^spring\.datasource\.username=' "$APP_PROPS" | head -1 | cut -d'=' -f2-)
    pass=$(grep -E '^spring\.datasource\.password=' "$APP_PROPS" | head -1 | cut -d'=' -f2-)
    dbname=$(printf '%s' "$url" | sed -E 's#.*/([^/?]+)(\?.*)?$#\1#')

    if [ -z "$url" ] || [ -z "$user" ] || [ -z "$dbname" ]; then
        log "ERROR: no pude leer url/username/dbname de $APP_PROPS. Abortando backup de BD."
        return 1
    fi

    local dump_file="$DB_BACKUP_DIR/${dbname}-$(date +%Y%m%d-%H%M%S).sql.gz"
    log "Backup de BD: volcando '$dbname' a $dump_file"
    if MYSQL_PWD="$pass" mysqldump --single-transaction --quick --routines --triggers -u "$user" "$dbname" | gzip > "$dump_file"; then
        log "Backup de BD OK ($(du -h "$dump_file" | cut -f1))"
    else
        log "ERROR: mysqldump falló."
        rm -f "$dump_file"
        return 1
    fi

    log "Podando backups de BD viejos (se conservan los últimos $DB_BACKUP_KEEP)"
    ls -1t "$DB_BACKUP_DIR"/*.sql.gz 2>/dev/null | tail -n +$((DB_BACKUP_KEEP + 1)) | xargs -r rm -f
    return 0
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

if ! backup_database; then
    log "ERROR: el backup de la base falló. Reiniciando con el jar anterior (sin tocar) y abortando el deploy."
    service clientmanager start
    exit 1
fi

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
