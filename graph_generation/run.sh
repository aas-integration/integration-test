#!/bin/bash

CORPUS_DIR=../corpus/benchmarks

if [ ! -f "make_dots.py" ]; then
  echo "Downloading graph generation scripts"
  wget http://www.csl.sri.com/~schaef/graphgen.zip
  unzip graphgen.zip
  rm graphgen.zip
fi

echo "Create dot files with soot."
#TODO: change prog2dfg to petablox
python make_dots.py prog2dfg

echo "Precompute the kernels."
python ./scripts/precompute_kernel.py $CORPUS_DIR kernels.txt
