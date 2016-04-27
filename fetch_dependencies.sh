#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
pushd $DIR &> /dev/null

# Libraries
mkdir -p libs
pushd libs &> /dev/null

JARS=(
    "http://www.csl.sri.com/users/schaef/jars/daikon.jar"
    "http://www.csl.sri.com/users/schaef/jars/randoop.jar"
    "https://www.dropbox.com/s/i1iqgf9w7jk1x3x/prog2dfg.jar"
    "https://github.com/junit-team/junit/releases/download/r4.12/junit-4.12.jar"
    "http://search.maven.org/remotecontent?filepath=org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar"
    "https://github.com/petablox-project/petablox/releases/download/v1.0/petablox.zip"
)

for jar in "${JARS[@]}"
do
    base=$(basename $jar)
    echo Fetching $base
    curl -L -o $base $jar &> /dev/null
done

# Unpack petablox
unzip -o petablox.zip
rm petablox.zip

popd &> /dev/null # Exit libs


# Tools
mkdir -p tools
pushd tools &> /dev/null

# Fetch do-like-javac
if [ ! -d do-like-javac ]; then
    git clone https://github.com/SRI-CSL/do-like-javac.git
    pushd do-like-javac &> /dev/null
    git checkout checker
    popd &> /dev/null # Exit do-like-javac
else
    pushd do-like-javac &> /dev/null
    git pull
    popd &> /dev/null # Exit do-like-javac
fi

if [ ! -d "generic-type-inference-solver" ]; then
    git clone https://github.com/Jianchu/generic-type-inference-solver.git
fi

pushd generic-type-inference-solver &> /dev/null
git pull
export TRAVIS_BUILD_DIR=`pwd`
./.travis-build-without-test.sh
popd &> /dev/null # Exit generic-type-inference-solver

popd &> /dev/null # Exit tools

popd &> /dev/null # Exit integration-test
