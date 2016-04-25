#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
pushd $DIR &> /dev/null

mkdir -p libs
pushd libs &> /dev/null

JARS=(
    "http://www.csl.sri.com/users/schaef/jars/daikon.jar"
    "http://www.csl.sri.com/users/schaef/jars/randoop.jar"
    "https://github.com/junit-team/junit/releases/download/r4.12/junit-4.12.jar"
    "http://search.maven.org/remotecontent?filepath=org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar"
    "https://github.com/petablox-project/petablox/releases/download/v1.0/petablox.zip"
)
# Download .jars

for jar in "${JARS[@]}"
do
    base=$(basename $jar)
    echo Fetching $base
    curl -L -o $base $jar &> /dev/null
done

# Unpack petablox
unzip -o petablox.zip
rm petablox.zip

popd &> /dev/null

# Fetch do-like-javac
if [ ! -d do-like-javac ]; then
    git clone https://github.com/SRI-CSL/do-like-javac.git
    pushd do-like-javac &> /dev/null
    git checkout checker
    popd &> /dev/null
else
    pushd do-like-javac &> /dev/null
    git pull
    popd &> /dev/null
fi


popd &> /dev/null
