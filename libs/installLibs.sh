#!/bin/bash

if [ "$#" -ne 2 ]; then
    echo -e "\e[91mRequired syntax: ./libs.sh <maven binary path> <jars directory>"
    exit 1
fi

mavenPath=$1
jarsPath=$2

echo -e "\e[93mInstalling EnderTranslate dependencies jars from \"$jarsPath\" to the local repository..."

echo -e "Maven path: $mavenPath\e[39m"

"$mavenPath" install:install-file -Dfile=$jarsPath/entitylib-api-2.2.1-SNAPSHOT.jar -DgroupId=com.github.Tofaa2.EntityLib -DartifactId=api -Dversion=2.2.1 -Dpackaging=jar -DgeneratePom=true
"$mavenPath" install:install-file -Dfile=$jarsPath/entitylib-spigot-2.0-SNAPSHOT.jar -DgroupId=com.github.Tofaa2.EntityLib -DartifactId=spigot -Dversion=2.2.1 -Dpackaging=jar -DgeneratePom=true
"$mavenPath" install:install-file -Dfile=$jarsPath/entitylib-common-2.0-SNAPSHOT.jar -DgroupId=com.github.Tofaa2.EntityLib -DartifactId=common -Dversion=2.2.1 -Dpackaging=jar -DgeneratePom=true
"$mavenPath" install:install-file -Dfile=$jarsPath/entitylib-velocity-2.0-SNAPSHOT.jar -DgroupId=com.github.Tofaa2.EntityLib -DartifactId=velocity -Dversion=2.2.1 -Dpackaging=jar -DgeneratePom=true

"$mavenPath" install:install-file -Dfile=$jarsPath/packetevents-api-2.5.0-SNAPSHOT.jar -DgroupId=com.github.retrooper.packetevents -DartifactId=api -Dversion=2.5.0 -Dpackaging=jar -DgeneratePom=true
"$mavenPath" install:install-file -Dfile=$jarsPath/packetevents-bungeecord-2.5.0-SNAPSHOT.jar -DgroupId=com.github.retrooper.packetevents -DartifactId=bungeecord -Dversion=2.5.0 -Dpackaging=jar -DgeneratePom=true
"$mavenPath" install:install-file -Dfile=$jarsPath/packetevents-spigot-2.5.0-SNAPSHOT.jar -DgroupId=com.github.retrooper.packetevents -DartifactId=spigot -Dversion=2.5.0 -Dpackaging=jar -DgeneratePom=true
"$mavenPath" install:install-file -Dfile=$jarsPath/packetevents-velocity-2.5.0-SNAPSHOT.jar -DgroupId=com.github.retrooper.packetevents -DartifactId=velocity -Dversion=2.5.0 -Dpackaging=jar -DgeneratePom=true


echo -e "\e[92mOperation complete."