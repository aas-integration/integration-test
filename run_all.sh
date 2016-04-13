#!/bin/bash


echo "Dynamic Analysis"
cd dynamic_analysis
python run_randoop.py
cd ..

echo "Checker Framework"
cd type_inference
./run.sh
cd ..


echo "Run PetaBlox"
cd into_logicblox
./run.sh
cd ..


echo "Graph Generation"
cd graph_generation
./run.sh
cd ..
