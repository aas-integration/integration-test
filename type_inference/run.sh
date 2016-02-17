#!/bin/bash

CORPUS_DIR=../corpus/benchmarks

if [ ! -d "checker-framework-inference" ]; then
  echo "Downloading checker-framework"
  git clone https://github.com/typetools/checker-framework-inference.git
  cd checker-framework-inference
  echo "Checking out fixed commit for reproducability."
  git checkout c6692c7c6ae0d6dfc785257d2468c3a2908f39e0
  export TRAVIS_BUILD_DIR=./
  ./.travis-build.sh
  cd ..
fi

cd checker-framework-inference
export TRAVIS_BUILD_DIR=./
./.travis-build.sh
cd ..


echo "Apply checker-framework-inference"
#TODO: change prog2dfg to petablox

