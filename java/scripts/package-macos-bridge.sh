#!/bin/zsh
set -e

SCRIPT_DIR="${0:A:h}"
JAVA_ROOT="${SCRIPT_DIR:h}"
cd "$JAVA_ROOT"
mvn -q -pl skillport-bridge -am -DskipTests package
rm -rf skillport-bridge/target/jpackage-input
mkdir -p skillport-bridge/target/jpackage-input
cp skillport-bridge/target/skillport-bridge-1.0.0-SNAPSHOT.jar skillport-bridge/target/jpackage-input/
jpackage --type pkg --name SkillPortBridge --input skillport-bridge/target/jpackage-input \
  --main-jar skillport-bridge-1.0.0-SNAPSHOT.jar --main-class com.skillport.bridge.SkillPortBridgeApplication \
  --dest skillport-bridge/target/installer
