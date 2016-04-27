# integration-test
Big integration test of all tools

Our new story is: 
- Start from an empty ontology.
- Add a concept 'Sequence' to the ontology that maps to 'java.util.LinkedList', 'Array', etc.
- Run checker framework to propagate the corresponding annotations in the corpus.
- Add concept 'Sorted Sequence' to the ontology which is a specialization of 'Sequence' with a sortedness invariant.
- Generate a Daikon pattern from the sortedness invariant.
- Find all methods that take a Sequence and produce a Sequence and establish that invariant.
- Use program similarity to find more methods like this (because dynamic analysis will only find a few).
- Order the resulting set by 'quality'.
- Pick the best implementation and add it back to the ontology as a cliche for sorting.

How to run:

    ./fetch_dependencies.sh  

This downloads all the jars, compiles stuff, etc. Only needs to be run once.

    python backend.py

Compiles the corpus projects, generates tests and dtrace files, computes dot files, runs petablox.

    python forntend.py

or

    python forntend.py Sort04 Sort15

runs the main loop over the corpus, or the subset of corpus programs specified in the args.


Results of all tools running can be found on Travis: 
[![Click here to see the Results of the experiments on TravisCI](https://travis-ci.org/aas-integration/integration-test.svg?branch=master)](https://travis-ci.org/aas-integration/integration-test)

# Obtaining LogicBlox

LogicBlox is released every month. It is available for download at
https://download.logicblox.com . To obtain a username and password for
this website, please fill out the form at
http://www.logicblox.com/learn/academic-license-request-form/ and
indicate that you are a MUSE team member.

LogicBlox offers Linux 64-bit as well as OSX distributions. If you
problems installing LogicBlox, please contact
muse-users@logicblox.com. You might want to be signed up for this
group as well!

Documentation is available at
https://developer.logicblox.com/documentation/ . Depending on what you
are most interested in, you want to check the reference manual or the
admin manual.

Just email martin.bravenboer@logicblox.com if you have any further
questions.
