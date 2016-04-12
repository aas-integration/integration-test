#!/bin/bash

CORPUS_DIR=../corpus/benchmarks

if [ -d "generic-type-inference-solver" ]; then
  (cd generic-type-inference-solver && git pull)
else
  git clone https://github.com/wmdietl/generic-type-inference-solver.git
fi

cd generic-type-inference-solver
export TRAVIS_BUILD_DIR=`pwd`

./.travis-build.sh
cd ..


echo "Apply checker-framework-inference"


