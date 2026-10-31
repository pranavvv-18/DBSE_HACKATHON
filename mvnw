#!/usr/bin/env sh
set -eu

MAVEN_VERSION=3.9.9
WRAPPER_BASE="${MAVEN_USER_HOME:-${HOME}/.m2}/wrapper/dists/apache-maven-${MAVEN_VERSION}-bin/taskflow"
MAVEN_HOME="${WRAPPER_BASE}/apache-maven-${MAVEN_VERSION}"

if [ ! -x "${MAVEN_HOME}/bin/mvn" ]; then
  mkdir -p "${WRAPPER_BASE}"
  ARCHIVE="${WRAPPER_BASE}/apache-maven-${MAVEN_VERSION}-bin.zip"
  URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/${MAVEN_VERSION}/apache-maven-${MAVEN_VERSION}-bin.zip"
  if command -v curl >/dev/null 2>&1; then
    curl --fail --location --silent --show-error --retry 2 --connect-timeout 15 --max-time 180 "${URL}" -o "${ARCHIVE}"
  elif command -v wget >/dev/null 2>&1; then
    wget -q "${URL}" -O "${ARCHIVE}"
  else
    echo "Maven Wrapper requires curl or wget on first run." >&2
    exit 1
  fi
  python3 - "${ARCHIVE}" "${WRAPPER_BASE}" <<'PY'
import pathlib, sys, zipfile
archive = pathlib.Path(sys.argv[1])
destination = pathlib.Path(sys.argv[2]).resolve()
with zipfile.ZipFile(archive) as bundle:
    for member in bundle.infolist():
        target = (destination / member.filename).resolve()
        if target != destination and destination not in target.parents:
            raise SystemExit("Unsafe path in Maven distribution archive")
    bundle.extractall(destination)
archive.unlink(missing_ok=True)
PY
fi

exec "${MAVEN_HOME}/bin/mvn" "$@"
