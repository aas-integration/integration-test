# Ordering of Dynamic Analysis Results


The goal of this step is to find the *k* most typical (best) implementation in a set of relevant (and similar) methods
returned by the graph similarity step. 


*Input*: 
	1. A set of relevant (and similar) methods   
	2. A reduced version of original corpus (containing classes where relevant methods exist)
	3. A *k* parameter.

*Output*:
	The *k* most typical (best) implementation found in the set of relevant (and similar) methods.


*Assumption*: Source files in the corpus are syntactically correct (and hence compilable).


Additional feature:

Finds the k most frequent concepts in a corpus.

*Input*:
	 1. A list of method names to introspect
	 2. A *k* parameter
	 3. The corpus
	 
*Output*:
The *k* most frequent concepts in a corpus	 

# Example

The following two commands demonstrates the typicality and concepts location components:

Finds the 2 most typical implementation of the methods specified in the methods.txt:

```
› ./run.sh -k 2 -f ./methods.txt -t typicality
```

The output is:

../corpus/Sort05/src/Sort05.java::sort(int[]):int[]
```
Finds k most typical implementation in corpus.
../corpus/Sort03/src/Sort03
../corpus/Sort14/src/Sort14
```

**Note**: A file called `typicalset.txt` will be produced after executing the above command. This file
will contain the method of interest per returned source file. 

Finds the 2 most frequent concepts in those implementations.

```
› ./run.sh -k 2 -f ./methods.txt -t concepts
```

The output is:

```
Extracts concepts from corpus.
[sort, array]
```


Or, if you prefer running both components with one single command, then avoid using the `-t` argument and its value. E.g., the following command runs both typicality and concept location components:

```
› ./run.sh -k 2 -f ./methods.txt
```

The output is:

```
Finds k most typical implementation in corpus.
../corpus/Sort03/src/Sort03
../corpus/Sort14/src/Sort14

Exiting with Code 0
Extracts concepts from corpus.
[sort, array]
```

