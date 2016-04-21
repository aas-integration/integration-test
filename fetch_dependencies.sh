#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
pushd $DIR &> /dev/null

mkdir -p libs
# Download .jars
if [ ! -e libs/daikon.jar ]; then
    curl -L -o libs/daikon.jar "http://plse.cs.washington.edu/daikon/download/daikon.jar"
fi

if [ ! -e libs/randoop-2.1.4.jar ]; then
    curl -L -o libs/randoop-2.1.4.jar "https://github.com/randoop/randoop/releases/download/v2.1.4/randoop-2.1.4.jar"
fi

if [ ! -e libs/junit-4.12.jar ]; then
    curl -L -o libs/junit-4.12.jar "https://github.com/junit-team/junit/releases/download/r4.12/junit-4.12.jar"
fi

if [ ! -e libs/hamcrest-core-1.3.jar ]; then
    curl -L -o libs/hamcrest-core-1.3.jar "http://search.maven.org/remotecontent?filepath=org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar"
fi


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
