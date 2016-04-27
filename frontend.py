import sys, os, shutil

import inv_check
import insert_jaif
import ontology_to_daikon

import backend
import common

def run_inference(project):
  project_dir = common.get_project_dir(project)
  annotation_dir = os.path.join(project_dir, common.DLJC_OUTPUT_DIR, 'annotations')

  jsr308 = common.TOOLS_DIR
  os.environ['JSR308'] = jsr308
  classpath = os.path.join(jsr308, 'generic-type-inference-solver', 'bin')
  if os.environ.get('CLASSPATH'):
    os.environ['CLASSPATH'] += ':' + classpath
  else:
    os.environ['CLASSPATH'] = classpath

  afu = os.path.join(jsr308, 'annotation-tools', 'annotation-file-utilities')
  os.environ['AFU'] = afu
  os.environ['PATH'] += ':' + os.path.join(afu, 'scripts')

  if os.path.isdir(annotation_dir):
    shutil.rmtree(annotation_dir)

  with common.cd(project_dir):
    common.clean_project(project)
    common.run_dljc(project,
                    ['inference'],
                    ['--solverArgs=backEndType=maxsatbackend.MaxSat',
                     '--checker', 'ontology.OntologyChecker',
                     '--solver', 'constraintsolver.ConstraintSolver',
                     '-m', 'ROUNDTRIP',
                     '-afud', annotation_dir])



def read_jaif_file(jaif_file):
  current_package = ""
  current_class = ""
  current_method = ""
  with open(jaif_file, 'r') as f:
    for line in f.readlines():
      if line.startswith("package "):
        current_package = line[len("package "):line.find(":")]
      if line.startswith("method "):
        current_class = line[len("class "):line.find(":")]
      if line.startswith("method "):
        current_method = line[len("method "):line.find(":")]

      if line.startswith("insert-annotation Method.parameter"):
        print ("YEAH {}{}".format(current_class, current_method))
      if line.startswith("insert-annotation Method.type"):
        print ("RET {}{}".format(current_class, current_method))



def find_methods_with_signature(corpus, return_annotation, param_annotation_list):

  good_methods = []

  for project in corpus:
    project_dir = common.get_project_dir(project)
    jaif_file = os.path.join(project_dir, "default.jaif")
  
    has_param = False
    has_ret = False
    current_package = ""
    current_class = ""
    current_method = ""
    with open(jaif_file, 'r') as f:
      for line in f.readlines():
        if line.startswith("package "):
          current_package = line[len("package "):line.find(":")]
        if line.startswith("class "):
          current_class = line[len("class "):line.find(":")]
          has_param = False
          has_ret = False          
        if line.startswith("method "):
          current_method = line[len("method "):line.find(":")]

        if line.startswith("insert-annotation Method.parameter"):
          s = line[len("insert-annotation Method.parameter "):]
          param_idx = int(s[:s.find(",")])
          if len(param_annotation_list) > param_idx and param_annotation_list[param_idx] in line:
            has_param = True
          else: 
            has_param = False
        if line.startswith("insert-annotation Method.type") and return_annotation in line:
          has_ret = True

        if has_param==True and has_ret==True:
          good_methods += [(project, current_package, current_class, current_method)]
          print ("Relevant Method: {}.{}".format(current_class,current_method))
          has_param = False
          has_ret = False


def main(corpus):
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

  for project in corpus:
    run_inference(project)

  """ Missing step: interact with PA to add a definition of Sorted Sequence
  which is a specialization of Sequence that has a sortedness invariants.
  The sortedness invariant gets turned into a Daikon template
  INPUT: user interaction
  OUTPUT: type_annotation and type_invariant (for sorted sequence)

  """

  ontology_invariant_file = "TODO_from_Howie.txt"
  with open(ontology_invariant_file, 'w') as f:
    f.write("TODO")

  invariant_name = "TODO_sorted_sequence"

  daikon_pattern_java_file = ontology_to_daikon.create_daikon_invariant(ontology_invariant_file, invariant_name)

  """ Search for methods that have a return type annotated with Sequence
  and for which we can establish a sortedness invariant (may done by LB).

  INPUT: dtrace file of project
         daikon_pattern_java_file that we want to check on the dtrace file.

  OUTPUT: list of ppt names that establish the invariant. Here a ppt
  is a Daikon program point, s.a. test01.TestClass01.sort(int[]):::EXIT

  Note: this step translate the type_invariant into a Daikon
  template (which is a Java file).
  """

  pattern_class_name = invariant_name
  pattern_class_dir = os.path.join(common.WORKING_DIR, "invClass")
  if os.path.isdir(pattern_class_dir):
    shutil.rmtree(pattern_class_dir)
  os.mkdir(pattern_class_dir)

  cmd = ["javac", "-g", "-classpath", common.get_jar('daikon.jar'),
         daikon_pattern_java_file, "-d", pattern_class_dir]
  common.run_cmd(cmd)

  list_of_methods = []
  for project in corpus:
    dtrace_file = backend.get_dtrace_file_for_project(project)
    if not dtrace_file:
      print ("Ignoring folder {} because it does not contain dtrace file".format(project))
      continue
    ppt_names = inv_check.find_ppts_that_establish_inv(dtrace_file, pattern_class_dir, pattern_class_name)
    methods = set()
    for ppt in ppt_names:
      method_name = ppt[:ppt.find(':::EXIT')]
      methods.add(method_name)
    list_of_methods +=[(project, methods)]

  print ("\n   ************")
  print ("The following corpus methods establish Howies invariant in the return value:")
  for project, methods in list_of_methods:
    if len(methods)>0:
      print (project)
      for m in methods:
        print("\t{}".format(m))
  print ("\n   ************")

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

  #TODO: create input file for huascar where each line is formatted like:
  # ../corpus/Sort05/src/Sort05.java::sort(int[]):int[]

  ordering_dir = os.path.join(common.WORKING_DIR, "ordering_results/")
  
  with common.cd(ordering_dir):
    #TODO generate a proper relevant methods file.
    relevant_methods_file = "methods.txt"
    cmd = ["./cue",
           "typical", 
           "-k", "3", 
           "-f", relevant_methods_file]
    common.run_cmd(cmd, print_output=True)

  """
  Close the loop and add the best implementation found in the previous
  step back to the ontology.
  INPUT: k_list_of_methods
  OUTPUT: patch file for the ontology. Worst case: just add the 'best' implementation
  found in the corpus as a blob to the ontology. Best case: generate an equivalent
  flow-graph in the ontology.
  """
  print "TODO" # ALL



# if not os.path.isfile(daikon_jar):
#   print "Downloading dependencies"
#   cmd = ["./fetch_dependencies.sh"]
#   run_command(cmd)
#   print "Done."

if __name__ == '__main__':
  corpus = common.get_project_list()
  if len(sys.argv)>1:
    filtered_corpus = []
    for arg in sys.argv[1:]:
      if arg in corpus:
        filtered_corpus += [arg]
    print ("Filtered corpus contianing: {}".format(','.join(filtered_corpus)))
    corpus = filtered_corpus
  main(corpus)
  #find_methods_with_signature(corpus, "@ontology.qual.Sequence", ["@ontology.qual.Sequence"])

