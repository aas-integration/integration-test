#!/bin/bash
CORPUS_DIR=../../corpus
SCRIPT=$(readlink -f $0)
MYDIRPATH=`dirname $SCRIPT`
#DLJC=$MYDIRPATH/do-like-javac
DLJC_BIN=$MYDIRPATH/pascali-public/do-like-javac/bin

if [ -d "generic-type-inference-solver" ]; then
  (cd generic-type-inference-solver && git pull)
else
  git clone https://github.com/Jianchu/generic-type-inference-solver.git
fi

#if [ -d "do-like-javac" ]; then
if [ -d "pascali-public" ]; then
  (cd do-like-javac && git pull)
else
  #git clone https://github.com/SRI-CSL/do-like-javac.git
  git clone https://github.com/Jianchu/pascali-public.git
fi

cd generic-type-inference-solver
export TRAVIS_BUILD_DIR=`pwd`

./.travis-build-without-test.sh

for f in $CORPUS_DIR/*
do
  if [ -d "$f" ]; then
  	cd $f
  	ant clean
  	echo "Inferring ${PWD##*/}:"
    python $DLJC_BIN/do-like-javac.py -t inference --solverArgs="backEndType=maxsatbackend.MaxSat" --checker ontology.OntologyChecker --solver constraintsolver.ConstraintSolver -o logs -m ROUNDTRIP -afud $CORPUS_DIR/annotated -- ant
  fi
done

# Sort03 causes checker framework inference crushing, 
# the problem has been filed as Issue #24 in checker framework inference.
cd ..