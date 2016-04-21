import os,sys
import inv_check
import insert_jaif

import backend



def main():
	""" SUMMARY: use case of the user-driven functionality of PASCALI.
	Scenario: User provides the concept of Sequence and the equivalent Java
	types, and the concept of sorted sequence and the relevant type invariant.
	Goal: learn how to get from Sequence -> Sorted Sequence. 
	"""

	""" Look for new mapping from 'ontology concepts'->'java type' and run 
	checker framework. Should be implemented in type_inference
	Mapping example:
	  Sequence -> java.lang.Array, java.util.List, LinkedHashSet, etc.

	INPUT: corpus, file containing set of concept->java_type mapping
	OUTPUT: Set of jaif files that are merged into the classes using
	        insert_jaif. 
	BODY: This also triggers back-end labeled graph generation.
	"""

	print "todo" #WERNER


	""" Missing step: interact with PA to add a definition of Sorted Sequence
	which is a specialization of Sequence that has a sortedness invariants. 
	The sortedness invariant gets turned into a Daikon template
	INPUT: user interaction
	OUTPUT: type_annotation and type_invariant (for sorted sequence)

	"""

	print "todo" #MARTIN S

	""" Search for methods that have a return type annotated with Sequence
	and for which we can establish a sortedness invariant (may done by LB).

	INPUT: dtrace file of project
	   type_invariant that we want to check on the dtrace file.
	
	OUTPUT: list of ppt names that establish the invariant. Here a ppt 
	is a Daikon program point, s.a. test01.TestClass01.sort(int[]):::EXIT

	Note: this step translate the type_invariant into a Daikon 
	template (which is a Java file).
	"""
	invariant_substring = "sorted by" #MARTIN S
	corpus = ["TODO"]

	for project in corpus:
		dtrace_file = backend.get_dtrace_file_for_project(project)
		list_of_methods = inv_check.find_ppts_that_establish_inv(dtrace_file, invariant_substring)

	""" Expansion of dynamic analysis results .... 
	Find a list of similar methods that are similar to the ones found above (list_of_methods).
	INPUT: list_of_methods, corpus with labeled graphs generated, threshold value for similarity, 
	OUTPUT: superset_list_of_methods
	"""

	print "todo" # WENCHAO

	""" Update the type annotations for the expanded dynamic analysis results.
	INPUT: superset_list_of_methods, annotation to be added
	OUTPUT: nothing
	EFFECT: updates the type annotations of the methods in superset_list_of_methods.
	This requires some additional checks to make sure that the methods actually 
	perform some kind of sorting. Note that we do it on the superset because the original
	list_of_methods might miss many implementations because fuzz testing could not 
	reach them.
	"""
	for class_file in []: # MARTIN
		generated_jaif_file = "TODO"
		insert_jaif.merge_jaif_into_class(class_file, generated_jaif_file)


	""" Ordering of expanded dynamic analysis results ....
	Find the k 'best' implementations in superset of list_of_methods
	INPUT: superset_list_of_methods, corpus, k
	OUTPUT: k_list_of_methods 
	Note: similarity score is used. may consider using other scores; e.g., TODO:???
	"""

	print "todo" # Huascar

	""" 
	Close the loop and add the best implementation found in the previous
	step back to the ontology.
	INPUT: k_list_of_methods
	OUTPUT: patch file for the ontology. Worst case: just add the 'best' implementation
	found in the corpus as a blob to the ontology. Best case: generate an equivalent 
	flow-graph in the ontology.
	"""
	print "TODO" # ALL




if __name__ == '__main__':
	main()
