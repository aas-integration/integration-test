#!/bin/bash

ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
export JSR308=$ROOT
echo $JSR308
export CLASSPATH=${CLASSPATH}:$JSR308/generic-type-inference-solver/bin
export AFU=$JSR308/annotation-tools/annotation-file-utilities
export PATH=${PATH}:$AFU/scripts
CORPUS_DIR=../../corpus
DLJC=$JSR308/do-like-javac

if [ -d "generic-type-inference-solver" ]; then
  (cd generic-type-inference-solver && git pull)
else
  git clone https://github.com/Jianchu/generic-type-inference-solver.git
fi

if [ -d "do-like-javac" ]; then
  (cd do-like-javac && git pull && git checkout checker)
else
  git clone https://github.com/SRI-CSL/do-like-javac.git
  (cd do-like-javac && git checkout checker)
fi

cd generic-type-inference-solver
export TRAVIS_BUILD_DIR=`pwd`

./.travis-build-without-test.sh

#TODO: generate sequence annotations and adapt OntologyUtils
#recompile generic-type-inference-solver

rm -rf $CORPUS_DIR/annotated/
#infer all examples in corpus
for f in $CORPUS_DIR/*
do
  if [ -d "$f" ]; then
    cd $f
    ant clean
    echo "Inferring ${PWD##*/}:"
    python $DLJC/dljc -t inference --solverArgs="backEndType=maxsatbackend.MaxSat" --checker ontology.OntologyChecker --solver constraintsolver.ConstraintSolver -o logs -m ROUNDTRIP -afud $CORPUS_DIR/annotated -- ant
  fi
done

# Sort03 causes checker framework inference crushing,
# the problem has been filed as Issue #24 in checker framework inference.