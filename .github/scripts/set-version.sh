#!/usr/bin/env bash
# Sets the library version and the demo's dependency on it.
set -euo pipefail
version="$1"
cd "$(dirname "$0")/../.."
${MVN:-./mvnw} -B -q versions:set -DnewVersion="$version" -DgenerateBackupPoms=false -DprocessAllModules=false
perl -0pi -e "s|(<artifactId>criteria-filter</artifactId>\s*<version>)[^<]+(</version>)|\${1}${version}\${2}|" criteria-filter-demo/pom.xml
sed -i -E "s|(dev\.agiro:criteria-filter:)[^'\"]+|\1${version}|" criteria-filter-demo/build.gradle
