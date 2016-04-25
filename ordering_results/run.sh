#!/bin/bash

CORPUS_DIR=../corpus/

K=1
M=methods.txt

echo "Finds k most typical implementation in corpus."
# "./cue typical -d $CORPUS_DIR"
./cue typical -d $CORPUS_DIR 

echo "Extracts concepts from corpus."
./cue concepts -k 2 -m $M -f $CORPUS_DIR