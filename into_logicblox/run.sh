#!/bin/bash

CORPUS_DIR=../corpus/benchmarks

if [ ! -f "petablox.jar" ]; then
  echo "Downloading petablox"
  wget https://github.com/petablox-project/petablox/releases/download/v1.0/petablox.zip
  unzip petablox.zip
  rm petablox.zip
fi

echo "Run petablox on all corpus projects"
#TODO: change prog2dfg to petablox
python petablox_runner.py $CORPUS_DIR
