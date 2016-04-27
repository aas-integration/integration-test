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

The following command will compute the 2 most typical implementation of the methods specified in the methods.txt file. It will also extract the 2 most frequent concepts in those implementations.

```
› ./run.sh -k 2 -f ./methods.txt
```

The output is:

```
Finds k most typical implementation in corpus.
Sort03                           public class ....
Sort14                           public class ....

Exiting with Code 0
Extracts concepts from corpus.
[sort, array]
```


