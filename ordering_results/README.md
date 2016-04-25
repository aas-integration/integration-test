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