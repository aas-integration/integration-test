#!/bin/bash

echo "Finds k most typical implementation in corpus."
# "./cue typical -d $CORPUS_DIR"
./cue typical $*  

echo "Extracts concepts from corpus."
./cue concepts $*