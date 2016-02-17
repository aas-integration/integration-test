# Stub for the Dynamic Analysis part.

The script

    ./run.sh 

downloads Randoop, JUnit, and Hamcrest (if not present) and
generates a bunch of unit tests for each project in the corpus. The unit tests will be stored in a sub folder of the corpus project called __randoop_0000. 

What is missing is the connection to Daikon and information exchange with Petablox and other tools.