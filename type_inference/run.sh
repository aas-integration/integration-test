#!/bin/bash

CORPUS_DIR=../../corpus/benchmarks
CORPUS_SORT1_SRC_DIR=../../corpus/sorting/00_sort/Sort01/src
CORPUS_SORT2_SRC_DIR=../../corpus/sorting/00_sort/Sort02/src
CORPUS_SORT3_SRC_DIR=../../corpus/sorting/00_sort/Sort03/src

if [ -d "generic-type-inference-solver" ]; then
  (cd generic-type-inference-solver && git pull)
else
  git clone https://github.com/Jianchu/generic-type-inference-solver.git
fi

cd generic-type-inference-solver
export TRAVIS_BUILD_DIR=`pwd`

./.travis-build-without-test.sh

echo "Inferring example Sort01"
bash scripts/ontology/roundtripOntologyMaxSat.sh $CORPUS_SORT1_SRC_DIR/*.java
echo "Inferring example Sort02"
bash scripts/ontology/roundtripOntologyMaxSat.sh $CORPUS_SORT2_SRC_DIR/*.java
# Following example causes checker framework inference crushing, 
# the problem has been filed as Issue #24 in checker framework inference.

# echo "Inferring example Sort03"
# bash scripts/ontology/roundtripOntologyMaxSat.sh $CORPUS_SORT3_SRC_DIR/*.java
cd ..