#!/usr/bin/env bash
# Empaqueta el servidor REP para Linux con su propio Java incluido (no requiere instalar
# Java ni MySQL). Resultado: target/empaquetado/rep-servidor_<versión>_amd64.deb
#
#   empaquetado/servidor-linux.sh            # .deb (Debian, Ubuntu, Mint…)
#   TIPO=rpm empaquetado/servidor-linux.sh   # .rpm (Fedora…; requiere rpmbuild)
#   TIPO=app-image empaquetado/servidor-linux.sh   # carpeta portable
#
# Requisitos para empaquetar: JDK 17+ con jpackage, y fakeroot + dpkg-deb (o rpmbuild).
set -euo pipefail
cd "$(dirname "$0")/.."

TIPO="${TIPO:-deb}"
VERSION="$(sed -n '0,/<artifactId>main<\/artifactId>/d; s/.*<version>\(.*\)<\/version>.*/\1/p' pom.xml | head -1)"
SALIDA=target/empaquetado
echo "== REP Servidor $VERSION ($TIPO)"

if [ "${OMITIR_COMPILACION:-0}" != "1" ]; then
  ./mvnw -B -ntp -DskipTests package
fi
rm -rf "$SALIDA" && mkdir -p "$SALIDA/entrada"
cp "target/main-$VERSION.jar" "$SALIDA/entrada/rep-servidor.jar"
cp empaquetado/LEEME-servidor.txt "$SALIDA/entrada/LEEME.txt"

OPCIONES_LINUX=()
if [ "$TIPO" != "app-image" ]; then
  OPCIONES_LINUX=(--linux-package-name rep-servidor --linux-shortcut --linux-menu-group Education
                  --linux-app-category education --linux-deb-maintainer "soporte@rep.local")
fi

jpackage \
  --type "$TIPO" \
  --name rep-servidor \
  --app-version "$VERSION" \
  --vendor "REP" \
  --description "Servidor REP: gestión educativa en red local, con o sin internet" \
  --icon empaquetado/recursos/rep-servidor.png \
  --input "$SALIDA/entrada" \
  --main-jar rep-servidor.jar \
  --arguments "--spring.profiles.active=local" \
  --java-options "-Xmx768m" \
  --java-options "-Dfile.encoding=UTF-8" \
  --jlink-options "--strip-debug --no-man-pages --no-header-files" \
  "${OPCIONES_LINUX[@]}" \
  --dest "$SALIDA"

echo "== Listo:"
ls -lh "$SALIDA" | grep -v entrada
