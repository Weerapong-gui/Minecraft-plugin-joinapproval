#!/usr/bin/env bash
# Build JoinApproval.jar against a Paper server install (no Gradle needed).
# Usage: ./build.sh /path/to/paper-server-dir   (the folder containing libraries/ and versions/)
set -euo pipefail
SERVER="${1:?usage: ./build.sh /path/to/paper-server-dir}"
CP="$(find "$SERVER/libraries" "$SERVER/versions" -name '*.jar' | tr '\n' ':')"
rm -rf build && mkdir -p build/classes
javac --release 25 -nowarn -cp "$CP" -d build/classes src/main/java/th/park/joinapproval/JoinApproval.java
cp src/main/resources/plugin.yml src/main/resources/config.yml build/classes/
jar cf build/JoinApproval.jar -C build/classes .
echo "built build/JoinApproval.jar"
